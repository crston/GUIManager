package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class KeybindListener implements Listener {

    private final GUIManager plugin;
    private final GUIListener guiListener;

    public KeybindListener(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        ActionKeyUtil.KeyAction action = event.getPlayer().isSneaking() ? ActionKeyUtil.KeyAction.SHIFT_F : ActionKeyUtil.KeyAction.F;
        boolean wasHandled = handleKeyEvent(event.getPlayer(), event.getMainHandItem(), action);
        if (wasHandled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        ActionKeyUtil.KeyAction action = event.getPlayer().isSneaking() ? ActionKeyUtil.KeyAction.SHIFT_Q : ActionKeyUtil.KeyAction.Q;
        boolean wasHandled = handleKeyEvent(event.getPlayer(), event.getItemDrop().getItemStack(), action);
        if (wasHandled) {
            event.setCancelled(true);
        }
    }

    private boolean handleKeyEvent(Player player, ItemStack item, ActionKeyUtil.KeyAction action) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey commandKey = ActionKeyUtil.getCommandKey(action);

        if (!pdc.has(commandKey, PersistentDataType.STRING)) {
            return false;
        }

        NamespacedKey permKey = ActionKeyUtil.getPermissionKey(action);
        String permission = pdc.get(permKey, PersistentDataType.STRING);
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            String noPermMsg = pdc.get(GUIManager.KEY_PERMISSION_MESSAGE, PersistentDataType.STRING);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermMsg != null ? noPermMsg : "&cYou don't have permission."));
            return true;
        }

        String command = pdc.get(commandKey, PersistentDataType.STRING);
        if(command == null || command.isEmpty()) return false;

        if (!guiListener.checkAndTakeCosts(player, pdc, ActionKeyUtil.getMoneyCostKey(action), ActionKeyUtil.getItemCostKey(action))) {
            return true;
        }

        guiListener.executeCommand(player, command);
        return true;
    }
}