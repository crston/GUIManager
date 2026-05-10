package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GUI {
    private String id;
    private String title;
    private int size;
    private String permission;
    private String targets;
    private final Map<Integer, ItemStack> items = new ConcurrentHashMap<>();

    public GUI(String title, int size) {
        this.title = title;
        this.size = size;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getSize() { return size; }
    public void setSize(int size) {
        this.size = size;
        items.keySet().removeIf(slot -> slot >= size);
    }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public String getTargets() { return targets; }
    public void setTargets(String targets) { this.targets = targets; }

    public Map<Integer, ItemStack> getItems() { return items; }

    public void setItem(int slot, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            items.remove(slot);
        } else {
            items.put(slot, item);
        }
    }
}