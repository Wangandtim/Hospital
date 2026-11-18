package com.hospital.controller;

import com.hospital.entity.Doctor;
import com.hospital.repository.DoctorRepository;
import com.hospital.security.PasswordUtil;
import com.hospital.security.RateLimitService;
import com.hospital.security.TokenService;
import com.hospital.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor-self")
@CrossOrigin(origins = "*")
public class DoctorSelfController {

    @Autowired
    private DoctorService doctorService;
    
    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/{doctorId}")
    public Map<String, Object> getDoctorInfo(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            result.put("success", true);
            result.put("data", doctor);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}/profile")
    public Map<String, Object> updateProfile(
            @PathVariable String doctorId,
            @RequestBody Map<String, Object> profile) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);

            if (profile.containsKey("name")) {
                doctor.setName((String) profile.get("name"));
            }
            if (profile.containsKey("gender")) {
                doctor.setGender((String) profile.get("gender"));
            }
            if (profile.containsKey("title")) {
                doctor.setTitle((String) profile.get("title"));
            }
            if (profile.containsKey("expertise")) {
                doctor.setExpertise((String) profile.get("expertise"));
            }
            if (profile.containsKey("phone")) {
                doctor.setPhone((String) profile.get("phone"));
            }
            if (profile.containsKey("education")) {
                doctor.setEducation((String) profile.get("education"));
            }
            if (profile.containsKey("moral")) {
                doctor.setMoral((String) profile.get("moral"));
            }
            if (profile.containsKey("experience")) {
                doctor.setExperience((String) profile.get("experience"));
            }
            if (profile.containsKey("consultationFee")) {
                doctor.setConsultationFee(((Number) profile.get("consultationFee")).intValue());
            }

            Doctor updated = doctorService.update(doctorId, doctor);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}/password")
    public Map<String, Object> changePassword(
            @PathVariable String doctorId,
            @RequestBody Map<String, String> passwordInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);

            String oldPassword = passwordInfo.get("oldPassword");
            String newPassword = passwordInfo.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }

            if (!passwordUtil.matches(oldPassword, doctor.getPassword())) {
                result.put("success", false);
                result.put("message", "原密码错误");
                return result;
            }

            doctor.setPassword(passwordUtil.encode(newPassword));
            doctorService.update(doctorId, doctor);

            result.put("success", true);
            result.put("message", "密码修改成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}/disable")
    public Map<String, Object> disableAccount(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            doctor.setStatus(0);
            doctorService.update(doctorId, doctor);

            result.put("success", true);
            result.put("message", "账户已停用");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}/enable")
    public Map<String, Object> enableAccount(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            doctor.setStatus(1);
            doctorService.update(doctorId, doctor);

            result.put("success", true);
            result.put("message", "账户已启用");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 医生注册接口
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> registerInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证必填字段
            String username = (String) registerInfo.get("username");
            String password = (String) registerInfo.get("password");
            String name = (String) registerInfo.get("name");
            String phone = (String) registerInfo.get("phone");
            String hospitalId = (String) registerInfo.get("hospitalId");

            if (username == null || username.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            if (password == null || password.isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }

            if (name == null || name.isEmpty()) {
                result.put("success", false);
                result.put("message", "姓名不能为空");
                return result;
            }

            if (phone == null || phone.isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }

            if (hospitalId == null || hospitalId.isEmpty()) {
                result.put("success", false);
                result.put("message", "所属医院不能为空");
                return result;
            }

            // 检查用户名是否已存在
            Doctor existing = doctorRepository.findByUsername(username).orElse(null);
            if (existing != null) {
                result.put("success", false);
                result.put("message", "用户名已被注册");
                return result;
            }

            // 创建医生对象
            Doctor doctor = new Doctor();
            doctor.setDoctorId(nextDoctorId());
            doctor.setUsername(username);
            doctor.setPassword(passwordUtil.encode(password));
            doctor.setName(name);
            doctor.setPhone(phone);
            doctor.setHospitalId(hospitalId);
            
            // 设置默认值
            doctor.setGender((String) registerInfo.getOrDefault("gender", "男"));
            doctor.setTitle((String) registerInfo.getOrDefault("title", "医师"));
            doctor.setExpertise((String) registerInfo.get("expertise"));
            doctor.setConsultationFee(((Number) registerInfo.getOrDefault("consultationFee", 50)).intValue());
            doctor.setStatus(1); // 注册后默认启用

            // 保存医生
            Doctor saved = doctorService.create(doctor);

            result.put("success", true);
            result.put("message", "注册成功");
            result.put("data", saved);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生编号统一为 DOC001、DOC002…的连续编号，避免同一医生出现多种 ID 写法。 */
    private synchronized String nextDoctorId() {
        int max = 0;
        for (Doctor item : doctorRepository.findAll()) {
            String id = item.getDoctorId();
            if (id != null && id.matches("DOC\\d{3,}")) {
                max = Math.max(max, Integer.parseInt(id.substring(3)));
            }
        }
        return String.format("DOC%03d", max + 1);
    }

    /**
     * 医生登录接口
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginInfo,
                                     jakarta.servlet.http.HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = loginInfo.get("username");
            String password = loginInfo.get("password");

            String attemptKey = "doctor-self:" + username + "@" + rateLimitService.clientIp(httpRequest);
            if (rateLimitService.isLocked(attemptKey)) {
                result.put("success", false);
                result.put("message", "尝试次数过多，请 15 分钟后再试");
                return result;
            }

            if (username == null || password == null) {
                result.put("success", false);
                result.put("message", "用户名或密码不能为空");
                return result;
            }

            Doctor doctor = doctorRepository.findByUsername(username).orElse(null);
            if (doctor == null) {
                rateLimitService.recordFailure(attemptKey);
                result.put("success", false);
                result.put("message", "医生账号不存在");
                return result;
            }
            if (!passwordUtil.matches(password, doctor.getPassword())) {
                rateLimitService.recordFailure(attemptKey);
                result.put("success", false);
                result.put("message", "密码错误");
                return result;
            }
            if (!passwordUtil.isEncoded(doctor.getPassword())) {
                doctor.setPassword(passwordUtil.encode(password));
                doctorRepository.save(doctor);
            }

            if (doctor.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "账户已被停用");
                return result;
            }

            rateLimitService.recordSuccess(attemptKey);

            result.put("success", true);
            result.put("message", "登录成功");
            result.put("token", tokenService.issue("DOCTOR", doctor.getDoctorId()));
            result.put("doctorId", doctor.getDoctorId());
            result.put("doctorName", doctor.getName());
            result.put("hospitalId", doctor.getHospitalId());
            result.put("data", doctor);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}