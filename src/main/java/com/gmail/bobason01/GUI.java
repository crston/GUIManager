package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class GUI {

    private String title;
    private int size; // final 키워드 제거
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public GUI(String title, int size) {
        this.title = title;
        if (size <= 0 || size % 9 != 0 || size > 54) {
            throw new IllegalArgumentException("GUI size must be a multiple of 9 between 9 and 54.");
        }
        this.size = size;
    }

    public GUI(GUI other) {
        this.title = other.title;
        this.size = other.size;
        other.items.forEach((slot, item) -> this.items.put(slot, item.clone()));
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getSize() { return size; }

    /**
     * GUI의 크기를 변경합니다. 크기가 줄어들 경우 범위를 벗어나는 아이템은 제거됩니다.
     * @param newSize 새로운 GUI 크기 (반드시 9의 배수)
     */
    public void setSize(int newSize) {
        if (newSize <= 0 || newSize % 9 != 0 || newSize > 54) {
            return; // 유효하지 않은 사이즈는 무시
        }
        this.size = newSize;

        // 새로운 크기의 범위를 벗어나는 아이템들을 제거
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