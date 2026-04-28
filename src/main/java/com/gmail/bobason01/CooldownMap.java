package com.gmail.bobason01;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class CooldownMap {

    private final Map<String, Long> expiry = new ConcurrentHashMap<>();
    // 동시성 해시맵 사이즈 호출 부하를 막기 위한 원자적 카운터
    private final AtomicInteger cleanupCounter = new AtomicInteger(0);

    public void put(String id, long expireAtMillis) {
        expiry.put(id, expireAtMillis);
        if (cleanupCounter.incrementAndGet() > 256) {
            cleanupCounter.set(0);
            cleanup(System.currentTimeMillis());
        }
    }

    public long remainingMillis(String id) {
        long now = System.currentTimeMillis();
        Long e = expiry.get(id);
        if (e == null) return 0;
        if (e <= now) {
            expiry.remove(id);
            return 0;
        }
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