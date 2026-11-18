package com.hospital.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级内存限流：
 * 1) 登录失败计数（同一账号 + IP 连续失败 5 次锁 15 分钟）；
 * 2) 固定窗口计数（用于短信验证码按 IP 限流）。
 */
@Component
public class RateLimitService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCK_WINDOW_MS = 15L * 60 * 1000;
    private static final int MAX_ENTRIES = 5000;

    private final Map<String, FailureEntry> failures = new ConcurrentHashMap<>();
    private final Map<String, WindowEntry> windows = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        FailureEntry entry = failures.get(key);
        if (entry == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - entry.lastFailureAt > LOCK_WINDOW_MS) {
            failures.remove(key);
            return false;
        }
        return entry.count >= MAX_LOGIN_FAILURES;
    }

    public void recordFailure(String key) {
        purgeIfNeeded();
        FailureEntry entry = failures.computeIfAbsent(key, k -> new FailureEntry());
        synchronized (entry) {
            long now = System.currentTimeMillis();
            if (now - entry.lastFailureAt > LOCK_WINDOW_MS) {
                entry.count = 0;
            }
            entry.count++;
            entry.lastFailureAt = now;
        }
    }

    public void recordSuccess(String key) {
        failures.remove(key);
    }

    /** 固定窗口计数：返回 false 表示超过允许次数。 */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        purgeIfNeeded();
        long now = System.currentTimeMillis();
        WindowEntry entry = windows.computeIfAbsent(key, k -> new WindowEntry(now));
        synchronized (entry) {
            if (now - entry.windowStart > windowMs) {
                entry.windowStart = now;
                entry.count = 0;
            }
            if (entry.count >= limit) {
                return false;
            }
            entry.count++;
            return true;
        }
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private void purgeIfNeeded() {
        if (failures.size() + windows.size() < MAX_ENTRIES) {
            return;
        }
        long now = System.currentTimeMillis();
        failures.entrySet().removeIf(e -> now - e.getValue().lastFailureAt > LOCK_WINDOW_MS);
        windows.entrySet().removeIf(e -> now - e.getValue().windowStart > 60L * 60 * 1000);
    }

    private static final class FailureEntry {
        private int count;
        private long lastFailureAt;

        private FailureEntry() {
            this.lastFailureAt = System.currentTimeMillis();
        }
    }

    private static final class WindowEntry {
        private long windowStart;
        private int count;

        private WindowEntry(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
