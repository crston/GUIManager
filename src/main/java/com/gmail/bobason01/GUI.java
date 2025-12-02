package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class GUI {

    private String id;
    private String title;
    private int size;
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public GUI(String title, int size) {
        this.title = title;
        if (size <= 0 || size % 9 != 0 || size > 54) {
            throw new IllegalArgumentException("GUI size must be a multiple of 9 between 9 and 54.");
        }
        this.size = size;
    }

    public GUI(GUI other) {
        this.id = other.id;
        this.title = other.title;
        this.size = other.size;
        other.items.forEach((slot, item) -> this.items.put(slot, item.clone()));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getSize() { return size; }

    public void setSize(int newSize) {
        if (newSize <= 0 || newSize % 9 != 0 || newSize > 54) {
            return;
        }
        this.size = newSize;
        items.entrySet().removeIf(entry -> entry.getKey() >= newSize);
    }

    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < size) {
            if (item == null || item.getType().isAir()) {
                items.remove(slot);
            } else {
                items.put(slot, item);
            }
        }
    }

    public ItemStack getItem(int slot) { return items.get(slot); }
    public Map<Integer, ItemStack> getItems() { return items; }

    public void updateFromInventory(Inventory inventory) {
        items.clear();
        for (int i = 0; i < size; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                items.put(i, item);
            }
        }
    }

    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        items.forEach(inventory::setItem);
        return inventory;
    }
}