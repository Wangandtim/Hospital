package com.hospital.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 接口鉴权拦截器。
 * 规则：先校验令牌，再按「路径 + HTTP 方法」判定所需角色；
 * 管理员再细分卫健委总管理员（SUPER_ADMIN）与医院管理员（HOSPITAL_ADMIN），
 * 院管的数据范围由其令牌中的 hospitalId 决定，业务层通过 AuthContext 做二次校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Auth-Token";
    public static final String ATTR_ROLE = "auth.role";
    public static final String ATTR_OWNER_ID = "auth.ownerId";
    public static final String ATTR_ADMIN_ROLE = "auth.adminRole";
    public static final String ATTR_HOSPITAL_ID = "auth.hospitalId";

    private static final Set<String> WRITE_METHODS = new HashSet<>(Arrays.asList("POST", "PUT", "DELETE", "PATCH"));

    private static final Set<String> ADMIN_ONLY = new HashSet<>(Arrays.asList("ADMIN"));
    private static final Set<String> DOCTOR_ONLY = new HashSet<>(Arrays.asList("DOCTOR"));
    private static final Set<String> ADMIN_OR_DOCTOR = new HashSet<>(Arrays.asList("ADMIN", "DOCTOR"));
    private static final Set<String> ADMIN_OR_PATIENT = new HashSet<>(Arrays.asList("ADMIN", "PATIENT"));
    private static final Set<String> ANY_ROLE = new HashSet<>(Arrays.asList("ADMIN", "DOCTOR", "PATIENT"));
    private static final Set<String> POST_ONLY = new HashSet<>(Arrays.asList("POST"));
    private static final Set<String> GET_ONLY = new HashSet<>(Arrays.asList("GET"));

    /** 受保护规则：路径前缀 + 生效方法（null 表示全部方法）+ 允许角色 + 是否仅限超管。 */
    private static final List<Rule> RULES = Arrays.asList(
            // 卫健委专属：医院准入审批、管理员审批列表、医院分页管理
            new Rule("/api/admin/hospitals/", null, ADMIN_ONLY, false, true),
            new Rule("/api/admin/audit/", null, ADMIN_ONLY, false, true),
            // 管理员的增改删只允许卫健委总管理员，避免院管跨院操作其他管理员账号
            new Rule("/api/admin/", WRITE_METHODS, ADMIN_ONLY, false, true),
            new Rule("/api/admin/", null, ADMIN_ONLY, false),
            new Rule("/api/keys/all", null, ADMIN_ONLY, false, true),
            new Rule("/api/keys/hospital/", null, ADMIN_ONLY, false),
            new Rule("/api/data/", null, ADMIN_ONLY, false),
            // 批量导入：院管导入本院医生，超管导入医院管理员（角色在业务层二次校验）
            new Rule("/api/batch-import/", WRITE_METHODS, ADMIN_ONLY, false),
            new Rule("/api/payment/", null, ADMIN_OR_PATIENT, false),
            // 分诊 Agent：患者本人令牌归属即就诊人；管理员可只读查看会话详情
            new Rule("/api/triage/", null, ADMIN_OR_PATIENT, false),
            // 语音转写：患者录音上传，走可插拔 ASR 网关
            new Rule("/api/voice/", null, ADMIN_OR_PATIENT, false),
            new Rule("/api/appointments/admin/", null, ADMIN_ONLY, false),
            // 医生排班维度的预约明细仅医生/管理员可见
            new Rule("/api/appointments/schedule/", null, ADMIN_OR_DOCTOR, false),
            new Rule("/api/appointments/doctor/", null, ADMIN_OR_DOCTOR, false),
            // 预约读写均需登录；患者只能看到自己的预约（归属在业务层二次校验）
            new Rule("/api/appointments/", null, ANY_ROLE, false),
            // 服务反馈是面向患者的服务通道：允许未登录/登录态失效时提交，
            // 有令牌时仍优先使用令牌归属的患者身份（见 FeedbackController）。
            // 医院准入由卫健委统一管理，院管不能新增/修改医院
            new Rule("/api/hospitals/", WRITE_METHODS, ADMIN_ONLY, false, true),
            // 医院首页内容：读接口公开，写接口仅管理员（院管范围在业务层校验）
            new Rule("/api/hospital-articles/manage", null, ADMIN_ONLY, false),
            new Rule("/api/hospital-articles/", WRITE_METHODS, ADMIN_ONLY, false),
            new Rule("/api/upload/", WRITE_METHODS, ADMIN_ONLY, false),
            // 软删除回收站：仅管理员可查看与恢复
            new Rule("/api/departments/deleted/", null, ADMIN_ONLY, false),
            new Rule("/api/schedules/deleted/", null, ADMIN_ONLY, false),
            new Rule("/api/departments/", WRITE_METHODS, ADMIN_ONLY, false),
            new Rule("/api/doctors/", WRITE_METHODS, ADMIN_ONLY, false),
            new Rule("/api/duty-rules/", null, ADMIN_ONLY, false),
            new Rule("/api/schedules/", WRITE_METHODS, ADMIN_OR_DOCTOR, false),
            new Rule("/api/doctor-schedule-settings/", WRITE_METHODS, ADMIN_OR_DOCTOR, false),
            // 下周排班确认：医生只能查看/确认自己的排班
            new Rule("/api/schedule-weekly-confirm/", null, ADMIN_OR_DOCTOR, false),
            new Rule("/api/doctor-self/", WRITE_METHODS, ADMIN_OR_DOCTOR, true),
            new Rule("/api/patients/", WRITE_METHODS, ADMIN_OR_PATIENT, true),
            // 应急密钥管理：仅卫健委总管理员可生成/续期/作废；日志仅管理员可查
            // 应急通道：医生只能提交/查询/上传证件/交报告；院管只做知会、核对与终止；
            // 卫健委负责事后确认/驳回；证件查看受控并留痕。
            new Rule("/api/emergency/request/submit", POST_ONLY, DOCTOR_ONLY, false),
            new Rule("/api/emergency/request/my", GET_ONLY, DOCTOR_ONLY, false),
            new Rule("/api/emergency/request/cert", POST_ONLY, DOCTOR_ONLY, false),
            new Rule("/api/emergency/request/report-draft", GET_ONLY, DOCTOR_ONLY, false),
            new Rule("/api/emergency/request/report", POST_ONLY, DOCTOR_ONLY, false),
            new Rule("/api/emergency/supervisor/", null, ADMIN_ONLY, false, true),
            new Rule("/api/emergency/cert/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/requests/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/request/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/status/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/snapshots/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/revoke-all/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/unfreeze/", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/generate-key/", null, ADMIN_ONLY, false, true),
            new Rule("/api/emergency/renew-key/", null, ADMIN_ONLY, false, true),
            new Rule("/api/emergency/invalidate-key/", null, ADMIN_ONLY, false, true),
            new Rule("/api/emergency/logs", null, ADMIN_ONLY, false),
            new Rule("/api/emergency/logout", null, ADMIN_ONLY, false)
    );

    /** 公开接口：登录、注册、发送验证码、医院准入申请。 */
    private static final Set<String> ANONYMOUS_PATHS = new HashSet<>(Arrays.asList(
            "/api/admin/login",
            "/api/admin/create",
            "/api/doctor/login",
            "/api/doctor-self/login",
            "/api/doctor-self/register",
            "/api/patients/login",
            "/api/patients/login-by-code",
            "/api/patients/send-code",
            "/api/patients/register",
            "/api/hospitals/apply"
    ));

    @Autowired
    private TokenService tokenService;

    @Autowired
    private com.hospital.service.EmergencyGuardService emergencyGuardService;

    @Autowired
    private com.hospital.service.EmergencySnapshotService emergencySnapshotService;

    @Autowired
    private com.hospital.service.EmergencyOperationService emergencyOperationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // 有令牌先解析，即便接口本身匿名也要把身份透传给 Controller
        TokenService.Session session = tokenService.verify(resolveToken(request));
        if (session != null) {
            request.setAttribute(ATTR_ROLE, session.getRole());
            request.setAttribute(ATTR_OWNER_ID, session.getOwnerId());
            request.setAttribute(ATTR_ADMIN_ROLE, session.getAdminRole());
            request.setAttribute(ATTR_HOSPITAL_ID, session.getHospitalId());
        }

        Rule rule = matchRule(path, request.getMethod());
        if (rule == null) {
            return true;
        }

        if (session == null) {
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效");
        }

        if (!rule.roles.contains(session.getRole())) {
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "当前账号无权访问该接口");
        }

        if (rule.superOnly && !AuthContext.SUPER_ADMIN.equals(session.getAdminRole())) {
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "该操作仅限卫健委总管理员");
        }

        if (rule.ownerScoped && !"ADMIN".equals(session.getRole()) && !pathIdMatches(path, rule.prefix, session)) {
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "无权操作其他账号的数据");
        }

        // 应急账号只允许操作科室与本院基础信息，避免越权改密钥、医生、文章等
        if ("ADMIN".equals(session.getRole()) && AuthContext.EMERGENCY_ADMIN.equals(session.getAdminRole())) {
            if (!isEmergencyAllowedPath(path)) {
                return reject(response, HttpServletResponse.SC_FORBIDDEN, "应急账号仅可进行科室相关操作");
            }

            // 系统自保护：应急写操作计数 + 删除直接拦截并冻结
            String method = request.getMethod();
            boolean isWrite = !"GET".equalsIgnoreCase(method);
            if (isWrite && (path.startsWith("/api/departments") || path.startsWith("/api/schedules"))) {
                try {
                    String emergencyToken = resolveToken(request);
                    emergencyGuardService.assertWriteAllowed(emergencyToken, session.getHospitalId(), method);
                    emergencySnapshotService.capture(emergencyToken, session.getHospitalId(), method, path);
                    emergencyOperationService.record(emergencyToken, session.getHospitalId(), method, path);
                } catch (com.hospital.exception.ForbiddenException e) {
                    return reject(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                }
            }
        }

        return true;
    }

    private boolean isEmergencyAllowedPath(String path) {
        return path.startsWith("/api/departments")
                || path.startsWith("/api/hospitals/")
                || path.startsWith("/api/schedules")
                || path.startsWith("/api/emergency/");
    }

    private Rule matchRule(String path, String method) {
        if (ANONYMOUS_PATHS.contains(path)) {
            return null;
        }
        for (Rule rule : RULES) {
            if (!matchesPrefix(path, rule.prefix)) {
                continue;
            }
            if (rule.methods != null && !rule.methods.contains(method.toUpperCase())) {
                continue;
            }
            return rule;
        }
        return null;
    }

    /** 兼容「/api/appointments」这类不带尾斜杠的控制器根路径。 */
    private boolean matchesPrefix(String path, String prefix) {
        if (path.startsWith(prefix)) {
            return true;
        }
        return path.equals(prefix.substring(0, prefix.length() - 1));
    }

    /** 路径形如 /api/patients/{id}/... 或 /api/doctor-self/{id}/...，比较第二段与令牌归属。 */
    private boolean pathIdMatches(String path, String prefix, TokenService.Session session) {
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        return id.equals(session.getOwnerId());
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\",\"code\":" + status + "}");
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(HEADER_NAME);
        if (token != null && !token.isEmpty()) {
            return token.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private static final class Rule {
        private final String prefix;
        private final Set<String> methods;
        private final Set<String> roles;
        private final boolean ownerScoped;
        private final boolean superOnly;

        private Rule(String prefix, Set<String> methods, Set<String> roles, boolean ownerScoped) {
            this(prefix, methods, roles, ownerScoped, false);
        }

        private Rule(String prefix, Set<String> methods, Set<String> roles, boolean ownerScoped, boolean superOnly) {
            this.prefix = prefix;
            this.methods = methods;
            this.roles = roles;
            this.ownerScoped = ownerScoped;
            this.superOnly = superOnly;
        }
    }
}
