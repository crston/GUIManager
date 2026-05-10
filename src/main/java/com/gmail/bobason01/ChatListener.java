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

        // 명령어 대상(Target) 입력 대기 중인 경우
        if (plugin.isAwaitingTarget(player)) {
            event.setCancelled(true);
            String input = event.getMessage().trim();
            TargetInfo info = plugin.getAwaitingTargetInfo(player);
            plugin.removeAwaitingTarget(player);

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Action cancelled");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                ActionExecutor executor = new ActionExecutor(plugin, guiListener);
                executor.executeCommand(player, info.getCommand(), info.getExecutor(), input);
            });
            return;
        }

        // 아이템/GUI 편집 세션 중인 경우
        if (plugin.hasChatSession(player)) {
            event.setCancelled(true);
            EditSession session = plugin.getChatSession(player);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                plugin.endChatSession(player);
                player.sendMessage(ChatColor.RED + "Editing cancelled");
                openCorrectEditor(player, session);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> processChatInput(player, session, message));
        }
    }

    private void openCorrectEditor(Player player, EditSession session) {
        if (session.getEditType().name().startsWith("GUI_")) {
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui != null) MainGuiEditor.open(player, gui);
        } else {
            ItemEditor.open(player, session);
        }
    }

    private void processChatInput(Player player, EditSession session, String message) {
        plugin.endChatSession(player);
        EditSession.EditType type = session.getEditType();

        // 1. GUI 자체 설정 편집 (타이틀, 권한 등)
        if (type.name().startsWith("GUI_")) {
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui == null) return;
            String translated = ChatColor.translateAlternateColorCodes('&', message);

            if (type == EditSession.EditType.GUI_TITLE) {
                gui.setTitle(translated);
            } else if (type == EditSession.EditType.GUI_PERMISSION) {
                gui.setPermission(message.equalsIgnoreCase("none") ? "" : message);
            } else if (type == EditSession.EditType.GUI_TARGETS) {
                gui.setTargets(message.equalsIgnoreCase("none") ? "" : translated);
            }
            plugin.saveGui(gui.getId());
            MainGuiEditor.open(player, gui);
            return;
        }

        // 2. 아이템 정보 편집
        ItemStack item = session.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String translated = ChatColor.translateAlternateColorCodes('&', message);

        switch (type) {
            case NAME:
                meta.setDisplayName(translated);
                break;
            case MATERIAL:
                try {
                    Material mat = Material.valueOf(message.toUpperCase());
                    item.setType(mat);
                    meta = item.getItemMeta(); // 타입이 바뀌면 메타도 새로 가져옴
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Invalid material name");
                }
                break;
            case PERMISSION_MESSAGE:
                meta.getPersistentDataContainer().set(GUIManager.KEY_PERMISSION_MESSAGE, PersistentDataType.STRING, translated);
                break;
            case SKULL:
                if (item.getType() != Material.PLAYER_HEAD) {
                    item.setType(Material.PLAYER_HEAD);
                    meta = item.getItemMeta();
                }
                HeadCache.applyHead((SkullMeta) meta, message);
                break;
            case ITEM_MODEL_ID:
                try {
                    int cmd = Integer.parseInt(message);
                    meta.setCustomModelData(cmd);
                    meta.getPersistentDataContainer().remove(GUIManager.KEY_ITEM_MODEL);
                } catch (NumberFormatException e) {
                    meta.setCustomModelData(null);
                    meta.getPersistentDataContainer().set(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING, message);
                }
                break;
            case LORE_ADD:
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(translated);
                meta.setLore(lore);
                break;
            case LORE_EDIT:
                if (meta.hasLore()) {
                    List<String> editLore = meta.getLore();
                    if (session.getLoreLineEditIndex() >= 0 && session.getLoreLineEditIndex() < editLore.size()) {
                        editLore.set(session.getLoreLineEditIndex(), translated);
                        meta.setLore(editLore);
                    }
                }
                break;
            default:
                handleDynamicKeyType(meta, type, message, player);
                break;
        }

        item.setItemMeta(meta);

        // 중요: 수정된 아이템을 실제 GUI 객체에 동기화하고 저장
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
            String newValue = (exist.isEmpty()) ? message : exist + ";;" + message;
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, newValue);
        } else if (type.name().startsWith("MONEY_COST_") || type.name().startsWith("COOLDOWN_")) {
            try {
                double val = Double.parseDouble(message);
                meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, val);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Must be a number");
            }
        } else if (type.name().startsWith("PERMISSION_")) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, message.equalsIgnoreCase("none") ? "" : message);
        }
    }
}