package com.gmail.bobason01;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownMap {

    private final Map<String, Long> expiry = new ConcurrentHashMap<>();

    public void put(String id, long expireAtMillis) {
        expiry.put(id, expireAtMillis);
    }

    public long remainingMillis(String id) {
        long now = System.currentTimeMillis();
        Long e = expiry.get(id);
        if (e == null) return 0;
        if (e <= now) {
            expiry.remove(id);
            return 0;
        }
        if (expiry.size() > 256) cleanup(now);
        return e - now;
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, Long>> it = expiry.entrySet().iterator();
        int scans = 0;
        while (it.hasNext() && scans < 64) {
            Map.Entry<String, Long> en = it.next();
            if (en.getValue() <= now) it.remove();
            scans++;
        }
    }
}
