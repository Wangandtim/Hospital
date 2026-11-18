package com.hospital.repository;

import com.hospital.entity.TriageSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageSessionRepository extends JpaRepository<TriageSession, String> {

    Optional<TriageSession> findByTriageId(String triageId);

    List<TriageSession> findByPatientIdOrderByCreateTimeDesc(String patientId);

    List<TriageSession> findByStatusInAndUpdateTimeBefore(List<String> statuses, java.time.LocalDateTime time);
}
