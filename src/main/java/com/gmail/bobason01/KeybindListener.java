package com.gmail.bobason01;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class KeybindListener implements Listener {

    private final ActionExecutor actionExecutor;

    public KeybindListener(GUIManager plugin, GUIListener guiListener) {
        this.actionExecutor = new ActionExecutor(plugin, guiListener);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        ActionKeyUtil.KeyAction action = event.getPlayer().isSneaking() ? ActionKeyUtil.KeyAction.SHIFT_F : ActionKeyUtil.KeyAction.F;
        ItemStack item = event.getMainHandItem();
        if (item == null || !item.hasItemMeta()) return;

        actionExecutor.execute(event.getPlayer(), item, action);
        // ActionExecutor handles all logic, so we can assume if it was handled, it should be cancelled.
        // We need a way to know if an action was actually found.
        if (Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(ActionKeyUtil.getCommandKey(action), org.bukkit.persistence.PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        ActionKeyUtil.KeyAction action = event.getPlayer().isSneaking() ? ActionKeyUtil.KeyAction.SHIFT_Q : ActionKeyUtil.KeyAction.Q;
        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null || !item.hasItemMeta()) return;

        actionExecutor.execute(event.getPlayer(), item, action);
        if (Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(ActionKeyUtil.getCommandKey(action), org.bukkit.persistence.PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }
}