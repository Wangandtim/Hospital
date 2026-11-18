package com.hospital.service;

import com.hospital.entity.Doctor;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalRepository;
import com.hospital.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    public List<Doctor> findByHospitalId(String hospitalId) {
        return doctorRepository.findByHospitalId(hospitalId);
    }

    public Doctor findById(String doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
    }

    public List<Doctor> findByDeptId(String deptId) {
        departmentService.findById(deptId);
        return doctorRepository.findByDeptId(deptId);
    }

    public List<Doctor> findByDeptIdAndHospitalId(String deptId, String hospitalId) {
        departmentService.findById(deptId);
        return doctorRepository.findByDeptIdAndHospitalId(deptId, hospitalId);
    }

    public Doctor create(Doctor doctor) {
        if (doctor.getDoctorId() == null) {
            doctor.setDoctorId("DOC" + System.currentTimeMillis() % 100000);
        }
        if (doctor.getHospitalId() != null) {
            hospitalRepository.existsById(doctor.getHospitalId());
        }
        if (doctor.getDeptId() != null) {
            departmentService.findById(doctor.getDeptId());
        }
        doctor.setPassword(passwordUtil.encode(doctor.getPassword()));
        return doctorRepository.save(doctor);
    }

    public Doctor update(String doctorId, Doctor doctor) {
        Doctor existing = findById(doctorId);
        existing.setName(doctor.getName());
        existing.setGender(doctor.getGender());
        existing.setTitle(doctor.getTitle());
        existing.setExpertise(doctor.getExpertise());
        existing.setPhone(doctor.getPhone());
        existing.setStatus(doctor.getStatus());
        existing.setConsultationFee(doctor.getConsultationFee());

        if (doctor.getDeptId() != null) {
            departmentService.findById(doctor.getDeptId());
            existing.setDeptId(doctor.getDeptId());
        }

        // 仅在显式传入新密码时更新，并统一转为哈希（已哈希的密码不重复加密）
        if (doctor.getPassword() != null && !doctor.getPassword().isEmpty()
                && !passwordUtil.isEncoded(doctor.getPassword())) {
            existing.setPassword(passwordUtil.encode(doctor.getPassword()));
        }

        return doctorRepository.save(existing);
    }

    public void delete(String doctorId) {
        doctorRepository.deleteById(doctorId);
    }
    
    public List<Doctor> search(String keyword) {
        return doctorRepository.findByNameContaining(keyword);
    }
    
    public List<Doctor> searchByName(String keyword) {
        return doctorRepository.findByNameContaining(keyword);
    }
    
    public List<Doctor> searchByExpertise(String keyword) {
        return doctorRepository.findByExpertiseContaining(keyword);
    }
    
    public List<Map<String, Object>> searchWithScore(String keyword) {
        List<Doctor> doctors = search(keyword);
        String lowerKeyword = keyword.toLowerCase();
        
        return doctors.stream()
                .map(d -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("doctor", d);
                    int score = calculateScore(d, lowerKeyword);
                    result.put("score", score);
                    result.put("type", "doctor");
                    return result;
                })
                .sorted((a, b) -> (Integer)b.get("score") - (Integer)a.get("score"))
                .collect(Collectors.toList());
    }
    
    private int calculateScore(Doctor d, String keyword) {
        int score = 0;
        String name = d.getName().toLowerCase();
        String title = d.getTitle() != null ? d.getTitle().toLowerCase() : "";
        String expertise = d.getExpertise() != null ? d.getExpertise().toLowerCase() : "";
        
        if (name.equals(keyword)) {
            score += 100;
        } else if (name.startsWith(keyword)) {
            score += 80;
        } else if (name.contains(keyword)) {
            score += 60;
        }
        
        if (title.contains(keyword)) {
            score += 50;
        }
        
        if (expertise.contains(keyword)) {
            score += 40;
        }
        
        if (d.getTitle() != null && d.getTitle().contains("主任医师")) {
            score += 15;
        } else if (d.getTitle() != null && d.getTitle().contains("副主任医师")) {
            score += 10;
        }
        
        return score;
    }
    
    public List<Map<String, String>> getSuggestions(String keyword) {
        List<Doctor> doctors = searchByName(keyword);
        return doctors.stream()
                .map(d -> {
                    Map<String, String> suggestion = new HashMap<>();
                    suggestion.put("name", d.getName());
                    suggestion.put("title", d.getTitle());
                    suggestion.put("type", "doctor");
                    suggestion.put("id", d.getDoctorId());
                    return suggestion;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    public Doctor login(String username, String password) {
        Doctor doctor = doctorRepository.findByUsername(username).orElse(null);
        if (doctor == null) {
            throw new RuntimeException("医生不存在");
        }
        if (!passwordUtil.matches(password, doctor.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        // 历史明文密码在首次登录成功后自动升级为哈希
        if (!passwordUtil.isEncoded(doctor.getPassword())) {
            doctor.setPassword(passwordUtil.encode(password));
            doctorRepository.save(doctor);
        }
        return doctor;
    }
}
