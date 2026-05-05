package com.gmail.bobason01;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class CooldownMap {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    public void put(String actionId, long expiryTime) { cooldowns.put(actionId, expiryTime); }
    public long remainingMillis(String actionId) {
        Long expiry = cooldowns.get(actionId);
        if (expiry == null) return 0;
        long rem = expiry - System.currentTimeMillis();
        if (rem <= 0) { cooldowns.remove(actionId); return 0; }
        return rem;
    }
}