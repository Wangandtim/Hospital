package com.hospital.controller;

import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.repository.DepartmentRepository;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorService;
import com.hospital.service.DutyRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医院管理员「在岗达标」视图：只出事实，不做扣分或评级。
 */
@RestController
@RequestMapping("/api/admin/doctors")
@CrossOrigin(origins = "*")
public class DutyAdminController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DutyRuleService dutyRuleService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuthContext authContext;

    /**
     * 医生本周在岗达标概览。
     * 院管固定为本院；卫健委可传 hospitalId，不传则查看全部。
     */
    @GetMapping("/duty-overview")
    public Map<String, Object> dutyOverview(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        Map<String, Object> result = new HashMap<>();
        try {
            String scopedHospitalId = authContext.resolveHospitalId(hospitalId);
            List<Doctor> doctors = (scopedHospitalId != null && !scopedHospitalId.isEmpty())
                    ? doctorService.findByHospitalId(scopedHospitalId)
                    : doctorService.findAll();
            if (deptId != null && !deptId.isEmpty()) {
                doctors = doctors.stream()
                        .filter(d -> deptId.equals(d.getDeptId()))
                        .toList();
            }

            LocalDate start = weekStart != null ? weekStart : dutyRuleService.defaultWeekStart();
            List<Map<String, Object>> data = new ArrayList<>();
            for (Doctor doctor : doctors) {
                Map<String, Object> item = baseDoctorView(doctor);
                Map<String, Object> today = dutyRuleService.todayStatus(doctor.getDoctorId());
                Map<String, Object> summary = dutyRuleService.buildWeekSummary(doctor.getDoctorId(), start);
                @SuppressWarnings("unchecked")
                Map<String, Object> week = (Map<String, Object>) summary.get("week");
                item.put("today", today);
                item.put("week", week);
                item.put("weekStart", summary.get("weekStart"));
                item.put("weekEnd", summary.get("weekEnd"));
                data.add(item);
            }

            result.put("success", true);
            result.put("data", data);
            result.put("weekStart", start.toString());
            result.put("weekEnd", start.plusDays(6).toString());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 医生信息卡：基础信息 + 本周值班数据。
     */
    @GetMapping("/{doctorId}/duty-card")
    public Map<String, Object> dutyCard(
            @PathVariable String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());

            Map<String, Object> card = baseDoctorView(doctor);
            card.putAll(dutyRuleService.buildWeekSummary(doctorId, weekStart));
            card.put("today", dutyRuleService.todayStatus(doctorId));

            result.put("success", true);
            result.put("data", card);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> baseDoctorView(Doctor doctor) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("doctorId", doctor.getDoctorId());
        view.put("name", doctor.getName());
        view.put("title", doctor.getTitle());
        view.put("gender", doctor.getGender());
        view.put("phone", doctor.getPhone());
        view.put("hospitalId", doctor.getHospitalId());
        view.put("deptId", doctor.getDeptId());
        view.put("deptName", deptName(doctor.getDeptId()));
        view.put("expertise", doctor.getExpertise());
        view.put("education", doctor.getEducation());
        view.put("experience", doctor.getExperience());
        view.put("consultationFee", doctor.getConsultationFee());
        view.put("status", doctor.getStatus());
        view.put("username", doctor.getUsername());
        return view;
    }

    private String deptName(String deptId) {
        if (deptId == null || deptId.isEmpty()) {
            return "未填写";
        }
        return departmentRepository.findById(deptId)
                .map(Department::getName)
                .orElse(deptId);
    }
}
