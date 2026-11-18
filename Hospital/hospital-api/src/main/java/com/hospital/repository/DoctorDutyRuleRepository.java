package com.hospital.repository;

import com.hospital.entity.DoctorDutyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorDutyRuleRepository extends JpaRepository<DoctorDutyRule, String> {

    List<DoctorDutyRule> findByDoctorIdAndDutyDateBetweenOrderByDutyDateAscStartTimeAsc(
            String doctorId, LocalDate from, LocalDate to);

    List<DoctorDutyRule> findByHospitalIdAndDutyDateBetweenOrderByDutyDateAscStartTimeAsc(
            String hospitalId, LocalDate from, LocalDate to);

    List<DoctorDutyRule> findByHospitalIdAndDutyDateOrderByStartTimeAsc(String hospitalId, LocalDate dutyDate);

    List<DoctorDutyRule> findByDoctorIdAndDutyDateOrderByStartTimeAsc(String doctorId, LocalDate dutyDate);
}
