package com.hospital.repository;

import com.hospital.entity.HospitalKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalKeyRepository extends JpaRepository<HospitalKey, String> {
    Optional<HospitalKey> findByDailyKey(String dailyKey);
    Optional<HospitalKey> findByHospitalIdAndStatus(String hospitalId, Integer status);
}