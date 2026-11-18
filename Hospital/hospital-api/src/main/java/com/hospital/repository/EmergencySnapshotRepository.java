package com.hospital.repository;

import com.hospital.entity.EmergencySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmergencySnapshotRepository extends JpaRepository<EmergencySnapshot, String> {
    List<EmergencySnapshot> findByHospitalIdOrderByCreateTimeDesc(String hospitalId);
    void deleteByCreateTimeBefore(LocalDateTime time);
}
