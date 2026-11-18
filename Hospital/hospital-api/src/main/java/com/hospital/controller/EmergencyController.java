package com.hospital.controller;

import com.hospital.entity.EmergencyLog;
import com.hospital.service.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyController {

    private static final List<String> ALLOWED_CERT_EXT = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_CERT_BYTES = 8L * 1024 * 1024;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private com.hospital.security.AuthContext authContext;

    @Autowired
    private com.hospital.security.TokenService tokenService;

    @Autowired
    private com.hospital.service.EmergencyGuardService emergencyGuardService;

    @Autowired
    private com.hospital.service.EmergencySnapshotService emergencySnapshotService;

    @Autowired
    private com.hospital.service.AuditLogService auditLogService;

    @Autowired
    private com.hospital.repository.AdminRepository adminRepository;

    /** 查询本院应急通道使用记录（院管知会/事后核对）。 */
    @GetMapping("/requests/{hospitalId}")
    public Map<String, Object> listRequests(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireHospitalScope(hospitalId);
            result.put("success", true);
            result.put("data", emergencyService.listRequests(hospitalId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 医生提交应急权限申请：资格校验通过后立即开通 2 小时权限，不经任何审批。
     */
    @PostMapping("/request/submit")
    public Map<String, Object> submitRequest(@RequestBody(required = false) Map<String, String> request,
                                             HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        try {
            String doctorId = authContext.ownerId();
            if (doctorId == null || doctorId.isEmpty()) {
                throw new RuntimeException("当前登录医生信息缺失，请重新登录");
            }
            String reason = request == null ? null : request.get("reason");
            String password = request == null ? null : request.get("password");
            String certIdPath = request == null ? null : request.get("certIdPath");
            String certLicensePath = request == null ? null : request.get("certLicensePath");
            result.putAll(emergencyService.submitRequest(doctorId, password, getClientIp(httpRequest),
                    reason, certIdPath, certLicensePath));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生查询自己最近一次应急申请、待交报告与资格状态。 */
    @GetMapping("/request/my")
    public Map<String, Object> myRequest() {
        Map<String, Object> result = new HashMap<>();
        try {
            String doctorId = authContext.ownerId();
            if (doctorId == null || doctorId.isEmpty()) {
                throw new RuntimeException("当前登录医生信息缺失，请重新登录");
            }
            result.putAll(emergencyService.getMyRequest(doctorId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生上传应急证件（身份证人像面 / 医师资格证），归档复用。 */
    @PostMapping("/request/cert")
    public Map<String, Object> uploadCert(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "type", required = false) String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            String doctorId = authContext.ownerId();
            if (doctorId == null || doctorId.isEmpty()) {
                throw new RuntimeException("当前登录医生信息缺失，请重新登录");
            }
            if (doctorId.contains("/") || doctorId.contains("..")) {
                throw new RuntimeException("医生编号不合法");
            }
            if (!"idCard".equals(type) && !"license".equals(type)) {
                throw new RuntimeException("证件类型仅支持 idCard 或 license");
            }
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("请选择证件图片");
            }
            if (file.getSize() > MAX_CERT_BYTES) {
                throw new RuntimeException("证件图片不能超过 8MB");
            }
            String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            int dot = original.lastIndexOf('.');
            String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "";
            if (!ALLOWED_CERT_EXT.contains(ext)) {
                throw new RuntimeException("仅支持 jpg / png / webp 图片");
            }

            Path dir = Paths.get("emergency-certs", doctorId).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File target = dir.resolve(fileName).toFile();
            file.transferTo(target);

            result.put("success", true);
            result.put("path", doctorId + "/" + fileName);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 结束报告草稿：自动生成改动范围。 */
    @GetMapping("/request/report-draft")
    public Map<String, Object> reportDraft(@RequestParam("requestId") String requestId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String doctorId = authContext.ownerId();
            if (doctorId == null || doctorId.isEmpty()) {
                throw new RuntimeException("当前登录医生信息缺失，请重新登录");
            }
            result.putAll(emergencyService.getReportDraft(doctorId, requestId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生提交结束报告。 */
    @PostMapping("/request/report")
    public Map<String, Object> submitReport(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String doctorId = authContext.ownerId();
            if (doctorId == null || doctorId.isEmpty()) {
                throw new RuntimeException("当前登录医生信息缺失，请重新登录");
            }
            String requestId = request == null ? null : String.valueOf(request.get("requestId"));
            String scope = request == null ? null : (String) request.get("scope");
            String impact = request == null ? null : (String) request.get("impact");
            boolean complete = Boolean.TRUE.equals(request == null ? null : request.get("complete"));
            result.putAll(emergencyService.submitReport(doctorId, requestId, scope, impact, complete));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 受控查看证件图片：仅院管/卫健委，访问留痕，应急账号拒绝。 */
    @GetMapping("/cert/{requestId}/{type}")
    public ResponseEntity<Object> viewCert(@PathVariable String requestId, @PathVariable String type) {
        try {
            rejectEmergencyAccount();
            String hospitalId = emergencyService.getRequestHospitalId(requestId);
            if (hospitalId == null) {
                throw new RuntimeException("申请不存在或已被清理");
            }
            authContext.requireHospitalScope(hospitalId);

            Path path = emergencyService.getCertPath(requestId, type);
            String fileName = path.getFileName().toString().toLowerCase();
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (fileName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (fileName.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            auditLogService.record("EMERGENCY_CERT_VIEW", "admin", authContext.ownerId(),
                    "request=" + requestId + ", type=" + type);

            Resource resource = new FileSystemResource(path);
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }

    /** 院管/卫健委记录证件核对结论。 */
    @PostMapping("/request/{requestId}/verify-cert")
    public Map<String, Object> verifyCert(@PathVariable String requestId,
                                          @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            String hospitalId = emergencyService.getRequestHospitalId(requestId);
            if (hospitalId == null) {
                throw new RuntimeException("申请不存在或已被清理");
            }
            authContext.requireHospitalScope(hospitalId);
            boolean verified = Boolean.TRUE.equals(request == null ? null : request.get("verified"));
            String remark = request == null ? null : (String) request.get("remark");
            emergencyService.verifyCert(requestId, verified, remark, authContext.ownerId(), resolveAdminName());
            result.put("success", true);
            result.put("message", verified ? "已记录：证件与本人一致" : "已记录：证件与本人不一致");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 院管/卫健委强制终止应急权限。 */
    @PostMapping("/request/{requestId}/terminate")
    public Map<String, Object> terminate(@PathVariable String requestId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            String hospitalId = emergencyService.getRequestHospitalId(requestId);
            if (hospitalId == null) {
                throw new RuntimeException("申请不存在或已被清理");
            }
            authContext.requireHospitalScope(hospitalId);
            result.putAll(emergencyService.terminate(requestId, authContext.ownerId(), resolveAdminName()));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 卫健委待确认队列（跨医院）。 */
    @GetMapping("/supervisor/pending")
    public Map<String, Object> supervisorPending() {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireSuperAdmin();
            result.put("success", true);
            result.put("data", emergencyService.supervisorPending());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 卫健委确认归档。 */
    @PostMapping("/supervisor/{requestId}/confirm")
    public Map<String, Object> confirm(@PathVariable String requestId,
                                       @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireSuperAdmin();
            boolean certChecked = Boolean.TRUE.equals(request == null ? null : request.get("certChecked"));
            String remark = request == null ? null : (String) request.get("remark");
            result.putAll(emergencyService.confirm(requestId, certChecked, remark,
                    authContext.ownerId(), resolveAdminName()));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 卫健委驳回异常。 */
    @PostMapping("/supervisor/{requestId}/reject")
    public Map<String, Object> reject(@PathVariable String requestId,
                                      @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireSuperAdmin();
            String reason = request == null ? null : (String) request.get("reason");
            result.putAll(emergencyService.reject(requestId, reason,
                    authContext.ownerId(), resolveAdminName()));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private String resolveAdminName() {
        String adminId = authContext.ownerId();
        if (adminId == null || adminId.isEmpty()) {
            return "管理员";
        }
        return adminRepository.findById(adminId)
                .map(admin -> admin.getRealName() != null && !admin.getRealName().isEmpty()
                        ? admin.getRealName()
                        : (admin.getUsername() != null ? admin.getUsername() : adminId))
                .orElse(adminId);
    }

    /** 应急操作快照列表（院管/超管），用于回滚被应急账号修改的数据。 */
    @GetMapping("/snapshots/{hospitalId}")
    public Map<String, Object> snapshots(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireHospitalScope(hospitalId);
            result.put("success", true);
            result.put("data", emergencySnapshotService.list(hospitalId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 一键回滚某个应急快照（院管/超管）。 */
    @PostMapping("/snapshots/{snapshotId}/restore")
    public Map<String, Object> restoreSnapshot(@PathVariable String snapshotId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            String hospitalId = emergencySnapshotService.getHospitalId(snapshotId);
            if (hospitalId == null) {
                throw new RuntimeException("快照不存在或已过期");
            }
            authContext.requireHospitalScope(hospitalId);
            emergencySnapshotService.restore(snapshotId);
            result.put("success", true);
            result.put("message", "已回滚到该快照状态");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private void rejectEmergencyAccount() {
        if (authContext.isEmergencyAdmin()) {
            throw new RuntimeException("应急账号无权执行该操作");
        }
    }

    /** 查询本院应急冻结状态（院管/超管）。 */
    @GetMapping("/status/{hospitalId}")
    public Map<String, Object> status(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireHospitalScope(hospitalId);
            result.put("success", true);
            result.put("locked", emergencyGuardService.isLocked(hospitalId));
            result.put("lockReason", emergencyGuardService.getLockReason(hospitalId));
            result.put("lockUntil", emergencyGuardService.getLockUntil(hospitalId));
            result.put("todayWrites", emergencyGuardService.getTodayWrites(hospitalId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 一键冻结本院所有应急权限（院管/超管），并立即吊销已签发的应急令牌。 */
    @PostMapping("/revoke-all/{hospitalId}")
    public Map<String, Object> revokeAll(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireHospitalScope(hospitalId);
            emergencyGuardService.freeze(hospitalId, 24, "管理员手动冻结");
            tokenService.revokeEmergencyByHospital(hospitalId);
            result.put("success", true);
            result.put("message", "已冻结本院应急权限，所有应急令牌已失效");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 解除冻结（院管/超管）。 */
    @PostMapping("/unfreeze/{hospitalId}")
    public Map<String, Object> unfreeze(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            rejectEmergencyAccount();
            authContext.requireHospitalScope(hospitalId);
            emergencyGuardService.unfreeze(hospitalId);
            result.put("success", true);
            result.put("message", "已解除应急冻结");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();

        try {
            String hospitalId = request.get("hospitalId");
            String dailyKey = request.get("dailyKey");

            String ipAddress = getClientIp(httpRequest);

            Map<String, Object> loginResult = emergencyService.login(hospitalId, dailyKey, ipAddress);
            result.putAll(loginResult);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody(required = false) Map<String, String> request,
                                      HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();

        try {
            String token = request != null ? request.get("token") : null;
            if (token == null || token.isEmpty()) {
                String authorization = httpRequest.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    token = authorization.substring(7).trim();
                }
            }
            if (token == null || token.isEmpty()) {
                token = httpRequest.getHeader("X-Auth-Token");
            }
            emergencyService.logout(token);
            result.put("success", true);
            result.put("message", "退出成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/validate-token")
    public Map<String, Object> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return emergencyService.validateToken(token);
    }

    @PostMapping("/generate-key/{hospitalId}")
    public Map<String, Object> generateDailyKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String dailyKey = emergencyService.generateDailyKey(hospitalId);
            result.put("success", true);
            result.put("message", "密钥生成成功");
            result.put("dailyKey", dailyKey);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/renew-key/{hospitalId}")
    public Map<String, Object> renewDailyKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            emergencyService.renewDailyKey(hospitalId);
            result.put("success", true);
            result.put("message", "密钥已续期");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/invalidate-key/{hospitalId}")
    public Map<String, Object> invalidateDailyKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            emergencyService.invalidateDailyKey(hospitalId);
            result.put("success", true);
            result.put("message", "密钥已作废");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/logs")
    public Map<String, Object> getEmergencyLogs(
            @RequestParam(required = false) String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<EmergencyLog> logs;
            if (hospitalId != null && !hospitalId.isEmpty()) {
                logs = emergencyService.getEmergencyLogsByHospital(hospitalId);
            } else {
                logs = emergencyService.getEmergencyLogs();
            }
            result.put("success", true);
            result.put("data", logs);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
