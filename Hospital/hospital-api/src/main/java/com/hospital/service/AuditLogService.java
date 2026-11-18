package com.hospital.service;

import com.hospital.entity.AuditLog;
import com.hospital.repository.AuditLogRepository;
import com.hospital.security.AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 审计日志服务。写日志失败不影响主业务。
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void record(String action, String targetType, String targetId, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetail(detail);
            entry.setCreatedAt(LocalDateTime.now());

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object role = request.getAttribute(AuthInterceptor.ATTR_ROLE);
                Object ownerId = request.getAttribute(AuthInterceptor.ATTR_OWNER_ID);
                entry.setOperatorRole(role == null ? "ANONYMOUS" : role.toString());
                entry.setOperatorId(ownerId == null ? null : ownerId.toString());
                entry.setIp(clientIp(request));
            } else {
                entry.setOperatorRole("SYSTEM");
            }

            auditLogRepository.save(entry);
        } catch (Exception e) {
            // 审计失败只记日志，不影响业务主流程
            log.warn("审计日志写入失败: action={}, target={}/{}", action, targetType, targetId, e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
