package com.hospital.service;

import com.hospital.entity.Admin;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Hospital;
import com.hospital.repository.AdminRepository;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 批量导入医生与医院管理员账号。
 * 逐行处理：单行失败不影响其他行，返回失败行号与原因。
 */
@Service
public class BatchImportService {

    private static final int MAX_ROWS = 500;
    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    public Map<String, Object> importDoctors(String hospitalId, List<Map<String, Object>> rows) {
        if (hospitalId == null || hospitalId.isEmpty()) {
            return fail("请先指定要导入的医院");
        }
        if (!hospitalRepository.existsById(hospitalId)) {
            return fail("医院不存在：" + hospitalId);
        }
        if (rows == null || rows.isEmpty()) {
            return fail("没有可导入的数据");
        }
        if (rows.size() > MAX_ROWS) {
            return fail("单次最多导入 " + MAX_ROWS + " 条");
        }

        Map<String, Object> result = baseResult(rows.size());
        List<Map<String, Object>> errors = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                String username = text(row.get("username"));
                String name = text(row.get("name"));
                String deptInput = text(row.get("dept"));
                if (username == null) {
                    throw new RuntimeException("登录账号不能为空");
                }
                if (name == null) {
                    throw new RuntimeException("姓名不能为空");
                }
                if (deptInput == null) {
                    throw new RuntimeException("科室不能为空");
                }
                if (doctorRepository.findByUsername(username).isPresent()) {
                    throw new RuntimeException("登录账号已存在");
                }

                Department department = resolveDepartment(deptInput, hospitalId);
                if (department == null) {
                    throw new RuntimeException("科室不存在：" + deptInput);
                }

                Doctor doctor = new Doctor();
                doctor.setDoctorId("DOC" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
                doctor.setUsername(username);
                doctor.setName(name);
                doctor.setHospitalId(hospitalId);
                doctor.setDeptId(department.getDeptId());
                doctor.setGender(text(row.get("gender")));
                doctor.setTitle(text(row.get("title")) == null ? "医师" : text(row.get("title")));
                doctor.setPhone(text(row.get("phone")));
                doctor.setExpertise(text(row.get("expertise")));
                doctor.setConsultationFee(parseFee(row.get("fee")));
                doctor.setStatus(1);
                doctor.setPassword(text(row.get("password")) == null ? DEFAULT_PASSWORD : text(row.get("password")));

                doctorService.create(doctor);
                successCount++;
            } catch (RuntimeException e) {
                pushError(errors, i, e.getMessage());
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", rows.size() - successCount);
        result.put("errors", errors);
        return result;
    }

    public Map<String, Object> importAdmins(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return fail("没有可导入的数据");
        }
        if (rows.size() > MAX_ROWS) {
            return fail("单次最多导入 " + MAX_ROWS + " 条");
        }

        Map<String, Object> result = baseResult(rows.size());
        List<Map<String, Object>> errors = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                String username = text(row.get("username"));
                String hospitalInput = text(row.get("hospital"));
                if (username == null) {
                    throw new RuntimeException("登录账号不能为空");
                }
                if (hospitalInput == null) {
                    throw new RuntimeException("所属医院不能为空");
                }
                if (adminRepository.findByUsername(username).isPresent()) {
                    throw new RuntimeException("登录账号已存在");
                }

                Hospital hospital = resolveHospital(hospitalInput);
                if (hospital == null) {
                    throw new RuntimeException("医院不存在：" + hospitalInput);
                }

                Admin admin = new Admin();
                admin.setAdminId("ADM" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
                admin.setUsername(username);
                admin.setRealName(text(row.get("name")));
                admin.setPhone(text(row.get("phone")));
                admin.setHospitalId(hospital.getHospitalId());
                admin.setRole("HOSPITAL_ADMIN");
                admin.setStatus(1);
                admin.setPassword(text(row.get("password")) == null ? DEFAULT_PASSWORD : text(row.get("password")));
                admin.setCreateTime(LocalDateTime.now());

                Admin created = adminService.create(admin);
                // 超管批量创建视为已审核通过，可直接登录
                created.setAuditStatus(1);
                created.setAuditTime(LocalDateTime.now());
                created.setAuditComment("批量导入");
                if (created.getCreateTime() == null) {
                    created.setCreateTime(LocalDateTime.now());
                }
                adminRepository.save(created);
                successCount++;
            } catch (RuntimeException e) {
                pushError(errors, i, e.getMessage());
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", rows.size() - successCount);
        result.put("errors", errors);
        return result;
    }

    private Department resolveDepartment(String input, String hospitalId) {
        Department byId = departmentRepository.findById(input).orElse(null);
        if (byId != null && hospitalId.equals(byId.getHospitalId())) {
            return byId;
        }
        List<Department> departments = departmentRepository.findByHospitalId(hospitalId);
        for (Department department : departments) {
            if (department.getName() != null && department.getName().equals(input)) {
                return department;
            }
        }
        for (Department department : departments) {
            if (department.getName() != null && department.getName().contains(input)) {
                return department;
            }
        }
        return null;
    }

    private Hospital resolveHospital(String input) {
        Hospital byId = hospitalRepository.findById(input).orElse(null);
        if (byId != null) {
            return byId;
        }
        List<Hospital> hospitals = hospitalRepository.findAll();
        for (Hospital hospital : hospitals) {
            if (hospital.getHospitalName() != null && hospital.getHospitalName().equals(input)) {
                return hospital;
            }
        }
        return null;
    }

    private int parseFee(Object value) {
        if (value == null || text(value) == null) {
            return 30;
        }
        try {
            int fee = Integer.parseInt(text(value));
            if (fee < 0 || fee > 10000) {
                throw new RuntimeException("挂号费超出范围");
            }
            return fee;
        } catch (NumberFormatException e) {
            throw new RuntimeException("挂号费格式不正确");
        }
    }

    private Map<String, Object> baseResult(int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("total", total);
        result.put("successCount", 0);
        result.put("failCount", 0);
        result.put("errors", new ArrayList<>());
        return result;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private void pushError(List<Map<String, Object>> errors, int index, String message) {
        if (errors.size() >= 50) {
            return;
        }
        Map<String, Object> error = new HashMap<>();
        error.put("row", index + 1);
        error.put("message", message == null ? "导入失败" : message);
        errors.add(error);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
