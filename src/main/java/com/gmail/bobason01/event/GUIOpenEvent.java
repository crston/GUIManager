package com.gmail.bobason01.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GUIOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String guiId;
    private boolean cancelled = false;

    public GUIOpenEvent(Player player, String guiId) {
        this.player = player;
        this.guiId = guiId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getGuiId() {
        return guiId;
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