package com.hospital.repository;

import com.hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {
    
    List<Doctor> findByHospitalId(String hospitalId);
    
    List<Doctor> findByDeptId(String deptId);
    
    List<Doctor> findByDeptIdAndHospitalId(String deptId, String hospitalId);
    
    List<Doctor> findByStatus(Integer status);
    
    Optional<Doctor> findByUsername(String username);
    
    Optional<Doctor> findByPhone(String phone);
    
    Optional<Doctor> findById(String id);
    
    List<Doctor> findByNameContaining(String keyword);
    
    List<Doctor> findByExpertiseContaining(String keyword);
}