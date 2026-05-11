package com.gmail.bobason01.utils;

import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

public class HeadCache {
    // 이제 PlayerProfile 대신 ItemStack 자체를 캐싱합니다.
    private static final ConcurrentHashMap<String, ItemStack> cache = new ConcurrentHashMap<>();

    public static boolean has(String value) {
        return value != null && cache.containsKey(value);
    }

    public static ItemStack getHead(String value) {
        ItemStack item = cache.get(value);
        return item != null ? item.clone() : null;
    }

    public static void cacheHead(String value, ItemStack item) {
        if (value == null || item == null) return;
        cache.put(value, item.clone());
    }

    public static void clear() {
        cache.clear();
    }

    // 기존 applyHead 메서드 (필요시 사용)
    public static void applyHead(org.bukkit.inventory.meta.SkullMeta meta, String value) {
        if (value == null || value.isEmpty()) return;
        // HeadUtils의 로직을 활용하거나 직접 구현
        ItemStack head = HeadUtils.createHeadBySpec(null, value);
        if (head.hasItemMeta()) {
            meta.setOwnerProfile(((org.bukkit.inventory.meta.SkullMeta)head.getItemMeta()).getOwnerProfile());
        }
    }
}