package com.hospital.repository;

import com.hospital.entity.ScheduleWeeklyConfirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleWeeklyConfirmRepository extends JpaRepository<ScheduleWeeklyConfirm, String> {
    Optional<ScheduleWeeklyConfirm> findByDoctorIdAndWeekStart(String doctorId, LocalDate weekStart);
    boolean existsByDoctorIdAndWeekStart(String doctorId, LocalDate weekStart);
    List<ScheduleWeeklyConfirm> findByStatusAndWeekStartLessThanEqual(String status, LocalDate weekStart);
}
