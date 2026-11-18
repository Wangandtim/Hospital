package com.hospital.repository;

import com.hospital.entity.EmergencyOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyOperationRepository extends JpaRepository<EmergencyOperation, String> {
    List<EmergencyOperation> findByRequestIdOrderByCreateTimeAsc(String requestId);
    List<EmergencyOperation> findByTokenOrderByCreateTimeAsc(String token);
}
