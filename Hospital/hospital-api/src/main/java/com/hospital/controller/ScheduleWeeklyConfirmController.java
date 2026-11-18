package com.hospital.controller;

import com.hospital.security.AuthContext;
import com.hospital.service.ScheduleWeeklyConfirmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 医生下周排班确认接口。
 * 周日打开医生端即生成待确认提醒；医生选择沿用上周或需要修改；超时由定时任务自动沿用。
 */
@RestController
@RequestMapping("/api/schedule-weekly-confirm")
@CrossOrigin(origins = "*")
public class ScheduleWeeklyConfirmController {

    @Autowired
    private ScheduleWeeklyConfirmService service;

    @Autowired
    private AuthContext authContext;

    private void requireDoctorScope(String doctorId) {
        if ("DOCTOR".equals(authContext.role())) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(doctorId)) {
                throw new RuntimeException("无权操作其他医生的排班");
            }
        }
    }

    @GetMapping("/doctor/{doctorId}/pending")
    public Map<String, Object> getPending(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            result.put("success", true);
            result.put("data", service.getPendingInfo(doctorId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/doctor/{doctorId}/confirm")
    public Map<String, Object> confirm(@PathVariable String doctorId,
                                       @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            String action = body != null ? body.get("action") : null;
            result.put("success", true);
            result.put("data", service.confirm(doctorId, action));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 管理端手动触发提醒生成（用于验证/补发）。 */
    @PostMapping("/admin/remind-now")
    public Map<String, Object> remindNow() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!authContext.isAdmin()) {
                throw new RuntimeException("仅管理员可执行该操作");
            }
            result.put("success", true);
            result.put("created", service.createRemindersForAllDoctors());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 管理端手动触发超时自动沿用（用于验证/补发）。 */
    @PostMapping("/admin/inherit-now")
    public Map<String, Object> inheritNow() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!authContext.isAdmin()) {
                throw new RuntimeException("仅管理员可执行该操作");
            }
            result.put("success", true);
            result.put("inherited", service.autoInheritExpiredPending());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
