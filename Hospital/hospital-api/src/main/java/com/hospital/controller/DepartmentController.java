package com.hospital.controller;

import com.hospital.entity.Department;
import com.hospital.security.AuthContext;
import com.hospital.service.DepartmentService;
import com.hospital.util.HospitalIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private com.hospital.config.ScheduleRules scheduleRules;

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    @Autowired
    private com.hospital.repository.DoctorRepository doctorRepository;

    @GetMapping
    public List<Department> list(@RequestParam(required = false, defaultValue = "false") boolean bookable) {
        List<Department> departments;
        if (authContext.isHospitalAdmin()) {
            departments = departmentService.findByHospitalId(authContext.hospitalId());
        } else {
            departments = departmentService.findAll();
        }
        return filterBookable(departments, bookable);
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<Department> listByHospital(
            @PathVariable String hospitalId,
            @RequestParam(required = false, defaultValue = "false") boolean bookable) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        return filterBookable(departmentService.findByHospitalId(normalizedId), bookable);
    }

    /** 可预约科室列表排除急诊科：急诊只提供医院名称/地址/电话，不开放挂号。 */
    private List<Department> filterBookable(List<Department> departments, boolean bookable) {
        if (!bookable || departments == null) {
            return departments;
        }
        return departments.stream()
                .filter(d -> !scheduleRules.isEmergencyDeptName(d.getDeptName()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 科室分页：带分组、今日出诊数、余号、停诊状态。 */
    @GetMapping("/hospital/{hospitalId}/page")
    public Map<String, Object> pageByHospital(
            @PathVariable String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "false") boolean bookable,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String keyword) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, int[]> stats = catalogService.deptStats(normalizedId, today);
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        List<Department> departments = keyword == null || keyword.trim().isEmpty()
                ? departmentService.findByHospitalId(normalizedId)
                : departmentService.searchByHospitalId(keyword.trim(), normalizedId);
        for (Department dept : filterBookable(departments, bookable)) {
            Map<String, Object> card = catalogService.deptCard(dept, stats.get(dept.getDeptId()));
            if (group == null || group.isEmpty() || group.equals(card.get("group"))) {
                items.add(card);
            }
        }
        items.sort(java.util.Comparator
                .comparingInt((Map<String, Object> d) -> groupOrder((String) d.get("group")))
                .thenComparing(d -> String.valueOf(d.get("name")))
                .thenComparing(d -> String.valueOf(d.get("deptId"))));
        int safePage = catalogService.page(page);
        int safeSize = catalogService.size(size);
        long total = items.size();
        return catalogService.envelope(catalogService.slice(items, safePage, safeSize), safePage, safeSize, total);
    }

    private int groupOrder(String group) {
        return switch (group == null ? "" : group) {
            case "内科" -> 0;
            case "外科" -> 1;
            case "专科" -> 2;
            case "医技" -> 3;
            default -> 9;
        };
    }

    @GetMapping("/{deptId}")
    public Map<String, Object> getById(@PathVariable String deptId) {
        Department department = departmentService.findById(deptId);
        authContext.requireHospitalScope(department.getHospitalId());
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, int[]> stats = catalogService.deptStats(department.getHospitalId(), today);
        Map<String, Integer> remain = catalogService.doctorRemaining(department.getHospitalId(), today);
        List<com.hospital.entity.Doctor> doctors = doctorRepository.findByDeptId(deptId).stream()
                .filter(d -> d.getStatus() == null || d.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList());
        doctors.sort(java.util.Comparator
                .comparingInt((com.hospital.entity.Doctor d) -> titleOrder(d.getTitle()))
                .thenComparing(com.hospital.entity.Doctor::getName)
                .thenComparing(com.hospital.entity.Doctor::getDoctorId));
        Map<String, String> deptNames = new java.util.HashMap<>();
        deptNames.put(deptId, department.getName());
        Map<String, Object> detail = catalogService.deptDetail(department, stats, remain, doctors, deptNames);
        detail.put("serviceTime", department.getServiceTime());
        detail.put("awards", department.getAwards());
        detail.put("cases", department.getCases());
        detail.put("createTime", department.getCreateTime());
        detail.put("isDeleted", department.getIsDeleted());
        detail.put("deleteTime", department.getDeleteTime());
        detail.put("deletedBy", department.getDeletedBy());
        return detail;
    }

    private int titleOrder(String title) {
        if (title == null) return 9;
        if (title.contains("主任医师") && !title.contains("副主任")) return 0;
        if (title.contains("副主任医师")) return 1;
        if (title.contains("主治")) return 2;
        return 3;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Department department) {
        Map<String, Object> result = new HashMap<>();
        try {
            department.setHospitalId(authContext.resolveHospitalId(department.getHospitalId()));
            Department created = departmentService.create(department);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{deptId}")
    public Map<String, Object> update(@PathVariable String deptId, @RequestBody Department department) {
        Map<String, Object> result = new HashMap<>();
        try {
            Department existing = departmentService.findById(deptId);
            authContext.requireHospitalScope(existing.getHospitalId());
            if (department.getHospitalId() != null) {
                department.setHospitalId(authContext.resolveHospitalId(department.getHospitalId()));
            }
            Department updated = departmentService.update(deptId, department);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{deptId}")
    public Map<String, Object> delete(@PathVariable String deptId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Department existing = departmentService.findById(deptId);
            authContext.requireHospitalScope(existing.getHospitalId());
            departmentService.delete(deptId);
            result.put("success", true);
            result.put("message", "科室已移入回收站，可由管理员恢复");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 回收站：已被软删除的科室（院管/超管）。 */
    @GetMapping("/deleted/{hospitalId}")
    public Map<String, Object> deletedList(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            authContext.requireHospitalScope(hospitalId);
            result.put("success", true);
            result.put("data", departmentService.findDeletedByHospitalId(hospitalId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 从回收站恢复科室。 */
    @PostMapping("/{deptId}/restore")
    public Map<String, Object> restore(@PathVariable String deptId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Department existing = departmentService.findByIdIncludingDeleted(deptId);
            authContext.requireHospitalScope(existing.getHospitalId());
            departmentService.restore(deptId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}