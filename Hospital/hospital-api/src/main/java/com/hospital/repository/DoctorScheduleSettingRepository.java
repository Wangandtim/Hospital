package com.hospital.repository;

import com.hospital.entity.DoctorScheduleSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorScheduleSettingRepository extends JpaRepository<DoctorScheduleSetting, String> {
    List<DoctorScheduleSetting> findByDoctorId(String doctorId);
    List<DoctorScheduleSetting> findByDoctorIdAndEnabled(String doctorId, Integer enabled);
    List<DoctorScheduleSetting> findByDoctorIdAndDayOfWeek(String doctorId, Integer dayOfWeek);
    void deleteByDoctorId(String doctorId);
}