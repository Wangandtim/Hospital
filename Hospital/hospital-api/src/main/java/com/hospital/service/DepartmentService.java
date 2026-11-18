package com.hospital.service;

import com.hospital.entity.Department;
import com.hospital.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public List<Department> findByHospitalId(String hospitalId) {
        return departmentRepository.findByHospitalId(hospitalId);
    }

    public Department findById(String deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("科室不存在"));
    }

    public Department create(Department dept) {
        if (dept.getDeptId() == null || dept.getDeptId().isEmpty()) {
            dept.setDeptId("D" + System.currentTimeMillis() % 10000);
        }
        return departmentRepository.save(dept);
    }

    public Department update(String deptId, Department dept) {
        Department existing = findById(deptId);
        existing.setDeptName(dept.getDeptName());
        existing.setDescription(dept.getDescription());
        existing.setServiceTime(dept.getServiceTime());
        if (dept.getDiseases() != null) {
            existing.setDiseases(dept.getDiseases());
        }
        if (dept.getHospitalId() != null) {
            existing.setHospitalId(dept.getHospitalId());
        }
        // 重点专科级别、奖项、案例：仅在前端确实提交时覆盖，避免局部更新把已填内容清空
        if (dept.getFlagshipLevel() != null) {
            existing.setFlagshipLevel(dept.getFlagshipLevel());
            existing.setIsFlagship(dept.getFlagshipLevel() > 0 ? 1 : 0);
        } else if (dept.getIsFlagship() != null) {
            existing.setIsFlagship(dept.getIsFlagship());
        }
        if (dept.getAwards() != null) {
            existing.setAwards(dept.getAwards());
        }
        if (dept.getCases() != null) {
            existing.setCases(dept.getCases());
        }
        return departmentRepository.save(existing);
    }

    public void delete(String deptId) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("科室不存在"));
        dept.setIsDeleted(1);
        dept.setDeleteTime(java.time.LocalDateTime.now());
        dept.setDeletedBy("ADMIN");
        departmentRepository.save(dept);
    }

    public Department findByIdIncludingDeleted(String deptId) {
        return departmentRepository.findIncludingDeleted(deptId)
                .orElseThrow(() -> new RuntimeException("科室不存在"));
    }

    public List<Department> findDeletedByHospitalId(String hospitalId) {
        return departmentRepository.findDeletedByHospitalId(hospitalId);
    }

    public void restore(String deptId) {
        Department dept = departmentRepository.findIncludingDeleted(deptId)
                .orElseThrow(() -> new RuntimeException("科室不存在"));
        if (dept.getIsDeleted() == null || dept.getIsDeleted() == 0) {
            throw new RuntimeException("该科室未被删除");
        }
        departmentRepository.restoreDeleted(deptId);
    }

    public List<Department> search(String keyword) {
        return departmentRepository.searchByKeyword(keyword);
    }

    public List<Department> searchByHospitalId(String keyword, String hospitalId) {
        return departmentRepository.searchByKeywordAndHospital(keyword, hospitalId);
    }

    public List<Department> searchByName(String keyword) {
        return departmentRepository.searchByName(keyword);
    }

    public List<Map<String, Object>> searchWithScore(String keyword) {
        List<Department> departments = search(keyword);
        String lowerKeyword = keyword.toLowerCase();

        return departments.stream()
                .map(d -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("department", d);
                    int score = calculateScore(d, lowerKeyword);
                    result.put("score", score);
                    result.put("type", "department");
                    return result;
                })
                .sorted((a, b) -> (Integer)b.get("score") - (Integer)a.get("score"))
                .collect(Collectors.toList());
    }

    private int calculateScore(Department d, String keyword) {
        int score = 0;
        String name = d.getDeptName().toLowerCase();
        String desc = d.getDescription() != null ? d.getDescription().toLowerCase() : "";

        if (name.equals(keyword)) {
            score += 100;
        } else if (name.startsWith(keyword)) {
            score += 80;
        } else if (name.contains(keyword)) {
            score += 60;
        }

        if (desc.contains(keyword)) {
            score += 30;
        }

        return score;
    }

    public List<Map<String, String>> getSuggestions(String keyword) {
        List<Department> departments = searchByName(keyword);
        return departments.stream()
                .map(d -> {
                    Map<String, String> suggestion = new HashMap<>();
                    suggestion.put("name", d.getDeptName());
                    suggestion.put("type", "department");
                    suggestion.put("id", d.getDeptId());
                    return suggestion;
                })
                .limit(10)
                .collect(Collectors.toList());
    }
}