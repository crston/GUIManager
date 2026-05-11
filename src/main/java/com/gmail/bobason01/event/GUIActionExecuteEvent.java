package com.gmail.bobason01.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class GUIActionExecuteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String guiId;
    private final int slot;
    private final ItemStack item;
    private final String actionId;
    private boolean cancelled = false;

    public GUIActionExecuteEvent(Player player, String guiId, int slot, ItemStack item, String actionId) {
        this.player = player;
        this.guiId = guiId;
        this.slot = slot;
        this.item = item;
        this.actionId = actionId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getGuiId() {
        return guiId;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getActionId() {
        return actionId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}