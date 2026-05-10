package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ChatListener implements Listener {

    private final GUIManager plugin;
    private final GUIListener guiListener;

    public ChatListener(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 명령어 대상(Target) 입력 대기
        if (plugin.isAwaitingTarget(player)) {
            event.setCancelled(true);
            String input = event.getMessage().trim();
            TargetInfo info = plugin.getAwaitingTargetInfo(player);
            plugin.removeAwaitingTarget(player);
            if (input.equalsIgnoreCase("cancel")) { player.sendMessage(ChatColor.RED + "Action cancelled"); return; }
            Bukkit.getScheduler().runTask(plugin, () -> {
                new ActionExecutor(plugin, guiListener).executeCommand(player, info.getCommand(), info.getExecutor(), input);
            });
            return;
        }

        // 아이템/GUI 편집 세션
        if (plugin.hasChatSession(player)) {
            event.setCancelled(true);
            EditSession session = plugin.getChatSession(player);
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                plugin.endChatSession(player);
                player.sendMessage(ChatColor.RED + "Editing cancelled");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (session.getEditType().name().startsWith("GUI_")) MainGuiEditor.open(player, plugin.getGui(session.getGuiName()));
                    else ItemEditor.open(player, session);
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> processChatInput(player, session, message));
        }
    }

    private void processChatInput(Player player, EditSession session, String message) {
        plugin.endChatSession(player);
        EditSession.EditType type = session.getEditType();
        String translated = GUIManager.color(message);

        // GUI 설정 편집
        if (type.name().startsWith("GUI_")) {
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui == null) return;
            if (type == EditSession.EditType.GUI_TITLE) gui.setTitle(message); // 저장 시 원본 저장, 출력 시 color() 적용
            else if (type == EditSession.EditType.GUI_PERMISSION) gui.setPermission(message.equalsIgnoreCase("none") ? "" : message);
            else if (type == EditSession.EditType.GUI_TARGETS) gui.setTargets(message.equalsIgnoreCase("none") ? "" : message);
            plugin.saveGui(gui.getId());
            MainGuiEditor.open(player, gui);
            return;
        }

        ItemStack item = session.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        switch (type) {
            case NAME:
                meta.setDisplayName(translated + "§r");
                break;
            case MATERIAL:
                try { item.setType(Material.valueOf(message.toUpperCase())); meta = item.getItemMeta(); }
                catch (Exception e) { player.sendMessage(ChatColor.RED + "Invalid material name"); }
                break;
            case PERMISSION_MESSAGE:
                meta.getPersistentDataContainer().set(GUIManager.KEY_PERMISSION_MESSAGE, PersistentDataType.STRING, translated);
                break;
            case SKULL:
                if (item.getType() != Material.PLAYER_HEAD) { item.setType(Material.PLAYER_HEAD); meta = item.getItemMeta(); }
                HeadCache.applyHead((SkullMeta) meta, message);
                break;
            case ITEM_MODEL_ID:
                try { meta.setCustomModelData(Integer.parseInt(message)); meta.getPersistentDataContainer().remove(GUIManager.KEY_ITEM_MODEL); }
                catch (Exception e) { meta.setCustomModelData(null); meta.getPersistentDataContainer().set(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING, message); }
                break;
            case LORE_ADD:
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("§r" + translated + "§r");
                meta.setLore(lore);
                break;
            case LORE_EDIT:
                if (meta.hasLore()) {
                    List<String> l = meta.getLore();
                    if (session.getLoreLineEditIndex() >= 0 && session.getLoreLineEditIndex() < l.size()) {
                        l.set(session.getLoreLineEditIndex(), "§r" + translated + "§r");
                        meta.setLore(l);
                    }
                }
                break;
            default:
                handleDynamicKeyType(meta, type, message, player);
                break;
        }

        item.setItemMeta(meta);
        GUI gui = plugin.getGui(session.getGuiName());
        if (gui != null && session.getSlot() != -1) {
            gui.setItem(session.getSlot(), item.clone());
            plugin.saveGui(gui.getId());
        }
        ItemEditor.open(player, session);
    }

    private void handleDynamicKeyType(ItemMeta meta, EditSession.EditType type, String message, Player player) {
        NamespacedKey key = ActionKeyUtil.getKeyFromType(type);
        if (key == null) return;
        if (type.name().startsWith("COMMAND_")) {
            String exist = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, exist.isEmpty() ? message : exist + ";;" + message);
        } else if (type.name().startsWith("MONEY_COST_") || type.name().startsWith("COOLDOWN_")) {
            try { meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, Double.parseDouble(message)); }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Must be a number"); }
        } else if (type.name().startsWith("PERMISSION_")) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, message.equalsIgnoreCase("none") ? "" : message);
        }
    }
}