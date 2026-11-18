package com.hospital.repository;

import com.hospital.entity.EmergencyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyLogRepository extends JpaRepository<EmergencyLog, String> {
    List<EmergencyLog> findByHospitalId(String hospitalId);
    List<EmergencyLog> findByLoginTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<EmergencyLog> findAllByOrderByLoginTimeDesc();
    List<EmergencyLog> findByStatus(Integer status);
    Optional<EmergencyLog> findByToken(String token);
    List<EmergencyLog> findByOperatorIdOrderByLoginTimeDesc(String operatorId);
}