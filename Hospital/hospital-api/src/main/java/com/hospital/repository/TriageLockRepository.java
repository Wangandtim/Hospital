package com.hospital.repository;

import com.hospital.entity.TriageLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriageLockRepository extends JpaRepository<TriageLock, String> {

    Optional<TriageLock> findFirstByTriageIdAndStatusOrderByCreateTimeDesc(String triageId, Integer status);

    List<TriageLock> findByPatientIdAndStatus(String patientId, Integer status);

    List<TriageLock> findByTriageIdAndStatus(String triageId, Integer status);

    List<TriageLock> findByStatusAndExpireAtBefore(Integer status, LocalDateTime time);

    Optional<TriageLock> findFirstByPatientIdAndStatusOrderByCreateTimeDesc(String patientId, Integer status);
}
