package com.hospital.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 统一密码处理。
 * 新密码一律 BCrypt 存储；历史明文密码通过 matches 兼容，
 * 登录成功后可原地升级为哈希（见各 Service 的登录逻辑）。
 */
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String raw) {
        return raw == null || raw.isEmpty() ? raw : encoder.encode(raw);
    }

    public boolean isEncoded(String value) {
        return value != null && value.startsWith("$2a$");
    }

    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        return isEncoded(stored) ? encoder.matches(raw, stored) : raw.equals(stored);
    }
}
