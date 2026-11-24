/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.auth;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private static final int CAPACITY = 10;
    private static final long REFILL_NANOS = Duration.ofMinutes(1).toNanos();
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        long now = System.nanoTime();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(CAPACITY, now));
        synchronized (bucket) {
            long elapsed = now - bucket.updatedAt;
            if (elapsed >= REFILL_NANOS) {
                bucket.tokens = Math.min(CAPACITY, bucket.tokens + (int) (elapsed / REFILL_NANOS) * CAPACITY);
                bucket.updatedAt = now;
            }
            if (bucket.tokens == 0) return false;
            bucket.tokens--;
            return true;
        }
    }

    private static final class Bucket {
        int tokens;
        long updatedAt;
        Bucket(int tokens, long updatedAt) { this.tokens = tokens; this.updatedAt = updatedAt; }
    }
}
