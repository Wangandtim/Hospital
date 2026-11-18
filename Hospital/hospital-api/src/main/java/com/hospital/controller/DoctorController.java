package com.hospital.controller;

import com.hospital.entity.Doctor;
import com.hospital.entity.Department;
import com.hospital.repository.DepartmentRepository;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorService;
import com.hospital.util.HospitalIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuthContext authContext;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String deptId) {
        List<Doctor> doctors;
        if (deptId != null && !deptId.isEmpty()) {
            doctors = authContext.isHospitalAdmin()
                    ? doctorService.findByDeptIdAndHospitalId(deptId, authContext.hospitalId())
                    : doctorService.findByDeptId(deptId);
        } else {
            doctors = authContext.isHospitalAdmin()
                    ? doctorService.findByHospitalId(authContext.hospitalId())
                    : doctorService.findAll();
        }
        return convertList(doctors);
    }

    // 通过医院查询
    @GetMapping("/hospital/{hospitalId}")
    public List<Map<String, Object>> listByHospital(@PathVariable String hospitalId) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        return convertList(doctorService.findByHospitalId(normalizedId));
    }

    /** 批量转换：一次性取出科室名称，避免逐条查询导致的 N+1 慢查询。 */
    private List<Map<String, Object>> convertList(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        Map<String, String> deptNameMap = buildDeptNameMap();
        return doctors.stream()
                .map(doctor -> convertToMapWithDeptName(doctor, deptNameMap))
                .collect(Collectors.toList());
    }

    private Map<String, String> buildDeptNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Department department : departmentRepository.findAll()) {
            map.put(department.getDeptId(), department.getName());
        }
        return map;
    }

    // 通过id查询
    @GetMapping("/{doctorId}")
    public Map<String, Object> getById(@PathVariable String doctorId) {
        Doctor doctor = doctorService.findById(doctorId);
        if (doctor == null) return null;
        authContext.requireHospitalScope(doctor.getHospitalId());
        return convertToMapWithDeptName(doctor);
    }
    
    private Map<String, Object> convertToMapWithDeptName(Doctor doctor) {
        return convertToMapWithDeptName(doctor, null);
    }

    private Map<String, Object> convertToMapWithDeptName(Doctor doctor, Map<String, String> deptNameMap) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", doctor.getDoctorId());
        result.put("doctorId", doctor.getDoctorId());
        result.put("name", doctor.getName());
        result.put("doctorName", doctor.getName());
        result.put("title", doctor.getTitle());
        result.put("hospitalId", doctor.getHospitalId());
        result.put("deptId", doctor.getDeptId());
        
        // 获取科室名称（列表接口传入批量映射，避免逐条查询）
        String deptName;
        if (deptNameMap != null) {
            deptName = doctor.getDeptId() == null || doctor.getDeptId().isEmpty()
                    ? "未填写"
                    : deptNameMap.getOrDefault(doctor.getDeptId(), doctor.getDeptId());
        } else {
            deptName = getDeptName(doctor.getDeptId());
        }
        result.put("department", deptName);
        result.put("deptName", deptName);
        
        result.put("expertise", doctor.getExpertise());
        result.put("introduction", doctor.getExperience());
        result.put("gender", doctor.getGender());
        result.put("consultationFee", doctor.getConsultationFee());
        result.put("status", doctor.getStatus());
        result.put("username", doctor.getUsername());
        result.put("phone", doctor.getPhone());
        
        return result;
    }
    
    private String getDeptName(String deptId) {
        if (deptId == null || deptId.isEmpty()) {
            return "未填写";
        }
        Optional<Department> dept = departmentRepository.findById(deptId);
        return dept.map(Department::getName).orElse(deptId);
    }

    @GetMapping("/dept/{deptId}")
    public List<Map<String, Object>> getByDept(@PathVariable String deptId) {
        if (authContext.isHospitalAdmin()) {
            return convertList(doctorService.findByDeptIdAndHospitalId(deptId, authContext.hospitalId()));
        }
        return convertList(doctorService.findByDeptId(deptId));
    }

    @GetMapping("/dept/{deptId}/hospital/{hospitalId}")
    public List<Map<String, Object>> getByDeptAndHospital(
            @PathVariable String deptId,
            @PathVariable String hospitalId) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        return convertList(doctorService.findByDeptIdAndHospitalId(deptId, normalizedId));
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Doctor doctor) {
        Map<String, Object> result = new HashMap<>();
        try {
            doctor.setHospitalId(authContext.resolveHospitalId(doctor.getHospitalId()));
            Doctor created = doctorService.create(doctor);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}")
    public Map<String, Object> update(@PathVariable String doctorId, @RequestBody Doctor doctor) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor existing = doctorService.findById(doctorId);
            authContext.requireHospitalScope(existing.getHospitalId());
            Doctor updated = doctorService.update(doctorId, doctor);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{doctorId}")
    public Map<String, Object> delete(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor existing = doctorService.findById(doctorId);
            authContext.requireHospitalScope(existing.getHospitalId());
            doctorService.delete(doctorId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}