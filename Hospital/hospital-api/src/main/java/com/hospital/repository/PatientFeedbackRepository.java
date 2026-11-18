package com.hospital.repository;

import com.hospital.entity.PatientFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientFeedbackRepository extends JpaRepository<PatientFeedback, String> {
    List<PatientFeedback> findByHospitalIdOrderByCreateTimeDesc(String hospitalId);
}
