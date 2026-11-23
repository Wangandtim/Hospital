package com.hospital.repository;

import com.hospital.entity.VoucherUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUseRepository extends JpaRepository<VoucherUse, String> {
}
