package com.hospital.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 周边医院统一入口。
 * 小程序只访问 8080，由本控制器代理到仅监听 127.0.0.1:5000 的爬虫服务，
 * 对外响应结构与爬虫保持一致（success / data / source / count / totalCount）。
 *
 * 优化点：
 * 1. RestTemplate 复用单例，避免每次请求重建连接工厂与客户端。
 * 2. 同一坐标附近的查询结果做短 TTL 缓存，降低爬虫与高德配额压力。
 * 3. 入参做边界收敛，防止异常参数透传给上游服务。
 */
@RestController
@RequestMapping("/api/nearby")
public class NearbyController {

    /** 查询结果缓存有效期：2 分钟。 */
    private static final long CACHE_TTL_MS = 120_000L;
    /** 缓存条目上限，避免无界增长。 */
    private static final int CACHE_MAX_SIZE = 200;

    @Value("${crawler.base-url}")
    private String crawlerBaseUrl;

    @Value("${crawler.timeout-ms:8000}")
    private int crawlerTimeoutMs;

    private volatile RestTemplate restTemplate;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    private RestTemplate restTemplate() {
        RestTemplate local = restTemplate;
        if (local == null) {
            synchronized (this) {
                local = restTemplate;
                if (local == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(crawlerTimeoutMs);
                    factory.setReadTimeout(crawlerTimeoutMs);
                    local = new RestTemplate(factory);
                    restTemplate = local;
                }
            }
        }
        return local;
    }

    @GetMapping("/hospitals")
    public Map<String, Object> hospitals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "5000") Integer radius,
            @RequestParam(required = false, defaultValue = "医院") String keyword,
            @RequestParam(required = false, defaultValue = "30") Integer count) {

        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            return failure("坐标参数不合法");
        }

        int safeRadius = clamp(radius == null ? 5000 : radius, 200, 50_000);
        int safeCount = clamp(count == null ? 30 : count, 1, 50);
        String safeKeyword = keyword == null ? "" : keyword.trim();

        String cacheKey = buildCacheKey(latitude, longitude, safeRadius, safeKeyword, safeCount);
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.body;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("latitude", latitude);
        payload.put("longitude", longitude);
        payload.put("radius", safeRadius);
        payload.put("keyword", safeKeyword);
        payload.put("count", safeCount);

        try {
            ResponseEntity<Map> response = restTemplate()
                    .postForEntity(crawlerBaseUrl + "/api/hospital/nearby", payload, Map.class);
            Map<?, ?> body = response.getBody();
            if (body != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) body;
                putCache(cacheKey, result);
                return result;
            }
            return failure("周边医院服务返回空响应");
        } catch (Exception e) {
            return failure("周边医院服务暂不可用: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            ResponseEntity<Map> response = restTemplate()
                    .getForEntity(crawlerBaseUrl + "/api/health", Map.class);
            result.put("status", "up");
            result.put("crawler", response.getBody());
        } catch (Exception e) {
            result.put("status", "down");
            result.put("crawler", null);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private void putCache(String key, Map<String, Object> body) {
        if (cache.size() >= CACHE_MAX_SIZE) {
            cache.clear();
        }
        cache.put(key, new CachedResult(body, System.currentTimeMillis() + CACHE_TTL_MS));
    }

    private String buildCacheKey(double latitude, double longitude, int radius, String keyword, int count) {
        // 坐标保留 4 位小数（约 10 米精度），让同一区域内的重复查询命中缓存
        return String.format("%.4f|%.4f|%d|%s|%d", latitude, longitude, radius, keyword, count);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("data", Collections.emptyList());
        result.put("count", 0);
        return result;
    }

    private static final class CachedResult {
        private final Map<String, Object> body;
        private final long expireAt;

        private CachedResult(Map<String, Object> body, long expireAt) {
            this.body = body;
            this.expireAt = expireAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
