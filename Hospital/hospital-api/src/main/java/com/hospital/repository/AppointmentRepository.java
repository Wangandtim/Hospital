package com.hospital.repository;

import com.hospital.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    List<Appointment> findByPatientIdOrderByCreateTimeDesc(String patientId);

    List<Appointment> findByOpenidOrderByCreateTimeDesc(String openid);

    List<Appointment> findByDoctorIdAndAppointmentDate(String doctorId, LocalDate date);
    
    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(String doctorId);

    boolean existsByPatientIdAndDoctorIdAndAppointmentDate(String patientId, String doctorId, LocalDate date);

    List<Appointment> findByPatientIdAndDoctorIdAndAppointmentDate(String patientId, String doctorId, LocalDate date);

    List<Appointment> findByScheduleId(String scheduleId);
}