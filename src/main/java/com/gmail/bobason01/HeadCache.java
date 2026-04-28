package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeadCache {

    private static final Map<String, ItemStack> cache = new ConcurrentHashMap<>();

    public static ItemStack getHead(String base64) {
        return cache.get(base64);
    }

    public static void cacheHead(String base64, ItemStack head) {
        if (base64 != null && head != null) {
            cache.put(base64, head.clone());
        }
    }

    public static void clear() {
        cache.clear();
    }

    public static boolean has(String base64) {
        return cache.containsKey(base64);
    }
}