package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HeadCache
 * Simple lock free cache for head item templates keyed by texture key
 * Stores already profiled ItemStack templates to avoid reflection on hot paths
 */
public final class HeadCache {

    private HeadCache() {}

    private static final Map<String, ItemStack> CACHE = new ConcurrentHashMap<>(256);

    public static ItemStack get(String key) {
        return CACHE.get(key);
    }

    public static void put(String key, ItemStack template) {
        if (key == null || template == null) return;
        CACHE.put(key, template);
    }

    public static boolean contains(String key) {
        return CACHE.containsKey(key);
    }

    public static void clear() {
        CACHE.clear();
    }
}
