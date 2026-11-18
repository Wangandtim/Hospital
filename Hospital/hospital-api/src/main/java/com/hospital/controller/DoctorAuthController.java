package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.entity.Doctor;
import com.hospital.entity.Patient;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.security.PasswordUtil;
import com.hospital.security.RateLimitService;
import com.hospital.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = "*")
public class DoctorAuthController {

    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private RateLimitService rateLimitService;

    @PostMapping("/login")
    public Map<String, Object> doctorLogin(@RequestBody Map<String, String> request,
                                           jakarta.servlet.http.HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> result = new HashMap<>();

        String attemptKey = "doctor:" + username + "@" + rateLimitService.clientIp(httpRequest);
        if (rateLimitService.isLocked(attemptKey)) {
            result.put("success", false);
            result.put("message", "尝试次数过多，请 15 分钟后再试");
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

        rateLimitService.recordSuccess(attemptKey);

        // 历史明文密码首次登录后升级为哈希
        if (!passwordUtil.isEncoded(doctor.getPassword())) {
            doctor.setPassword(passwordUtil.encode(password));
            doctorRepository.save(doctor);
        }

        String token = tokenService.issue("DOCTOR", doctor.getDoctorId());

        result.put("success", true);
        result.put("token", token);
        result.put("doctorId", doctor.getDoctorId());
        result.put("doctorName", doctor.getName());
        result.put("title", doctor.getTitle());
        result.put("deptId", doctor.getDeptId());
        result.put("hospitalId", doctor.getHospitalId());

        return result;
    }
    
    @GetMapping("/{doctorId}/appointments")
    public Map<String, Object> getDoctorAppointments(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
            
            // 检查并更新状态
            for (Appointment app : appointments) {
                Integer oldStatus = app.getStatus();
                app.checkAndUpdateStatus();
                if (oldStatus != app.getStatus()) {
                    appointmentRepository.save(app);
                }
            }
            
            List<Map<String, Object>> appointmentList = new ArrayList<>();
            for (Appointment app : appointments) {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("appointmentId", app.getAppointmentId());
                appMap.put("patientId", app.getPatientId());
                appMap.put("appointmentDate", app.getAppointmentDate().toString());
                appMap.put("timeSlot", app.getTimeSlot());
                appMap.put("status", app.getStatus());
                appMap.put("createTime", app.getCreateTime().toString());

                // 检查问卷是否过期（48小时后自动删除）
                boolean questionnaireExpired = false;
                if (app.getQuestionnaireData() != null && app.getCreateTime() != null) {
                    java.time.LocalDateTime expiryTime = app.getCreateTime().plusHours(48);
                    if (java.time.LocalDateTime.now().isAfter(expiryTime)) {
                        questionnaireExpired = true;
                    }
                }

                // 只有未过期的问卷才返回
                if (!questionnaireExpired) {
                    appMap.put("questionnaireData", app.getQuestionnaireData());
                } else {
                    // 清除过期的问卷数据
                    app.setQuestionnaireData(null);
                    appointmentRepository.save(app);
                    appMap.put("questionnaireData", null);
                    appMap.put("questionnaireExpired", true);
                }
                
                Patient patient = patientRepository.findById(app.getPatientId()).orElse(null);
                if (patient != null) {
                    Map<String, Object> patientMap = new HashMap<>();
                    patientMap.put("patientId", patient.getPatientId());
                    patientMap.put("name", patient.getName());
                    patientMap.put("gender", patient.getGender());
                    patientMap.put("age", patient.getAge());
                    patientMap.put("phone", patient.getPhone());
                    patientMap.put("medicalHistory", patient.getMedicalHistory());
                    appMap.put("patient", patientMap);
                }
                
                appointmentList.add(appMap);
            }
            
            result.put("success", true);
            result.put("appointments", appointmentList);
            
            LocalDate today = LocalDate.now();
            List<Appointment> todayAppointments = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, today);
            result.put("todayCount", todayAppointments.size());
            
            long completedCount = appointments.stream().filter(a -> a.getStatus() == 2).count();
            result.put("completedCount", completedCount);
            
            long pendingCount = appointments.stream().filter(a -> a.getStatus() == 1).count();
            result.put("pendingCount", pendingCount);
            
            long inProgressCount = appointments.stream().filter(a -> a.getStatus() == 3).count();
            result.put("inProgressCount", inProgressCount);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取预约数据失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @GetMapping("/{doctorId}/patients")
    public Map<String, Object> getDoctorPatients(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
            
            Set<String> patientIds = new HashSet<>();
            for (Appointment app : appointments) {
                patientIds.add(app.getPatientId());
            }
            
            List<Map<String, Object>> patientList = new ArrayList<>();
            for (String patientId : patientIds) {
                Patient patient = patientRepository.findById(patientId).orElse(null);
                if (patient != null) {
                    Map<String, Object> patientMap = new HashMap<>();
                    patientMap.put("patientId", patient.getPatientId());
                    patientMap.put("name", patient.getName());
                    patientMap.put("gender", patient.getGender());
                    patientMap.put("age", patient.getAge());
                    patientMap.put("phone", patient.getPhone());
                    patientMap.put("medicalHistory", patient.getMedicalHistory());
                    patientList.add(patientMap);
                }
            }
            
            result.put("success", true);
            result.put("patients", patientList);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取患者数据失败: " + e.getMessage());
        }
        
        return result;
    }
}
