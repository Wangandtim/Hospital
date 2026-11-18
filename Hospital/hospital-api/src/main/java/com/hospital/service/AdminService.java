package com.hospital.service;

import com.hospital.entity.Admin;
import com.hospital.entity.Hospital;
import com.hospital.repository.AdminRepository;
import com.hospital.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Admin login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("管理员账号不存在"));

        String storedPassword = admin.getPassword();
        boolean passwordMatch = false;
        
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            passwordMatch = passwordEncoder.matches(password, storedPassword);
        } else {
            passwordMatch = password.equals(storedPassword);
        }
        
        if (!passwordMatch) {
            throw new RuntimeException("密码错误");
        }

        // 历史明文密码在首次登录成功后自动升级为哈希
        if (storedPassword != null && !storedPassword.startsWith("$2a$")) {
            admin.setPassword(passwordEncoder.encode(password));
            adminRepository.save(admin);
        }

        if (admin.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        if (admin.getRole() == null || admin.getRole().isEmpty()) {
            if ("admin".equals(username)) {
                admin.setRole("SUPER_ADMIN");
                admin.setAuditStatus(1);
                adminRepository.save(admin);
            } else {
                admin.setRole("HOSPITAL_ADMIN");
                admin.setAuditStatus(1);
                adminRepository.save(admin);
            }
        }

        if (!"SUPER_ADMIN".equals(admin.getRole())) {
            if (admin.getAuditStatus() == null || admin.getAuditStatus() != 1) {
                throw new RuntimeException("账号审核未通过，请等待审核");
            }
        }

        if ("SUPER_ADMIN".equals(admin.getRole())) {
            return admin;
        }

        if (admin.getHospitalId() == null || admin.getHospitalId().isEmpty()) {
            throw new RuntimeException("管理员未绑定医院，无权登录");
        }

        Hospital hospital = hospitalRepository.findById(admin.getHospitalId()).orElse(null);
        if (hospital == null) {
            throw new RuntimeException("管理员绑定的医院不存在");
        }

        if (hospital.getStatus() != 1) {
            throw new RuntimeException("管理员所属医院已停用");
        }

        return admin;
    }

    public Admin findById(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
    }

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public Admin create(Admin admin) {
        if (adminRepository.findByUsername(admin.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        String role = admin.getRole();
        if (role == null || role.isEmpty()) {
            role = "HOSPITAL_ADMIN";
            admin.setRole(role);
        }

        if (!"SUPER_ADMIN".equals(role)) {
            if (admin.getHospitalId() == null || admin.getHospitalId().isEmpty()) {
                throw new RuntimeException("非超级管理员必须绑定医院");
            }

            if (!hospitalRepository.existsById(admin.getHospitalId())) {
                throw new RuntimeException("绑定的医院不存在");
            }
        }

        if (admin.getAdminId() == null) {
            admin.setAdminId("ADMIN" + System.currentTimeMillis() % 100000);
        }

        if (!"SUPER_ADMIN".equals(role)) {
            admin.setAuditStatus(0);
        } else {
            admin.setAuditStatus(1);
        }

        if (admin.getPassword() != null && !admin.getPassword().isEmpty()
                && !admin.getPassword().startsWith("$2a$")) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        return adminRepository.save(admin);
    }

    public Admin audit(String adminId, Integer auditStatus, String comment) {
        Admin admin = findById(adminId);
        admin.setAuditStatus(auditStatus);
        admin.setAuditTime(LocalDateTime.now());
        admin.setAuditComment(comment);
        return adminRepository.save(admin);
    }

    public List<Admin> findByAuditStatus(Integer auditStatus) {
        return adminRepository.findAll().stream()
                .filter(a -> auditStatus.equals(a.getAuditStatus()))
                .toList();
    }

    public String generateApiKey(String adminId) {
        Admin admin = findById(adminId);
        
        String apiKey = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        admin.setApiKey(apiKey);
        admin.setApiKeyExpire(LocalDateTime.now().plusDays(30));
        
        adminRepository.save(admin);
        return apiKey;
    }

    public void renewApiKey(String adminId) {
        Admin admin = findById(adminId);
        admin.setApiKeyExpire(LocalDateTime.now().plusDays(30));
        adminRepository.save(admin);
    }

    public void invalidateApiKey(String adminId) {
        Admin admin = findById(adminId);
        admin.setApiKey(null);
        admin.setApiKeyExpire(null);
        adminRepository.save(admin);
    }

    public List<Admin> findByHospitalId(String hospitalId) {
        return adminRepository.findByHospitalId(hospitalId);
    }

    public List<Admin> findByRole(String role) {
        return adminRepository.findByRole(role);
    }

    public List<Admin> findByHospitalIdAndRole(String hospitalId, String role) {
        return adminRepository.findByHospitalIdAndRole(hospitalId, role);
    }

    public List<Admin> searchByKeyword(String keyword) {
        return adminRepository.findAll().stream()
                .filter(a -> (a.getUsername() != null && a.getUsername().contains(keyword)) ||
                             (a.getRealName() != null && a.getRealName().contains(keyword)))
                .toList();
    }

    public Admin update(String adminId, Admin adminUpdate) {
        Admin admin = findById(adminId);

        if (adminUpdate.getRealName() != null) {
            admin.setRealName(adminUpdate.getRealName());
        }
        if (adminUpdate.getPhone() != null) {
            admin.setPhone(adminUpdate.getPhone());
        }
        if (adminUpdate.getPassword() != null && !adminUpdate.getPassword().isEmpty()
                && !adminUpdate.getPassword().startsWith("$2a$")) {
            admin.setPassword(passwordEncoder.encode(adminUpdate.getPassword()));
        }
        if (adminUpdate.getStatus() != null) {
            admin.setStatus(adminUpdate.getStatus());
        }

        return adminRepository.save(admin);
    }

    public void delete(String adminId) {
        adminRepository.deleteById(adminId);
    }

    public Hospital getHospitalByAdmin(String adminId) {
        Admin admin = findById(adminId);
        return hospitalRepository.findById(admin.getHospitalId())
                .orElseThrow(() -> new RuntimeException("医院不存在"));
    }
}