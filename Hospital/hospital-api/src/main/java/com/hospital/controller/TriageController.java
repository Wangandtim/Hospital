package com.hospital.controller;

import com.hospital.security.AuthContext;
import com.hospital.service.TriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分诊预约 Agent 接口。
 * 患者令牌归属即 patientId；管理员可只读查看会话详情，不能代患者锁号/出单。
 */
@RestController
@RequestMapping("/api/triage")
@CrossOrigin(origins = "*")
public class TriageController {

    @Autowired
    private TriageService triageService;

    @Autowired
    private AuthContext authContext;

    @PostMapping("/assess")
    public Map<String, Object> assess(@RequestBody(required = false) Map<String, Object> body) {
        return execute(() -> triageService.assess(requirePatient(), body == null ? new LinkedHashMap<>() : body));
    }

    @PostMapping("/{triageId}/profile")
    public Map<String, Object> profile(@PathVariable String triageId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        return execute(() -> triageService.profile(requirePatient(), triageId,
                body == null ? new LinkedHashMap<>() : body));
    }

    @PostMapping("/{triageId}/clarify")
    public Map<String, Object> clarify(@PathVariable String triageId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        return execute(() -> triageService.clarify(requirePatient(), triageId,
                body == null ? new LinkedHashMap<>() : body));
    }

    @PostMapping("/{triageId}/confirm")
    public Map<String, Object> confirm(@PathVariable String triageId) {
        return execute(() -> triageService.confirm(requirePatient(), triageId));
    }

    @PostMapping("/{triageId}/reject")
    public Map<String, Object> reject(@PathVariable String triageId) {
        return execute(() -> triageService.reject(requirePatient(), triageId));
    }

    @PostMapping("/{triageId}/lock")
    public Map<String, Object> lock(@PathVariable String triageId) {
        return execute(() -> triageService.lock(requirePatient(), triageId));
    }

    @PostMapping("/{triageId}/book")
    public Map<String, Object> book(@PathVariable String triageId) {
        return execute(() -> triageService.book(requirePatient(), triageId));
    }

    @GetMapping("/{triageId}")
    public Map<String, Object> detail(@PathVariable String triageId) {
        return execute(() -> {
            boolean admin = authContext.isAdmin();
            String patientId = admin ? null : requirePatient();
            return triageService.detail(patientId, triageId, admin);
        });
    }

    private String requirePatient() {
        String ownerId = authContext.ownerId();
        if (ownerId == null || ownerId.isEmpty()) {
            throw new IllegalArgumentException("登录状态异常，请重新登录");
        }
        return ownerId;
    }

    private Map<String, Object> execute(java.util.function.Supplier<Map<String, Object>> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "分诊服务暂时不可用，请稍后重试");
            return error;
        }
    }
}
