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

    @GetMapping("/{deptId}")
    public Department getById(@PathVariable String deptId) {
        Department department = departmentService.findById(deptId);
        authContext.requireHospitalScope(department.getHospitalId());
        return department;
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