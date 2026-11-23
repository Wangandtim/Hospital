package com.hospital.repository;

import com.hospital.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByDoctorIdAndAppointmentDateAndStatus(String doctorId, java.time.LocalDate appointmentDate, Integer status);

    /** 我的预约：按医院名/科室名/医生名关键词在数据库侧过滤（原生 SQL 联表）。 */
    @Query(value = "SELECT a.* FROM appointment a "
            + "LEFT JOIN doctor d ON d.doctor_id = a.doctor_id "
            + "LEFT JOIN schedule s ON s.schedule_id = a.schedule_id "
            + "LEFT JOIN department dp ON dp.dept_id = d.dept_id "
            + "LEFT JOIN hospital h ON h.hospital_id = s.hospital_id "
            + "WHERE a.patient_id = :patientId AND ("
            + "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(h.hospital_name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(dp.dept_name) LIKE LOWER(CONCAT('%', :keyword, '%'))"
            + ") ORDER BY a.create_time DESC, a.appointment_id DESC",
            nativeQuery = true)
    List<Appointment> searchByPatientAndKeyword(@Param("patientId") String patientId,
                                                @Param("keyword") String keyword);
}