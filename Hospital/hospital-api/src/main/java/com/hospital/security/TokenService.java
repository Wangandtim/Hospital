package com.hospital.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌管理。
 * 目前为单机内存实现，足以支撑当前单体部署；
 * 若后续多实例部署，需要替换为 Redis 等共享存储。
 */
@Component
public class TokenService {

    /** 令牌有效期：12 小时。 */
    private static final long TTL_MS = 12L * 60 * 60 * 1000;
    /** 超过该数量时清理过期令牌，避免内存无界增长。 */
    private static final int MAX_TOKENS = 5000;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String role, String ownerId) {
        return issue(role, ownerId, null, null);
    }

    /**
     * 管理员登录专用：令牌额外记录超管/院管身份与所属医院，供数据隔离使用。
     */
    public String issueAdmin(String ownerId, String adminRole, String hospitalId) {
        return issue("ADMIN", ownerId, adminRole, hospitalId);
    }

    private String issue(String role, String ownerId, String adminRole, String hospitalId) {
        purgeIfNeeded();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        sessions.put(token.toString(),
                new Session(role, ownerId, adminRole, hospitalId, System.currentTimeMillis() + TTL_MS));
        return token.toString();
    }

    public Session verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expired()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** 吊销某医院所有应急令牌（管理员冻结应急权限时使用）。 */
    public void revokeEmergencyByHospital(String hospitalId) {
        if (hospitalId == null) {
            return;
        }
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            return "ADMIN".equals(session.getRole())
                    && "EMERGENCY_ADMIN".equals(session.getAdminRole())
                    && hospitalId.equals(session.getHospitalId());
        });
    }

    /**
     * 登记外部令牌（应急管理员令牌由应急登录签发，有效期以调用方为准）。
     */
    public void register(String token, String role, String ownerId, String adminRole,
                         String hospitalId, long expireAtMillis) {
        if (token == null || token.isEmpty()) {
            return;
        }
        purgeIfNeeded();
        sessions.put(token, new Session(role, ownerId, adminRole, hospitalId, expireAtMillis));
    }

    private void purgeIfNeeded() {
        if (sessions.size() < MAX_TOKENS) {
            return;
        }
        sessions.entrySet().removeIf(entry -> entry.getValue().expired());
    }

    /** 一次登录会话。 */
    public static final class Session {
        private final String role;
        private final String ownerId;
        private final String adminRole;
        private final String hospitalId;
        private final long expireAt;

        Session(String role, String ownerId, String adminRole, String hospitalId, long expireAt) {
            this.role = role;
            this.ownerId = ownerId;
            this.adminRole = adminRole;
            this.hospitalId = hospitalId;
            this.expireAt = expireAt;
        }

        public String getRole() {
            return role;
        }

        public String getOwnerId() {
            return ownerId;
        }

        /** 管理员会话的超管/院管身份；医生、患者为 null。 */
        public String getAdminRole() {
            return adminRole;
        }

        /** 院管绑定的医院；超管为 null。 */
        public String getHospitalId() {
            return hospitalId;
        }

        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
