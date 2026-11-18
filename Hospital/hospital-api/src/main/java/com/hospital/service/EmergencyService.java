package com.hospital.service;

import com.hospital.entity.Doctor;
import com.hospital.entity.EmergencyLog;
import com.hospital.entity.EmergencyOperation;
import com.hospital.entity.EmergencyRequest;
import com.hospital.entity.Hospital;
import com.hospital.entity.HospitalKey;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.EmergencyLogRepository;
import com.hospital.repository.EmergencyOperationRepository;
import com.hospital.repository.EmergencyRequestRepository;
import com.hospital.repository.HospitalKeyRepository;
import com.hospital.repository.HospitalRepository;
import com.hospital.security.PasswordUtil;
import com.hospital.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 应急通道：申请即开通 2 小时权限，院管只做同步知会与事后核对，
 * 卫健委在结束报告提交后 2 小时内确认归档或驳回异常。
 */
@Service
public class EmergencyService {

    @Autowired
    private HospitalKeyRepository hospitalKeyRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private EmergencyLogRepository emergencyLogRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private EmergencyGuardService emergencyGuardService;

    @Autowired
    private EmergencyRequestRepository emergencyRequestRepository;

    @Autowired
    private EmergencyOperationRepository emergencyOperationRepository;

    /** 每院每日医生应急申请上限 */
    private static final int HOSPITAL_DAILY_LIMIT = 3;
    /** 同一医生申请间隔 */
    private static final int DOCTOR_REQUEST_HOURS = 24;
    /** 应急权限硬上限 */
    private static final int TOKEN_HOURS = 2;
    /** 结束报告提交时限 */
    private static final int REPORT_HOURS = 24;
    /** 卫健委确认时限 */
    private static final int CONFIRM_HOURS = 2;
    /** 证件归档目录（不对外静态暴露） */
    private static final Path CERT_ROOT = Paths.get("emergency-certs").toAbsolutePath().normalize();

    /**
     * 医生提交应急申请：资格校验通过后立即签发 2 小时权限，不等待任何审批。
     */
    public Map<String, Object> submitRequest(String doctorId, String password, String ipAddress,
                                             String reason, String certIdPath, String certLicensePath) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生信息不存在，请重新登录"));

        if (!isEligible(doctor)) {
            throw new RuntimeException("应急通道仅限主任医师及以上职称的注册医生使用");
        }

        if (reason == null || reason.trim().length() < 4) {
            throw new RuntimeException("请填写至少 4 个字的申请原因，便于事后审计");
        }

        if (password == null || !passwordUtil.matches(password, doctor.getPassword())) {
            throw new RuntimeException("身份校验失败：请输入正确的医生账号密码");
        }

        String hospitalId = doctor.getHospitalId();
        if (hospitalId == null || hospitalId.isEmpty()) {
            throw new RuntimeException("当前医生未绑定医院，无法申请应急权限");
        }

        if (emergencyGuardService.isLocked(hospitalId)) {
            throw new RuntimeException("本院应急权限已被冻结，请联系管理员解冻");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("医院不存在"));

        LocalDateTime now = LocalDateTime.now();

        for (EmergencyRequest item : emergencyRequestRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId)) {
            if (item.getStatus() != null && item.getStatus() == 6) {
                throw new RuntimeException("您有未提交的应急结束报告，请先补交后再申请");
            }
        }

        List<Integer> overdueConfirm = Arrays.asList(7);
        if (!emergencyRequestRepository
                .findByStatusInAndHospitalIdOrderByCreateTimeDesc(overdueConfirm, hospitalId).isEmpty()) {
            throw new RuntimeException("本院有应急事项待卫健委确认，已暂停新的自助申请");
        }

        // 驳回冻结是内存态，服务重启会丢失；用最近的驳回记录兜底 24 小时
        for (EmergencyRequest item : emergencyRequestRepository
                .findByStatusInAndHospitalIdOrderByCreateTimeDesc(Arrays.asList(4), hospitalId)) {
            if (item.getConfirmTime() != null && item.getConfirmTime().isAfter(now.minusHours(24))) {
                throw new RuntimeException("本院近期有应急异常被驳回，已暂停新的自助申请");
            }
        }

        for (EmergencyRequest item : emergencyRequestRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId)) {
            if (item.getCreateTime() != null
                    && item.getCreateTime().isAfter(now.minusHours(DOCTOR_REQUEST_HOURS))) {
                throw new RuntimeException("同一医生 24 小时内只能提交一次应急申请");
            }
        }

        int todayCount = 0;
        for (EmergencyRequest item : emergencyRequestRepository.findByHospitalIdOrderByCreateTimeDesc(hospitalId)) {
            if (item.getCreateTime() != null && item.getCreateTime().toLocalDate().equals(now.toLocalDate())) {
                todayCount++;
            }
        }
        if (todayCount >= HOSPITAL_DAILY_LIMIT) {
            throw new RuntimeException("本院今日医生应急申请已达 " + HOSPITAL_DAILY_LIMIT + " 次上限，请联系管理员");
        }

        String[] certs = resolveCertificates(doctorId, certIdPath, certLicensePath);

        EmergencyRequest request = new EmergencyRequest();
        request.setId("REQ" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        request.setHospitalId(hospitalId);
        request.setDoctorId(doctorId);
        request.setDoctorName(doctor.getName() != null ? doctor.getName() : doctorId);
        request.setDoctorTitle(doctor.getTitle());
        request.setReason(reason.trim());
        request.setStatus(0);
        request.setAutoGrant(1);
        request.setCertVerified(0);
        request.setCertIdPath(certs[0]);
        request.setCertLicensePath(certs[1]);
        request.setCreateTime(now);
        request.setTokenExpireTime(now.plusHours(TOKEN_HOURS));

        String token = generateToken(hospitalId);
        request.setToken(token);
        emergencyRequestRepository.save(request);

        EmergencyLog log = new EmergencyLog(
                "LOG" + System.currentTimeMillis(),
                hospitalId,
                hospital.getHospitalName(),
                ipAddress,
                request.getTokenExpireTime()
        );
        log.setToken(token);
        log.setOperatorType("DOCTOR");
        log.setOperatorId(doctorId);
        emergencyLogRepository.save(log);

        tokenService.register(token, "ADMIN", log.getLogId(), "EMERGENCY_ADMIN", hospitalId,
                System.currentTimeMillis() + TOKEN_HOURS * 60L * 60 * 1000);

        auditLogService.record("EMERGENCY_REQUEST_SUBMIT", "doctor", doctorId,
                "request=" + request.getId() + ", hospital=" + hospitalId + ", reason=" + reason.trim()
                        + ", autoGrant=true");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "应急权限已开通，有效期 2 小时；结束后 24 小时内须提交报告");
        result.put("requestId", request.getId());
        result.put("status", 0);
        result.put("token", token);
        result.put("expireTime", request.getTokenExpireTime().toString());
        result.put("hospitalId", hospitalId);
        result.put("hospitalName", hospital.getHospitalName());
        result.put("certReady", true);
        return result;
    }

    /**
     * 医生查询自己最近一次应急申请及报告状态。
     */
    public Map<String, Object> getMyRequest(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        boolean eligible = isEligible(doctor);
        boolean certReady = hasArchivedCerts(doctorId);

        List<EmergencyRequest> mine = emergencyRequestRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId);
        if (mine.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", "NONE");
            result.put("statusText", "尚未提交过应急申请");
            result.put("eligible", eligible);
            result.put("eligibleMessage", eligible ? "" : "应急通道仅限主任医师及以上职称的注册医生使用");
            result.put("certReady", certReady);
            return result;
        }

        EmergencyRequest request = mine.get(0);
        applyTimeouts(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.putAll(buildRequestItem(request));
        result.put("eligible", eligible);
        result.put("eligibleMessage", eligible ? "" : "应急通道仅限主任医师及以上职称的注册医生使用");
        result.put("certReady", certReady || hasCerts(request));
        result.put("reason", request.getReason());
        result.put("doctorName", request.getDoctorName());
        result.put("doctorTitle", request.getDoctorTitle());

        if (request.getStatus() != null && request.getStatus() == 0 && request.getToken() != null) {
            result.put("token", request.getToken());
            result.put("expireTime", request.getTokenExpireTime() == null
                    ? null : request.getTokenExpireTime().toString());
        }
        return result;
    }

    /** 院管查看本院应急通道使用记录（知会与事后核对用）。 */
    public List<Map<String, Object>> listRequests(String hospitalId) {
        List<Integer> statuses = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
        List<EmergencyRequest> requests = emergencyRequestRepository
                .findByStatusInAndHospitalIdOrderByCreateTimeDesc(statuses, hospitalId);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmergencyRequest request : requests) {
            if (request.getCreateTime() != null && request.getCreateTime().isBefore(sevenDaysAgo)) {
                continue;
            }
            applyTimeouts(request);
            Map<String, Object> item = buildRequestItem(request);
            result.add(item);
            if (result.size() >= 50) {
                break;
            }
        }
        return result;
    }

    /** 结束报告草稿：系统按应急写操作自动生成改动范围，医生核对补充。 */
    public Map<String, Object> getReportDraft(String doctorId, String requestId) {
        EmergencyRequest request = requireOwnedRequest(doctorId, requestId);
        if (request.getStatus() == null || !Arrays.asList(0, 1, 6).contains(request.getStatus())) {
            throw new RuntimeException("当前申请状态无需提交结束报告");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("requestId", request.getId());
        result.put("status", request.getStatus());
        result.put("statusText", requestStatusText(request.getStatus()));
        result.put("autoScope", buildAutoScope(request));
        result.put("reason", request.getReason());
        result.put("startTime", request.getCreateTime() == null ? null : request.getCreateTime().toString());
        result.put("expireTime", request.getTokenExpireTime() == null
                ? null : request.getTokenExpireTime().toString());
        result.put("reportDeadline", request.getReportDeadline() == null
                ? null : request.getReportDeadline().toString());
        result.put("certVerified", request.getCertVerified());
        return result;
    }

    /** 医生提交结束报告：改动范围、影响预估、确认无未完成事项。 */
    public Map<String, Object> submitReport(String doctorId, String requestId, String scope,
                                            String impact, boolean complete) {
        EmergencyRequest request = requireOwnedRequest(doctorId, requestId);
        if (request.getStatus() == null || !Arrays.asList(0, 1, 6).contains(request.getStatus())) {
            throw new RuntimeException("当前申请状态无需提交结束报告");
        }
        if (impact == null || impact.trim().length() < 4) {
            throw new RuntimeException("请填写至少 4 个字的影响预估");
        }
        if (!complete) {
            throw new RuntimeException("请确认已无未完成事项后再提交");
        }

        LocalDateTime now = LocalDateTime.now();
        String autoScope = buildAutoScope(request);
        String finalScope = scope == null || scope.trim().isEmpty() ? autoScope : scope.trim();

        if (request.getStatus() == 0) {
            request.setEndTime(now);
            request.setEndReason("医生提前提交结束报告");
            tokenService.revoke(request.getToken());
            emergencyGuardService.clearToken(request.getToken());
        }

        request.setReportScope(finalScope);
        request.setReportImpact(impact.trim());
        request.setReportComplete(1);
        request.setReportTime(now);
        request.setConfirmDeadline(now.plusHours(CONFIRM_HOURS));
        request.setStatus(2);
        emergencyRequestRepository.save(request);

        auditLogService.record("EMERGENCY_REPORT_SUBMIT", "doctor", doctorId,
                "request=" + requestId + ", impact=" + impact.trim());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "结束报告已提交，等待卫健委确认");
        result.put("requestId", requestId);
        result.put("status", 2);
        result.put("confirmDeadline", request.getConfirmDeadline().toString());
        return result;
    }

    /** 院管/卫健委记录证件核对结论：1 一致，2 不一致。 */
    public void verifyCert(String requestId, boolean verified, String remark,
                           String verifierId, String verifierName) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (request.getStatus() == null || !Arrays.asList(0, 1, 2, 6, 7).contains(request.getStatus())) {
            throw new RuntimeException("该申请已归档或已终止，不能再修改证件核对结论");
        }
        if (request.getCertIdPath() == null || request.getCertLicensePath() == null) {
            throw new RuntimeException("该申请缺少证件资料，无法核对");
        }
        request.setCertVerified(verified ? 1 : 2);
        request.setCertVerifiedBy(verifierId);
        request.setCertVerifiedTime(LocalDateTime.now());
        request.setCertVerifyRemark(remark);
        emergencyRequestRepository.save(request);

        auditLogService.record("EMERGENCY_CERT_VERIFY", "admin", verifierId,
                "request=" + requestId + ", verified=" + verified + ", by=" + verifierName);
    }

    /** 院管/卫健委强制终止应急权限。 */
    public Map<String, Object> terminate(String requestId, String operatorId, String operatorName) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (request.getStatus() == null || Arrays.asList(3, 4, 5).contains(request.getStatus())) {
            throw new RuntimeException("该申请已归档或已终止");
        }

        tokenService.revoke(request.getToken());
        emergencyGuardService.clearToken(request.getToken());
        request.setStatus(5);
        request.setEndTime(LocalDateTime.now());
        request.setEndReason("管理员终止：" + operatorName);
        emergencyRequestRepository.save(request);

        auditLogService.record("EMERGENCY_REQUEST_TERMINATE", "admin", operatorId,
                "request=" + requestId + ", by=" + operatorName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已终止该应急权限");
        result.put("requestId", requestId);
        return result;
    }

    /** 卫健委待确认队列（跨医院）。 */
    public List<Map<String, Object>> supervisorPending() {
        // 2/7 待确认；6 为逾期未交报告，进入异常队列供卫健委知悉
        List<Integer> statuses = Arrays.asList(2, 6, 7);
        List<EmergencyRequest> requests = emergencyRequestRepository
                .findByStatusInOrderByCreateTimeDesc(statuses);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmergencyRequest request : requests) {
            applyTimeouts(request);
            if (request.getStatus() == null || !Arrays.asList(2, 6, 7).contains(request.getStatus())) {
                continue;
            }
            Map<String, Object> item = buildRequestItem(request);
            Hospital hospital = hospitalRepository.findById(request.getHospitalId()).orElse(null);
            item.put("hospitalId", request.getHospitalId());
            item.put("hospitalName", hospital == null ? request.getHospitalId() : hospital.getHospitalName());
            item.put("overdue", request.getStatus() == 7);
            item.put("reportMissing", request.getStatus() == 6);
            result.add(item);
        }
        return result;
    }

    /** 卫健委确认归档。 */
    public Map<String, Object> confirm(String requestId, boolean certChecked, String remark,
                                       String operatorId, String operatorName) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (request.getStatus() == null || !Arrays.asList(2, 7).contains(request.getStatus())) {
            throw new RuntimeException("该申请当前不可确认");
        }
        if (!certChecked) {
            throw new RuntimeException("请先核对身份证、医师资格证与本人一致");
        }
        if (request.getCertIdPath() == null || request.getCertLicensePath() == null) {
            throw new RuntimeException("该申请缺少证件资料，不能确认归档");
        }
        if (request.getCertVerified() != null && request.getCertVerified() == 2) {
            throw new RuntimeException("证件核对不一致，不能确认归档，请驳回并按异常处理");
        }

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(3);
        request.setCertVerified(1);
        request.setCertVerifiedBy(operatorId);
        request.setCertVerifiedTime(now);
        request.setConfirmBy(operatorId);
        request.setConfirmByName(operatorName);
        request.setConfirmTime(now);
        request.setConfirmRemark(remark);
        emergencyRequestRepository.save(request);

        auditLogService.record("EMERGENCY_SUPERVISOR_CONFIRM", "admin", operatorId,
                "request=" + requestId + ", by=" + operatorName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已确认归档");
        result.put("requestId", requestId);
        return result;
    }

    /** 卫健委驳回异常：吊销令牌并冻结本院应急通道。 */
    public Map<String, Object> reject(String requestId, String reason,
                                      String operatorId, String operatorName) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (request.getStatus() == null || !Arrays.asList(2, 7).contains(request.getStatus())) {
            throw new RuntimeException("该申请当前不可驳回");
        }
        if (reason == null || reason.trim().length() < 2) {
            throw new RuntimeException("请填写驳回原因");
        }

        String hospitalId = request.getHospitalId();
        tokenService.revoke(request.getToken());
        emergencyGuardService.freeze(hospitalId, 24, "卫健委驳回应急事项：" + reason.trim());
        tokenService.revokeEmergencyByHospital(hospitalId);

        request.setStatus(4);
        request.setConfirmBy(operatorId);
        request.setConfirmByName(operatorName);
        request.setConfirmTime(LocalDateTime.now());
        request.setConfirmRemark(reason.trim());
        request.setEndTime(LocalDateTime.now());
        request.setEndReason("卫健委驳回：" + reason.trim());
        emergencyRequestRepository.save(request);

        auditLogService.record("EMERGENCY_SUPERVISOR_REJECT", "admin", operatorId,
                "request=" + requestId + ", hospital=" + hospitalId + ", reason=" + reason.trim());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已驳回并按重大异常处理，本院应急通道已冻结");
        result.put("requestId", requestId);
        return result;
    }

    /** 定时任务：处理权限到期、报告逾期、确认逾期。 */
    @Transactional
    public void processTimeouts() {
        LocalDateTime now = LocalDateTime.now();

        for (EmergencyRequest request : emergencyRequestRepository
                .findByStatusAndTokenExpireTimeBefore(0, now)) {
            request.setStatus(1);
            request.setEndTime(request.getTokenExpireTime());
            request.setEndReason("应急权限到期");
            request.setReportDeadline(request.getTokenExpireTime().plusHours(REPORT_HOURS));
            emergencyRequestRepository.save(request);
            auditLogService.record("EMERGENCY_REQUEST_EXPIRED", "system", request.getDoctorId(),
                    "request=" + request.getId() + ", reportDeadline=" + request.getReportDeadline());
        }

        for (EmergencyRequest request : emergencyRequestRepository
                .findByStatusAndReportDeadlineBefore(1, now)) {
            request.setStatus(6);
            emergencyRequestRepository.save(request);
            auditLogService.record("EMERGENCY_REPORT_OVERDUE", "system", request.getDoctorId(),
                    "request=" + request.getId() + ", deadline=" + request.getReportDeadline());
        }

        for (EmergencyRequest request : emergencyRequestRepository
                .findByStatusAndConfirmDeadlineBefore(2, now)) {
            request.setStatus(7);
            emergencyRequestRepository.save(request);
            auditLogService.record("EMERGENCY_CONFIRM_OVERDUE", "system", request.getHospitalId(),
                    "request=" + request.getId() + ", deadline=" + request.getConfirmDeadline());
        }
    }

    /** 返回证件文件绝对路径，供受控查看接口输出图片流。 */
    public Path getCertPath(String requestId, String type) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (!"idCard".equalsIgnoreCase(type) && !"license".equalsIgnoreCase(type)) {
            throw new RuntimeException("证件类型不合法");
        }
        String path = "license".equalsIgnoreCase(type) ? request.getCertLicensePath() : request.getCertIdPath();
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("该申请未上传对应证件");
        }
        Path resolved = CERT_ROOT.resolve(path).normalize();
        if (!resolved.startsWith(CERT_ROOT) || !Files.exists(resolved)) {
            throw new RuntimeException("证件文件不存在");
        }
        return resolved;
    }

    /** 业务层做院管范围校验时使用。 */
    public String getRequestHospitalId(String requestId) {
        return emergencyRequestRepository.findById(requestId)
                .map(EmergencyRequest::getHospitalId)
                .orElse(null);
    }

    private boolean isEligible(Doctor doctor) {
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            return false;
        }
        String title = doctor.getTitle() == null ? "" : doctor.getTitle().trim();
        return title.contains("主任医师") && !title.contains("副主任医师");
    }

    private String[] resolveCertificates(String doctorId, String certIdPath, String certLicensePath) {
        if (certIdPath != null && !certIdPath.trim().isEmpty()
                && certLicensePath != null && !certLicensePath.trim().isEmpty()) {
            return new String[]{normalizeCertPath(doctorId, certIdPath), normalizeCertPath(doctorId, certLicensePath)};
        }
        if ((certIdPath != null && !certIdPath.trim().isEmpty())
                || (certLicensePath != null && !certLicensePath.trim().isEmpty())) {
            throw new RuntimeException("身份证人像面与医师资格证需同时上传");
        }

        Optional<EmergencyRequest> archived = emergencyRequestRepository
                .findFirstByDoctorIdAndCertIdPathIsNotNullAndCertLicensePathIsNotNullOrderByCreateTimeDesc(doctorId);
        if (archived.isPresent()) {
            EmergencyRequest item = archived.get();
            if (item.getCertIdPath() != null && item.getCertLicensePath() != null) {
                return new String[]{item.getCertIdPath(), item.getCertLicensePath()};
            }
        }
        throw new RuntimeException("首次使用请上传身份证人像面和医师资格证");
    }

    private String normalizeCertPath(String doctorId, String input) {
        if (input == null) {
            throw new RuntimeException("证件路径不能为空");
        }
        String value = input.trim().replace('\\', '/');
        if (value.contains("..") || value.startsWith("/") || value.contains(":")) {
            throw new RuntimeException("证件路径不合法");
        }
        Path resolved = CERT_ROOT.resolve(value).normalize();
        if (!resolved.startsWith(CERT_ROOT) || !resolved.startsWith(CERT_ROOT.resolve(doctorId))) {
            throw new RuntimeException("证件路径不属于当前医生");
        }
        if (!Files.exists(resolved)) {
            throw new RuntimeException("证件文件不存在，请重新上传");
        }
        return CERT_ROOT.relativize(resolved).toString().replace('\\', '/');
    }

    private boolean hasArchivedCerts(String doctorId) {
        return emergencyRequestRepository
                .findFirstByDoctorIdAndCertIdPathIsNotNullAndCertLicensePathIsNotNullOrderByCreateTimeDesc(doctorId)
                .isPresent();
    }

    private boolean hasCerts(EmergencyRequest request) {
        return request.getCertIdPath() != null && request.getCertLicensePath() != null;
    }

    private EmergencyRequest requireOwnedRequest(String doctorId, String requestId) {
        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在或已被清理"));
        if (!doctorId.equals(request.getDoctorId())) {
            throw new RuntimeException("无权操作他人的应急申请");
        }
        return request;
    }

    private void applyTimeouts(EmergencyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Integer status = request.getStatus();
        if (status == null) {
            return;
        }

        if (status == 0 && request.getTokenExpireTime() != null
                && now.isAfter(request.getTokenExpireTime())) {
            request.setStatus(1);
            request.setEndTime(request.getTokenExpireTime());
            request.setEndReason("应急权限到期");
            request.setReportDeadline(request.getTokenExpireTime().plusHours(REPORT_HOURS));
            emergencyRequestRepository.save(request);
        } else if (status == 1 && request.getReportDeadline() != null
                && now.isAfter(request.getReportDeadline())) {
            request.setStatus(6);
            emergencyRequestRepository.save(request);
        } else if (status == 2 && request.getConfirmDeadline() != null
                && now.isAfter(request.getConfirmDeadline())) {
            request.setStatus(7);
            emergencyRequestRepository.save(request);
        }
    }

    private Map<String, Object> buildRequestItem(EmergencyRequest request) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("requestId", request.getId());
        item.put("hospitalId", request.getHospitalId());
        item.put("doctorId", request.getDoctorId());
        item.put("doctorName", request.getDoctorName());
        item.put("doctorTitle", request.getDoctorTitle());
        item.put("reason", request.getReason());
        item.put("status", request.getStatus());
        item.put("statusText", requestStatusText(request.getStatus()));
        item.put("createTime", request.getCreateTime() == null ? null : request.getCreateTime().toString());
        item.put("tokenExpireTime", request.getTokenExpireTime() == null
                ? null : request.getTokenExpireTime().toString());
        item.put("certVerified", request.getCertVerified());
        item.put("certIdAvailable", request.getCertIdPath() != null);
        item.put("certLicenseAvailable", request.getCertLicensePath() != null);
        item.put("needReport", request.getStatus() != null
                && Arrays.asList(0, 1, 6).contains(request.getStatus()));
        item.put("reportRequired", request.getStatus() != null
                && Arrays.asList(1, 6).contains(request.getStatus()));
        item.put("reportScope", request.getReportScope());
        item.put("reportImpact", request.getReportImpact());
        item.put("reportComplete", request.getReportComplete());
        item.put("reportTime", request.getReportTime() == null ? null : request.getReportTime().toString());
        item.put("reportDeadline", request.getReportDeadline() == null
                ? null : request.getReportDeadline().toString());
        item.put("confirmDeadline", request.getConfirmDeadline() == null
                ? null : request.getConfirmDeadline().toString());
        item.put("confirmByName", request.getConfirmByName());
        item.put("confirmTime", request.getConfirmTime() == null ? null : request.getConfirmTime().toString());
        item.put("confirmRemark", request.getConfirmRemark());
        item.put("endTime", request.getEndTime() == null ? null : request.getEndTime().toString());
        item.put("endReason", request.getEndReason());
        return item;
    }

    private String buildAutoScope(EmergencyRequest request) {
        List<EmergencyOperation> operations =
                emergencyOperationRepository.findByRequestIdOrderByCreateTimeAsc(request.getId());
        if (operations.isEmpty()) {
            return "本次应急期间未记录到科室/排班写操作";
        }
        StringBuilder builder = new StringBuilder();
        for (EmergencyOperation operation : operations) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            String summary = operation.getSummary() == null ? "应急操作" : operation.getSummary();
            builder.append(summary).append("（").append(operation.getPath()).append("）");
        }
        return builder.toString();
    }

    private String requestStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case 0: return "使用中·待核对";
            case 1: return "已结束·待报告";
            case 2: return "待卫健委确认";
            case 3: return "已确认归档";
            case 4: return "已驳回·异常";
            case 5: return "已中止";
            case 6: return "逾期未交报告";
            case 7: return "逾期未确认";
            default: return "未知状态";
        }
    }

    public Map<String, Object> login(String hospitalId, String dailyKey, String ipAddress) {
        Map<String, Object> result = new HashMap<>();

        if (emergencyGuardService.isLocked(hospitalId)) {
            throw new RuntimeException("本院应急权限已被冻结：" + emergencyGuardService.getLockReason(hospitalId));
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("医院不存在"));

        HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("该医院尚未启用应急登录，请先由本院管理员生成应急密钥"));

        String inputKey = dailyKey == null ? "" : dailyKey.trim();
        boolean matchesDaily = hospitalKey.getDailyKey() != null
                && inputKey.equalsIgnoreCase(hospitalKey.getDailyKey());
        boolean matchesRecovery = hospitalKey.getRecoveryKey() != null
                && hospitalKey.getRecoveryStatus() != null && hospitalKey.getRecoveryStatus() == 1
                && inputKey.equalsIgnoreCase(hospitalKey.getRecoveryKey());

        if (!matchesDaily && !matchesRecovery) {
            throw new RuntimeException("密钥错误，请核对本院应急密钥或常设应急码");
        }

        LocalDateTime now = LocalDateTime.now();
        if (matchesDaily) {
            if (hospitalKey.getStatus() == null || hospitalKey.getStatus() != 1) {
                throw new RuntimeException("应急密钥已被停用，请使用常设应急码或联系本院管理员");
            }
            if (now.isAfter(hospitalKey.getExpireTime())) {
                throw new RuntimeException("当日密钥已失效，请本院管理员重新生成，或使用常设应急码");
            }
        }

        String token = generateToken(hospitalId);
        LocalDateTime tokenExpireTime = now.plusHours(2);

        EmergencyLog log = new EmergencyLog(
                "LOG" + System.currentTimeMillis(),
                hospitalId,
                hospital.getHospitalName(),
                ipAddress,
                tokenExpireTime
        );
        log.setToken(token);
        emergencyLogRepository.save(log);

        tokenService.register(token, "ADMIN", log.getLogId(), "EMERGENCY_ADMIN", hospitalId,
                System.currentTimeMillis() + TOKEN_HOURS * 60L * 60 * 1000);

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("hospitalId", hospitalId);
        result.put("hospitalName", hospital.getHospitalName());
        result.put("expireTime", tokenExpireTime.toString());

        return result;
    }

    public String generateToken(String hospitalId) {
        return "EMERGENCY_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public String generateDailyKey(String hospitalId) {
        String dailyKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        HospitalKey existing = hospitalKeyRepository.findById(hospitalId).orElse(null);
        if (existing != null) {
            existing.setDailyKey(dailyKey);
            existing.setExpireTime(LocalDateTime.now().plusDays(1));
            existing.setLastRenewTime(LocalDateTime.now());
            hospitalKeyRepository.save(existing);
        } else {
            HospitalKey hospitalKey = new HospitalKey(
                    hospitalId,
                    dailyKey,
                    LocalDateTime.now().plusDays(1)
            );
            hospitalKeyRepository.save(hospitalKey);
        }

        return dailyKey;
    }

    public void renewDailyKey(String hospitalId) {
        HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("密钥记录不存在"));

        String newKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        hospitalKey.setDailyKey(newKey);
        hospitalKey.setExpireTime(LocalDateTime.now().plusDays(1));
        hospitalKey.setLastRenewTime(LocalDateTime.now());
        hospitalKeyRepository.save(hospitalKey);
    }

    public void invalidateDailyKey(String hospitalId) {
        HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("密钥记录不存在"));
        hospitalKey.setStatus(0);
        hospitalKeyRepository.save(hospitalKey);
    }

    public List<EmergencyLog> getEmergencyLogs() {
        return emergencyLogRepository.findAllByOrderByLoginTimeDesc();
    }

    public List<EmergencyLog> getEmergencyLogsByHospital(String hospitalId) {
        return emergencyLogRepository.findByHospitalId(hospitalId);
    }

    public Map<String, Object> validateToken(String token) {
        Map<String, Object> result = new HashMap<>();

        if (token == null || !token.startsWith("EMERGENCY_")) {
            result.put("valid", false);
            result.put("code", 4013);
            result.put("message", "无效的应急token");
            return result;
        }

        Optional<EmergencyLog> logOptional = emergencyLogRepository.findByToken(token);
        if (logOptional.isEmpty()) {
            result.put("valid", false);
            result.put("code", 4013);
            result.put("message", "应急登录记录不存在，请重新登录");
            return result;
        }

        EmergencyLog log = logOptional.get();
        LocalDateTime expireTime = log.getTokenExpireTime();
        if (log.getStatus() == null || log.getStatus() != 1 || log.getLogoutTime() != null
                || expireTime == null || LocalDateTime.now().isAfter(expireTime)) {
            result.put("valid", false);
            result.put("code", 4013);
            result.put("message", "应急登录已过期，请重新登录");
            return result;
        }

        result.put("valid", true);
        result.put("hospitalId", log.getHospitalId());
        result.put("hospitalName", log.getHospitalName());
        return result;
    }

    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        Optional<EmergencyLog> logOptional = emergencyLogRepository.findByToken(token);
        if (logOptional.isEmpty()) {
            return;
        }
        EmergencyLog log = logOptional.get();
        if (log.getLogoutTime() != null) {
            return;
        }
        log.setLogoutTime(LocalDateTime.now());
        log.setStatus(0);
        emergencyLogRepository.save(log);
        tokenService.revoke(token);
    }
}
