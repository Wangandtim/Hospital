package com.hospital.controller;

import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import com.hospital.security.PasswordUtil;
import com.hospital.security.RateLimitService;
import com.hospital.security.TokenService;
import com.hospital.service.PatientService;
import com.hospital.service.SmsCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    @Autowired
    private PatientService patientService;
    
    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SmsCodeService smsCodeService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private com.hospital.security.AuthContext authContext;

    @Autowired
    private com.hospital.service.DemoMockService demoMockService;

    @Autowired
    private com.hospital.service.FamilyMemberService familyMemberService;

    /**
     * 就诊人分页：当前登录患者本人为真实数据，家庭成员暂无真实表，按 demo mock 补齐并标 _mock。
     */
    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(required = false) String patientId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        String role = authContext.role();
        String owner = authContext.ownerId();
        if (role == null) {
            throw new com.hospital.exception.ForbiddenException("请先登录");
        }
        String target = patientId == null || patientId.isEmpty() ? owner : patientId;
        if ("PATIENT".equals(role) && target != null && !target.equals(owner)) {
            throw new com.hospital.exception.ForbiddenException("只能查看本人及家庭成员");
        }
        Patient self = target == null ? null : patientService.findById(target);
        java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (self != null) {
            items.add(patientCard(self, "本人", true, java.util.Collections.emptyList()));
        }
        items.addAll(familyMemberService.members(target));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(safePage * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        java.util.List<Map<String, Object>> data = items.subList(from, to);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", items.size());
        result.put("totalPages", (int) Math.ceil((double) items.size() / safeSize));
        result.put("hasMore", (long) (safePage + 1) * safeSize < items.size());
        result.put("meta", Map.of("mock", demoMockService.isEnabled()));
        return result;
    }

    private Map<String, Object> patientCard(Patient patient, String relation, boolean isDefault,
                                            java.util.List<String> mockFields) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("patientId", patient.getPatientId());
        map.put("name", patient.getName());
        map.put("gender", patient.getGender() == null || "?".equals(patient.getGender()) ? "未知" : patient.getGender());
        map.put("age", patient.getAge());
        map.put("phone", maskPhone(patient.getPhone()));
        map.put("idCard", maskIdCard(patient.getIdCard()));
        map.put("relation", relation);
        map.put("isDefault", isDefault);
        map.put("_mock", mockFields);
        return map;
    }

    /** 家庭成员：新增（患者令牌只能加在本人名下）。 */
    @PostMapping("/family")
    public Map<String, Object> addFamily(@RequestBody Map<String, Object> body,
                                         @RequestParam(required = false) String ownerPatientId) {
        String owner = resolveFamilyOwner(ownerPatientId != null ? ownerPatientId
                : (body.get("ownerPatientId") == null ? null : String.valueOf(body.get("ownerPatientId"))));
        Map<String, Object> member = familyMemberService.add(owner, body);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("data", member);
        return result;
    }

    /** 家庭成员：编辑。 */
    @PutMapping("/family/{memberId}")
    public Map<String, Object> updateFamily(@PathVariable String memberId,
                                            @RequestBody Map<String, Object> body,
                                            @RequestParam(required = false) String ownerPatientId) {
        String owner = resolveFamilyOwner(ownerPatientId);
        Map<String, Object> member = familyMemberService.update(owner, memberId, body);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (member == null) {
            result.put("success", false);
            result.put("message", "家庭成员不存在或不属于当前账号");
            return result;
        }
        result.put("success", true);
        result.put("data", member);
        return result;
    }

    /** 家庭成员：删除。 */
    @DeleteMapping("/family/{memberId}")
    public Map<String, Object> deleteFamily(@PathVariable String memberId,
                                            @RequestParam(required = false) String ownerPatientId) {
        String owner = resolveFamilyOwner(ownerPatientId);
        boolean removed = familyMemberService.delete(owner, memberId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", removed);
        result.put("message", removed ? "已删除" : "家庭成员不存在或不属于当前账号");
        return result;
    }

    private String resolveFamilyOwner(String requestedOwner) {
        String role = authContext.role();
        String owner = authContext.ownerId();
        if (role == null) {
            throw new com.hospital.exception.ForbiddenException("请先登录");
        }
        if ("PATIENT".equals(role)) {
            if (requestedOwner != null && !requestedOwner.isEmpty() && !requestedOwner.equals(owner)) {
                throw new com.hospital.exception.ForbiddenException("只能维护本人名下的家庭成员");
            }
            return owner;
        }
        return requestedOwner != null && !requestedOwner.isEmpty() ? requestedOwner : owner;
    }

    private Map<String, Object> mockFamily(String ownerId, String relation, String name,
                                           String gender, int age, String mockId) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("patientId", mockId);
        map.put("name", name);
        map.put("gender", gender);
        map.put("age", age);
        map.put("phone", "139****6621");
        map.put("idCard", "6101**********1234");
        map.put("relation", relation);
        map.put("isDefault", false);
        map.put("_mock", java.util.List.of("patientId", "name", "gender", "age", "phone", "idCard"));
        map.put("mockOwner", ownerId);
        return map;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /** 开发模式：验证码回显到接口响应，仅用于本地联调。 */
    @Value("${sms.dev-mode:false}")
    private boolean smsDevMode;

    @GetMapping
    public List<Patient> list() {
        return patientService.findAll();
    }

    @GetMapping("/{patientId}")
    public Patient getById(@PathVariable String patientId) {
        return patientService.findById(patientId);
    }

    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        return patientService.create(patient);
    }

    @PutMapping("/{patientId}")
    public Patient update(@PathVariable String patientId, @RequestBody Patient patient) {
        return patientService.update(patientId, patient);
    }

    @DeleteMapping("/{patientId}")
    public void delete(@PathVariable String patientId) {
        patientService.delete(patientId);
    }
    
    // 患者登录接口 - 通过手机号和密码登录
    @PostMapping("/login")
    public Map<String, Object> patientLogin(@RequestBody Map<String, String> request,
                                            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String phone = request.get("phone");
        String password = request.get("password");
        
        Map<String, Object> result = new HashMap<>();

        String attemptKey = "patient:" + phone + "@" + rateLimitService.clientIp(httpRequest);
        if (rateLimitService.isLocked(attemptKey)) {
            result.put("success", false);
            result.put("message", "尝试次数过多，请 15 分钟后再试");
            return result;
        }
        
        if (phone == null || phone.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入手机号");
            return result;
        }
        
        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入密码");
            return result;
        }
        
        // 尝试查找患者
        Patient patient = patientRepository.findByPhone(phone).orElse(null);
        
        if (patient == null) {
            rateLimitService.recordFailure(attemptKey);
            result.put("success", false);
            result.put("message", "该手机号未注册");
            return result;
        }
        
        // 验证密码（兼容历史明文，成功后自动升级为哈希）
        if (!passwordUtil.matches(password, patient.getPassword())) {
            rateLimitService.recordFailure(attemptKey);
            result.put("success", false);
            result.put("message", "密码错误");
            return result;
        }
        rateLimitService.recordSuccess(attemptKey);
        if (!passwordUtil.isEncoded(patient.getPassword())) {
            patient.setPassword(passwordUtil.encode(password));
            patientRepository.save(patient);
        }
        
        result.put("success", true);
        result.put("token", tokenService.issue("PATIENT", patient.getPatientId()));
        result.put("patientId", patient.getPatientId());
        result.put("patientName", patient.getName());
        result.put("phone", patient.getPhone());
        result.put("gender", patient.getGender());
        result.put("age", patient.getAge());
        result.put("medicalHistory", patient.getMedicalHistory());
        result.put("hasCompleteInfo", patient.getName() != null && patient.getGender() != null && patient.getAge() != null && patient.getAge() > 0);
        
        return result;
    }

    /** 发送登录验证码。开发模式会把验证码一并返回，便于本地联调。 */
    @PostMapping("/send-code")
    public Map<String, Object> sendLoginCode(@RequestBody Map<String, String> request,
                                             jakarta.servlet.http.HttpServletRequest httpRequest) {
        String phone = request.get("phone");
        Map<String, Object> result = new HashMap<>();

        if (!rateLimitService.tryAcquire("sms-ip:" + rateLimitService.clientIp(httpRequest), 10, 3600_000L)) {
            result.put("success", false);
            result.put("message", "当前网络请求验证码过于频繁，请稍后再试");
            return result;
        }

        String error = smsCodeService.sendCode(phone);
        if (error != null) {
            result.put("success", false);
            result.put("message", error);
            return result;
        }

        result.put("success", true);
        result.put("message", "验证码已发送");
        if (smsDevMode) {
            result.put("devCode", smsCodeService.peekCode(phone));
            result.put("devHint", "开发模式：验证码同时写入服务端日志");
        }
        return result;
    }

    /** 手机号 + 验证码登录。 */
    @PostMapping("/login-by-code")
    public Map<String, Object> patientLoginByCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        Map<String, Object> result = new HashMap<>();

        if (phone == null || phone.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入手机号");
            return result;
        }
        if (code == null || code.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入验证码");
            return result;
        }

        if (!smsCodeService.verify(phone, code)) {
            result.put("success", false);
            result.put("message", "验证码错误或已失效，请重新获取");
            return result;
        }

        Patient patient = patientRepository.findByPhone(phone).orElse(null);
        if (patient == null) {
            result.put("success", false);
            result.put("message", "该手机号未注册，请先完成患者注册");
            return result;
        }

        return buildLoginResult(patient);
    }

    private Map<String, Object> buildLoginResult(Patient patient) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("token", tokenService.issue("PATIENT", patient.getPatientId()));
        result.put("patientId", patient.getPatientId());
        result.put("patientName", patient.getName());
        result.put("phone", patient.getPhone());
        result.put("gender", patient.getGender());
        result.put("age", patient.getAge());
        result.put("medicalHistory", patient.getMedicalHistory());
        result.put("hasCompleteInfo", patient.getName() != null && patient.getGender() != null
                && patient.getAge() != null && patient.getAge() > 0);
        return result;
    }
    
    // 通过手机号查找患者
    @GetMapping("/by-phone/{phone}")
    public Patient getByPhone(@PathVariable String phone) {
        return patientRepository.findByPhone(phone).orElse(null);
    }
    
    // 患者注册接口 - 将注册信息保存到患者档案
    @PostMapping("/register")
    public Map<String, Object> patientRegister(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String password = (String) request.get("password");
        String name = (String) request.get("name");
        String gender = (String) request.get("gender");
        Object ageObj = request.get("age");
        String idCard = (String) request.get("idCard");
        String medicalHistory = (String) request.get("medicalHistory");
        
        Map<String, Object> result = new HashMap<>();
        
        if (phone == null || phone.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入手机号");
            return result;
        }
        
        if (name == null || name.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入姓名");
            return result;
        }
        
        if (gender == null || gender.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择性别");
            return result;
        }
        
        // 检查手机号是否已注册
        if (patientRepository.findByPhone(phone).isPresent()) {
            result.put("success", false);
            result.put("message", "该手机号已注册");
            return result;
        }
        
        // 检查密码
        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入密码");
            return result;
        }
        
        // 创建患者档案，所有注册信息自动填入
        Patient patient = new Patient();
        String patientId = "P" + phone.substring(phone.length() - 6);
        patient.setPatientId(patientId);
        patient.setName(name);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setPassword(password);
        
        // 处理年龄
        if (ageObj != null) {
            try {
                patient.setAge(Integer.parseInt(ageObj.toString()));
            } catch (Exception e) {
                patient.setAge(0);
            }
        } else {
            patient.setAge(0);
        }
        
        patient.setIdCard(idCard);
        patient.setMedicalHistory(medicalHistory);
        patient.setCreateTime(java.time.LocalDateTime.now());
        
        patient = patientRepository.save(patient);
        
        result.put("success", true);
        result.put("message", "注册成功");
        result.put("patientId", patient.getPatientId());
        result.put("patientName", patient.getName());
        result.put("phone", patient.getPhone());
        result.put("gender", patient.getGender());
        result.put("age", patient.getAge());
        result.put("idCard", patient.getIdCard());
        result.put("medicalHistory", patient.getMedicalHistory());
        
        return result;
    }
}