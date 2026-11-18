package com.hospital.repository;

import com.hospital.entity.HospitalArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalArticleRepository extends JpaRepository<HospitalArticle, String> {

    List<HospitalArticle> findByHospitalIdAndStatusOrderByCreateTimeDesc(String hospitalId, Integer status);

    List<HospitalArticle> findByHospitalIdOrderByCreateTimeDesc(String hospitalId);
}
