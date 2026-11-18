package com.hospital.security;

import com.hospital.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前请求的登录身份上下文。
 * 由 AuthInterceptor 在令牌校验通过后写入请求属性，业务层据此做医院级数据隔离。
 */
@Component
public class AuthContext {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String HOSPITAL_ADMIN = "HOSPITAL_ADMIN";
    public static final String EMERGENCY_ADMIN = "EMERGENCY_ADMIN";

    public String role() {
        return attribute(AuthInterceptor.ATTR_ROLE);
    }

    public String adminRole() {
        return attribute(AuthInterceptor.ATTR_ADMIN_ROLE);
    }

    public String hospitalId() {
        return attribute(AuthInterceptor.ATTR_HOSPITAL_ID);
    }

    public String ownerId() {
        return attribute(AuthInterceptor.ATTR_OWNER_ID);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public boolean isSuperAdmin() {
        return SUPER_ADMIN.equals(adminRole());
    }

    public boolean isHospitalAdmin() {
        return HOSPITAL_ADMIN.equals(adminRole()) || EMERGENCY_ADMIN.equals(adminRole());
    }

    public boolean isEmergencyAdmin() {
        return EMERGENCY_ADMIN.equals(adminRole());
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ForbiddenException("该操作仅限卫健委总管理员");
        }
    }

    /**
     * 校验目标医院是否为当前院管所属医院；超管不受限。
     */
    public void requireHospitalScope(String hospitalId) {
        if (!isHospitalAdmin()) {
            return;
        }
        if (hospitalId == null || !hospitalId.equals(this.hospitalId())) {
            throw new ForbiddenException("无权操作其他医院的数据");
        }
    }

    /**
     * 解析本次操作实际使用的医院 ID。
     * 院管只能操作本院，传入其他医院直接拒绝；超管按请求值执行。
     */
    public String resolveHospitalId(String requested) {
        if (isHospitalAdmin()) {
            String own = hospitalId();
            if (requested != null && !requested.isEmpty() && !requested.equals(own)) {
                throw new ForbiddenException("无权操作其他医院的数据");
            }
            return own;
        }
        return requested;
    }

    private String attribute(String name) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object value = request.getAttribute(name);
        return value == null ? null : value.toString();
    }
}
