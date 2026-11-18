package com.hospital.controller;

import com.hospital.entity.Doctor;
import com.hospital.entity.DoctorDutyRule;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorService;
import com.hospital.service.DutyRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出诊规则维护与达标核对。医院管理员只能操作本院医生。
 */
@RestController
@RequestMapping("/api/duty-rules")
@CrossOrigin(origins = "*")
public class DutyRuleController {

    @Autowired
    private DutyRuleService dutyRuleService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AuthContext authContext;

    @GetMapping("/doctor/{doctorId}")
    public List<DoctorDutyRule> listByDoctor(
            @PathVariable String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Doctor doctor = doctorService.findById(doctorId);
        authContext.requireHospitalScope(doctor.getHospitalId());
        return dutyRuleService.findByDoctor(doctorId, from, to);
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<DoctorDutyRule> listByHospital(
            @PathVariable String hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        authContext.requireHospitalScope(hospitalId);
        return dutyRuleService.findByHospital(hospitalId, date);
    }

    @GetMapping("/doctor/{doctorId}/summary")
    public Map<String, Object> weeklySummary(
            @PathVariable String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        Doctor doctor = doctorService.findById(doctorId);
        authContext.requireHospitalScope(doctor.getHospitalId());
        return dutyRuleService.buildWeekSummary(doctorId, weekStart);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody DoctorDutyRule rule) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor doctor = doctorService.findById(rule.getDoctorId());
            authContext.requireHospitalScope(doctor.getHospitalId());
            rule.setHospitalId(doctor.getHospitalId());
            rule.setCreatedBy(authContext.ownerId());
            DoctorDutyRule created = dutyRuleService.create(rule);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{ruleId}")
    public Map<String, Object> update(@PathVariable String ruleId, @RequestBody DoctorDutyRule rule) {
        Map<String, Object> result = new HashMap<>();
        try {
            DoctorDutyRule existing = dutyRuleService.findById(ruleId);
            authContext.requireHospitalScope(existing.getHospitalId());
            DoctorDutyRule updated = dutyRuleService.update(ruleId, rule);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{ruleId}")
    public Map<String, Object> delete(@PathVariable String ruleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DoctorDutyRule existing = dutyRuleService.findById(ruleId);
            authContext.requireHospitalScope(existing.getHospitalId());
            dutyRuleService.delete(ruleId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
