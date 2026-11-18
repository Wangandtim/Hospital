package com.hospital.repository;

import com.hospital.entity.EmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, String> {
    List<EmergencyRequest> findByHospitalIdAndStatusOrderByCreateTimeAsc(String hospitalId, Integer status);
    List<EmergencyRequest> findByDoctorIdOrderByCreateTimeDesc(String doctorId);
    List<EmergencyRequest> findByHospitalIdAndStatusOrderByCreateTimeDesc(String hospitalId, Integer status);
    List<EmergencyRequest> findByHospitalIdOrderByCreateTimeDesc(String hospitalId);

    List<EmergencyRequest> findByStatusAndTokenExpireTimeBefore(Integer status, LocalDateTime time);
    List<EmergencyRequest> findByStatusAndReportDeadlineBefore(Integer status, LocalDateTime time);
    List<EmergencyRequest> findByStatusAndConfirmDeadlineBefore(Integer status, LocalDateTime time);

    Optional<EmergencyRequest> findFirstByDoctorIdAndCertIdPathIsNotNullOrderByCreateTimeDesc(String doctorId);
    Optional<EmergencyRequest> findFirstByDoctorIdAndCertIdPathIsNotNullAndCertLicensePathIsNotNullOrderByCreateTimeDesc(String doctorId);
    Optional<EmergencyRequest> findFirstByDoctorIdOrderByCreateTimeDesc(String doctorId);

    List<EmergencyRequest> findByToken(String token);

    List<EmergencyRequest> findByStatusInAndHospitalIdOrderByCreateTimeDesc(List<Integer> statuses, String hospitalId);
    List<EmergencyRequest> findByStatusInOrderByCreateTimeDesc(List<Integer> statuses);
}
