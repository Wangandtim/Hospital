package com.hospital.repository;

import com.hospital.entity.DoctorDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorDetailRepository extends JpaRepository<DoctorDetail, String> {
    Optional<DoctorDetail> findByDoctorId(String doctorId);
    List<DoctorDetail> findByDoctorIdIn(Collection<String> doctorIds);
    void deleteByDoctorId(String doctorId);
    boolean existsByDoctorId(String doctorId);
}