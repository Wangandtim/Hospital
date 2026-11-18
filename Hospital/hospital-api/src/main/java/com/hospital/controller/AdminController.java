package com.hospital.controller;

import com.hospital.entity.Admin;
import com.hospital.entity.Hospital;
import com.hospital.security.AuthContext;
import com.hospital.security.RateLimitService;
import com.hospital.security.TokenService;
import com.hospital.service.AuditLogService;
import com.hospital.service.AdminService;
import com.hospital.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private HospitalService hospitalService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request,
                                     jakarta.servlet.http.HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> result = new HashMap<>();

        String attemptKey = "admin:" + username + "@" + rateLimitService.clientIp(httpRequest);
        if (rateLimitService.isLocked(attemptKey)) {
            result.put("success", false);
            result.put("message", "尝试次数过多，请 15 分钟后再试");
            return result;
        }

        try {
            Admin admin = adminService.login(username, password);
            rateLimitService.recordSuccess(attemptKey);

            String role = admin.getRole();
            if (role == null || role.isEmpty()) {
                role = "admin".equals(username) ? "SUPER_ADMIN" : "HOSPITAL_ADMIN";
            }
            String hospitalId = "SUPER_ADMIN".equals(role) ? null : admin.getHospitalId();

            result.put("success", true);
            result.put("message", "登录成功");
            // 令牌记录超管/院管身份与所属医院，后续接口据此做数据隔离
            result.put("token", tokenService.issueAdmin(admin.getAdminId(), role, hospitalId));
            result.put("adminId", admin.getAdminId());
            result.put("username", admin.getUsername());
            result.put("realName", admin.getRealName());
            result.put("role", role);

            if ("SUPER_ADMIN".equals(role)) {
                result.put("hospitalId", null);
                result.put("hospitalName", null);
            } else if (hospitalId != null && !hospitalId.isEmpty()) {
                result.put("hospitalId", hospitalId);
                Hospital hospital = adminService.getHospitalByAdmin(admin.getAdminId());
                result.put("hospitalName", hospital.getHospitalName());
            }
        } catch (RuntimeException e) {
            rateLimitService.recordFailure(attemptKey);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/{adminId}")
    public Map<String, Object> getAdminInfo(@PathVariable String adminId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Admin admin = adminService.findById(adminId);
            
            result.put("success", true);
            result.put("adminId", admin.getAdminId());
            result.put("username", admin.getUsername());
            result.put("realName", admin.getRealName());
            result.put("hospitalId", admin.getHospitalId());
            result.put("role", admin.getRole());
            result.put("status", admin.getStatus());
            
            if (admin.getHospitalId() != null && !admin.getHospitalId().isEmpty()) {
                Hospital hospital = adminService.getHospitalByAdmin(adminId);
                result.put("hospitalName", hospital.getHospitalName());
            }
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listAdmins(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 院管只能看到本院管理员，避免跨院查看
            if (authContext.isHospitalAdmin()) {
                hospitalId = authContext.hospitalId();
            }

            List<Admin> admins;
            if (hospitalId != null && !hospitalId.isEmpty()) {
                if (role != null && !role.isEmpty()) {
                    admins = adminService.findByHospitalIdAndRole(hospitalId, role);
                } else {
                    admins = adminService.findByHospitalId(hospitalId);
                }
            } else if (role != null && !role.isEmpty()) {
                admins = adminService.findByRole(role);
            } else {
                admins = adminService.findAll();
            }

            if (auditStatus != null) {
                admins = admins.stream()
                        .filter(a -> auditStatus.equals(a.getAuditStatus()))
                        .toList();
            }
            if (keyword != null && !keyword.isEmpty()) {
                admins = admins.stream()
                        .filter(a -> (a.getUsername() != null && a.getUsername().contains(keyword))
                                || (a.getRealName() != null && a.getRealName().contains(keyword)))
                        .toList();
            }

            long total = admins.size();
            int safeSize = Math.min(Math.max(size, 1), 100);
            int start = Math.max(page, 0) * safeSize;
            int end = Math.min(start + safeSize, admins.size());
            List<Admin> pageAdmins = start < admins.size() ? admins.subList(start, end) : new java.util.ArrayList<>();

            List<Map<String, Object>> adminList = new java.util.ArrayList<>();
            for (Admin admin : pageAdmins) {
                Map<String, Object> adminMap = new HashMap<>();
                adminMap.put("adminId", admin.getAdminId());
                adminMap.put("username", admin.getUsername());
                adminMap.put("realName", admin.getRealName());
                adminMap.put("phone", admin.getPhone());
                adminMap.put("role", admin.getRole());
                adminMap.put("hospitalId", admin.getHospitalId());
                adminMap.put("status", admin.getStatus());
                adminMap.put("auditStatus", admin.getAuditStatus());
                adminMap.put("auditComment", admin.getAuditComment());
                adminList.add(adminMap);
            }

            result.put("success", true);
            result.put("data", adminList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", safeSize);
            result.put("totalPages", (int) Math.ceil((double) total / safeSize));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/create")
    public Map<String, Object> createAdmin(@RequestBody Admin admin, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 首批管理员确定后，任何新增管理员都必须走申请：统一进入待审核
            // 院管只能为自家医院申请，不能指定其他医院
            admin.setRole("HOSPITAL_ADMIN");
            admin.setStatus(1);
            admin.setAuditStatus(0);
            if (authContext.isHospitalAdmin()) {
                admin.setHospitalId(authContext.hospitalId());
            }

            Admin created = adminService.create(admin);
            auditLogService.record("ADMIN_APPLY", "admin", created.getAdminId(),
                    "username=" + admin.getUsername() + ", hospitalId=" + created.getHospitalId());
            result.put("success", true);
            result.put("message", "管理员申请已提交，待卫健委审核");
            result.put("adminId", created.getAdminId());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PutMapping("/{adminId}")
    public Map<String, Object> updateAdmin(@PathVariable String adminId, @RequestBody Admin admin) {
        Map<String, Object> result = new HashMap<>();

        try {
            Admin updated = adminService.update(adminId, admin);
            auditLogService.record("ADMIN_UPDATE", "admin", adminId, null);
            result.put("success", true);
            result.put("message", "管理员信息更新成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/{adminId}")
    public Map<String, Object> deleteAdmin(@PathVariable String adminId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (adminId != null && adminId.equals(authContext.ownerId())) {
                throw new RuntimeException("不能删除当前登录账号");
            }
            adminService.delete(adminId);
            auditLogService.record("ADMIN_DELETE", "admin", adminId, null);
            result.put("success", true);
            result.put("message", "管理员删除成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/{adminId}/generate-key")
    public Map<String, Object> generateApiKey(@PathVariable String adminId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String apiKey = adminService.generateApiKey(adminId);
            auditLogService.record("ADMIN_KEY_GENERATE", "admin", adminId, null);
            result.put("success", true);
            result.put("message", "API密钥生成成功");
            result.put("apiKey", apiKey);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/{adminId}/renew-key")
    public Map<String, Object> renewApiKey(@PathVariable String adminId) {
        Map<String, Object> result = new HashMap<>();

        try {
            adminService.renewApiKey(adminId);
            result.put("success", true);
            result.put("message", "API密钥已续期");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/{adminId}/invalidate-key")
    public Map<String, Object> invalidateApiKey(@PathVariable String adminId) {
        Map<String, Object> result = new HashMap<>();

        try {
            adminService.invalidateApiKey(adminId);
            result.put("success", true);
            result.put("message", "API密钥已失效");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/audit/list")
    public Map<String, Object> listPendingAudit(
            @RequestParam(required = false) Integer auditStatus) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Admin> admins;
            if (auditStatus != null) {
                admins = adminService.findByAuditStatus(auditStatus);
            } else {
                admins = adminService.findByAuditStatus(0);
            }

            result.put("success", true);
            result.put("data", admins);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/{adminId}/audit")
    public Map<String, Object> auditAdmin(@PathVariable String adminId, @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            authContext.requireSuperAdmin();
            Integer auditStatus = (Integer) request.get("auditStatus");
            String comment = (String) request.get("comment");

            adminService.audit(adminId, auditStatus, comment);
            auditLogService.record("ADMIN_AUDIT", "admin", adminId,
                    "auditStatus=" + auditStatus + ", comment=" + comment);
            result.put("success", true);
            result.put("message", auditStatus == 1 ? "审核通过" : "审核拒绝");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 医院准入列表（卫健委专用）：含待审核、已启用、已驳回，支持省市区与关键字筛选。
     */
    @GetMapping("/hospitals/page")
    public Map<String, Object> listHospitalsForAudit(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.putAll(hospitalService.findForAdmin(status, province, city, district, keyword, page, size));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 医院准入审批（卫健委专用）：status=1 通过并启用，status=2 驳回。
     */
    @PostMapping("/hospitals/{hospitalId}/audit")
    public Map<String, Object> auditHospital(@PathVariable String hospitalId,
                                             @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            authContext.requireSuperAdmin();
            Object statusValue = request.get("status");
            if (statusValue == null) {
                throw new RuntimeException("请选择审批结果");
            }
            Integer status = Integer.valueOf(statusValue.toString());
            if (status != 1 && status != 2) {
                throw new RuntimeException("审批结果只能是通过或驳回");
            }
            String comment = request.get("comment") == null ? null : request.get("comment").toString();

            Hospital hospital = hospitalService.audit(hospitalId, status);
            auditLogService.record("HOSPITAL_AUDIT", "hospital", hospitalId,
                    "status=" + status + ", comment=" + comment);
            result.put("success", true);
            result.put("message", status == 1 ? "医院准入已通过" : "医院准入已驳回");
            result.put("data", hospital);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}