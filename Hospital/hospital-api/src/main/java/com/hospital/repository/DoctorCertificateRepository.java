package com.hospital.repository;

import com.hospital.entity.DoctorCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorCertificateRepository extends JpaRepository<DoctorCertificate, String> {
    List<DoctorCertificate> findByDoctorIdOrderBySortOrderAscCreateTimeAsc(String doctorId);
    Optional<DoctorCertificate> findByCertificateIdAndDoctorId(String certificateId, String doctorId);
    void deleteByDoctorId(String doctorId);
}
