package com.hospital.service;

import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import com.hospital.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    public Patient findById(String patientId) {
        // 先尝试按 patient_id 查找
        Optional<Patient> patient = patientRepository.findByPatientId(patientId);
        if (patient.isPresent()) {
            return patient.get();
        }
        // 如果找不到，再按主键 id 查找
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
    }

    public Patient create(Patient patient) {
        if (patient.getPatientId() == null || patient.getPatientId().isEmpty()) {
            patient.setPatientId("P" + System.currentTimeMillis() % 100000);
        }
        if (patientRepository.existsById(patient.getPatientId())) {
            throw new RuntimeException("该患者已存在");
        }
        if (!passwordUtil.isEncoded(patient.getPassword())) {
            patient.setPassword(passwordUtil.encode(patient.getPassword()));
        }
        return patientRepository.save(patient);
    }

    public Patient update(String patientId, Patient patient) {
        Patient existing = findById(patientId);
        existing.setName(patient.getName());
        existing.setGender(patient.getGender());
        existing.setAge(patient.getAge());
        existing.setPhone(patient.getPhone());
        existing.setMedicalHistory(patient.getMedicalHistory());
        return patientRepository.save(existing);
    }

    public void delete(String patientId) {
        patientRepository.deleteById(patientId);
    }

    public Patient login(String phone, String password) {
        Patient patient = patientRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        if (!passwordUtil.matches(password, patient.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        // 历史明文密码在首次登录成功后自动升级为哈希
        if (!passwordUtil.isEncoded(patient.getPassword())) {
            patient.setPassword(passwordUtil.encode(password));
            patientRepository.save(patient);
        }
        return patient;
    }
}