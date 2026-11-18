package com.hospital.repository;

import com.hospital.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByUsernameAndStatus(String username, Integer status);
    List<Admin> findByHospitalId(String hospitalId);
    List<Admin> findByRole(String role);
    List<Admin> findByHospitalIdAndRole(String hospitalId, String role);
}