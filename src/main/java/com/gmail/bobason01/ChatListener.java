package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ChatListener implements Listener {

    private final GUIManager plugin;
    private final ActionExecutor actionExecutor;

    public ChatListener(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin, guiListener);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.isAwaitingTarget(player)) {
            event.setCancelled(true);
            String targetName = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> processTargetInput(player, targetName));
            return;
        }

        if (plugin.hasChatSession(player)) {
            event.setCancelled(true);
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> processChatEdit(player, message));
        }
    }

    private void processTargetInput(Player player, String targetName) {
        TargetInfo targetInfo = plugin.getAwaitingTargetInfo(player);
        if (targetInfo == null) return;

        plugin.removeAwaitingTarget(player);

        if (targetName.equalsIgnoreCase("cancel") || targetName.equals("취소")) {
            player.sendMessage(ChatColor.YELLOW + "Target selection cancelled");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or is offline");
            return;
        }

        actionExecutor.executeCommand(player, targetInfo.command(), targetInfo.executor(), target.getName());
    }

    private void processChatEdit(Player player, String message) {
        EditSession session = plugin.getChatSession(player);
        if (session == null) return;

        if (message.equalsIgnoreCase("cancel") || message.equals("취소")) {
            player.sendMessage(ChatColor.YELLOW + "Editing cancelled");
            plugin.endChatSession(player);
            ItemEditor.open(player, session);
            return;
        }

        EditSession.EditType type = session.getEditType();
        switch (type) {
            case NAME:
                handleNameEdit(player, session, message);
                break;
            case CUSTOM_MODEL_DATA:
            case ITEM_DAMAGE:
                handleIntegerEdit(player, session, message);
                break;
            case ITEM_MODEL_ID:
                handleItemModelEdit(player, session, message);
                break;
            case LORE_ADD:
            case LORE_EDIT:
                handleLoreEdit(player, session, message);
                break;
            case SKULL:
                handleSkullEdit(player, session, message);
                break;
            case MONEY_COST_LEFT: case MONEY_COST_RIGHT: case MONEY_COST_SHIFT_LEFT: case MONEY_COST_SHIFT_RIGHT:
            case MONEY_COST_F: case MONEY_COST_SHIFT_F: case MONEY_COST_Q: case MONEY_COST_SHIFT_Q:
            case COOLDOWN_LEFT: case COOLDOWN_RIGHT: case COOLDOWN_SHIFT_LEFT: case COOLDOWN_SHIFT_RIGHT:
            case COOLDOWN_F: case COOLDOWN_SHIFT_F: case COOLDOWN_Q: case COOLDOWN_SHIFT_Q:
                handleDoubleEdit(player, session, message);
                break;
            default:
                handleStringEdit(player, session, message);
                break;
        }

        GUI gui = plugin.getGui(session.getGuiName());
        if (gui != null) {
            gui.setItem(session.getSlot(), session.getItem());
        }

        plugin.endChatSession(player);
        ItemEditor.open(player, session);
    }

    private void handleSkullEdit(Player p, EditSession s, String msg) {
        ItemStack currentItem = s.getItem();
        ItemStack newHead;

        if (msg.length() > 20) {
            newHead = HeadUtils.createHeadByBase64(msg);
        } else {
            newHead = HeadUtils.createHeadByName(msg);
        }

        if (newHead == null) {
            p.sendMessage(ChatColor.RED + "Failed to create skull");
            return;
        }

        ItemMeta oldMeta = currentItem.getItemMeta();
        ItemMeta newMeta = newHead.getItemMeta();

        if (oldMeta != null && newMeta != null) {
            if (oldMeta.hasDisplayName()) newMeta.setDisplayName(oldMeta.getDisplayName());
            if (oldMeta.hasLore()) newMeta.setLore(oldMeta.getLore());
            if (oldMeta.hasCustomModelData()) newMeta.setCustomModelData(oldMeta.getCustomModelData());
            ActionKeyUtil.copyPersistentData(oldMeta.getPersistentDataContainer(), newMeta.getPersistentDataContainer());
        }

        newHead.setItemMeta(newMeta);
        newHead.setAmount(currentItem.getAmount());

        s.setItem(newHead);
        p.sendMessage(ChatColor.GREEN + "Skull texture updated");
    }

    private void handleItemModelEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;

        if (msg.equalsIgnoreCase("delete") || msg.equals("삭제") || msg.equalsIgnoreCase("none")) {
            meta.getPersistentDataContainer().remove(GUIManager.KEY_ITEM_MODEL);
            p.sendMessage(ChatColor.GREEN + "Item Model removed");
        } else {
            meta.getPersistentDataContainer().set(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING, msg);
            try {
                NamespacedKey modelKey = NamespacedKey.fromString(msg);
                if (modelKey != null) {
                    meta.setItemModel(modelKey);
                    p.sendMessage(ChatColor.GREEN + "Item Model set to " + msg);
                }
            } catch (Throwable ignored) {
                p.sendMessage(ChatColor.RED + "Invalid Namespace format");
            }
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleStringEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;

        if (msg.equalsIgnoreCase("delete") || msg.equals("삭제") || msg.equalsIgnoreCase("none")) {
            meta.getPersistentDataContainer().remove(key);
            p.sendMessage(ChatColor.GREEN + "Setting removed");
        } else {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, msg);
            p.sendMessage(ChatColor.GREEN + "Setting saved");
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleIntegerEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;

        if (msg.equalsIgnoreCase("delete") || msg.equals("삭제")) {
            meta.getPersistentDataContainer().remove(key);
            if(s.getEditType() == EditSession.EditType.CUSTOM_MODEL_DATA) meta.setCustomModelData(null);
            else if (meta instanceof Damageable) ((Damageable) meta).setDamage(0);
            p.sendMessage(ChatColor.GREEN + "Value removed");
        } else {
            try {
                int value = Integer.parseInt(msg);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
                if(s.getEditType() == EditSession.EditType.CUSTOM_MODEL_DATA) meta.setCustomModelData(value);
                else if (meta instanceof Damageable) ((Damageable) meta).setDamage(value);
                p.sendMessage(ChatColor.GREEN + "Value set");
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid number format");
            }
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleDoubleEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;

        if (msg.equalsIgnoreCase("delete") || msg.equals("삭제")) {
            meta.getPersistentDataContainer().remove(key);
            p.sendMessage(ChatColor.GREEN + "Value removed");
        } else {
            try {
                double value = Double.parseDouble(msg);
                meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                p.sendMessage(ChatColor.GREEN + "Value set");
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid number format");
            }
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleNameEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', msg));
        s.getItem().setItemMeta(meta);
        p.sendMessage(ChatColor.GREEN + "Display name changed");
    }

    private void handleLoreEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String text = ChatColor.translateAlternateColorCodes('&', msg);

        if (s.getEditType() == EditSession.EditType.LORE_ADD) {
            lore.add(text);
            p.sendMessage(ChatColor.GREEN + "Lore line added");
        } else {
            int line = s.getLoreLineEditIndex();
            if (line >= 0 && line < lore.size()) {
                lore.set(line, text);
                p.sendMessage(ChatColor.GREEN + "Lore line changed");
            } else {
                p.sendMessage(ChatColor.RED + "Invalid lore line to edit");
                return;
            }
        }
        meta.setLore(lore);
        s.getItem().setItemMeta(meta);
    }
}