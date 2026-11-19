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

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    @Autowired
    private com.hospital.service.ScheduleService scheduleService;

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

    /** 医生分页（按医院）：职称/年限/擅长/标签/挂号费/今日余号。 */
    @GetMapping("/hospital/{hospitalId}/page")
    public Map<String, Object> pageByHospital(
            @PathVariable String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String keyword) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        List<Doctor> doctors = doctorService.findByHospitalId(normalizedId);
        return pageDoctors(doctors, deptId, keyword, page, size, normalizedId);
    }

    /** 医生分页（按科室）。 */
    @GetMapping("/dept/{deptId}/page")
    public Map<String, Object> pageByDept(
            @PathVariable String deptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        List<Doctor> doctors = doctorService.findByDeptId(deptId);
        String hospitalId = doctors.isEmpty() ? null : doctors.get(0).getHospitalId();
        if (hospitalId != null) {
            authContext.requireHospitalScope(hospitalId);
        }
        return pageDoctors(doctors, null, keyword, page, size, hospitalId);
    }

    private Map<String, Object> pageDoctors(List<Doctor> doctors, String deptId, String keyword,
                                            int page, int size, String hospitalId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, Integer> remain = hospitalId == null
                ? new HashMap<>()
                : catalogService.doctorRemaining(hospitalId, today);
        Map<String, String> deptNames = new HashMap<>();
        for (Department d : departmentRepository.findAll()) {
            deptNames.put(d.getDeptId(), d.getName());
        }
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (Doctor doctor : doctors) {
            if (doctor.getStatus() != null && doctor.getStatus() != 1) {
                continue;
            }
            if (deptId != null && !deptId.isEmpty() && !deptId.equals(doctor.getDeptId())) {
                continue;
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                boolean match = (doctor.getName() != null && doctor.getName().contains(kw))
                        || (doctor.getExpertise() != null && doctor.getExpertise().contains(kw))
                        || (deptNames.get(doctor.getDeptId()) != null && deptNames.get(doctor.getDeptId()).contains(kw));
                if (!match) {
                    continue;
                }
            }
            items.add(catalogService.doctorCard(doctor, deptNames.get(doctor.getDeptId()),
                    remain.getOrDefault(doctor.getDoctorId(), 0), null));
        }
        items.sort(java.util.Comparator
                .comparingInt((Map<String, Object> d) -> titleRank((String) d.get("title")))
                .thenComparing(d -> String.valueOf(d.get("name")))
                .thenComparing(d -> String.valueOf(d.get("doctorId"))));
        int safePage = catalogService.page(page);
        int safeSize = catalogService.size(size);
        long total = items.size();
        return catalogService.envelope(catalogService.slice(items, safePage, safeSize), safePage, safeSize, total);
    }

    private int titleRank(String title) {
        if (title == null) return 9;
        if (title.contains("主任医师") && !title.contains("副主任")) return 0;
        if (title.contains("副主任医师")) return 1;
        if (title.contains("主治")) return 2;
        return 3;
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
        String deptName = getDeptName(doctor.getDeptId());
        Integer remain = catalogService.doctorRemaining(doctor.getHospitalId(), java.time.LocalDate.now())
                .getOrDefault(doctorId, 0);
        List<com.hospital.dto.ScheduleDetailDTO> schedules = scheduleService.findByDoctorAndDateRangeDetail(
                doctorId, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(6));
        Map<String, Object> detail = catalogService.doctorDetail(doctor, deptName, remain, schedules);
        detail.put("status", doctor.getStatus());
        detail.put("username", doctor.getUsername());
        return detail;
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