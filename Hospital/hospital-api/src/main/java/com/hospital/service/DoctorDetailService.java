package com.hospital.service;

import com.hospital.entity.DoctorDetail;
import com.hospital.repository.DoctorDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DoctorDetailService {

    @Autowired
    private DoctorDetailRepository doctorDetailRepository;

    public DoctorDetail findByDoctorId(String doctorId) {
        return doctorDetailRepository.findByDoctorId(doctorId).orElse(null);
    }

    public DoctorDetail create(DoctorDetail detail) {
        if (detail.getDetailId() == null) {
            detail.setDetailId(UUID.randomUUID().toString().replace("-", ""));
        }
        return doctorDetailRepository.save(detail);
    }

    public DoctorDetail update(String doctorId, DoctorDetail detail) {
        DoctorDetail existing = findByDoctorId(doctorId);
        if (existing == null) {
            detail.setDoctorId(doctorId);
            return create(detail);
        }
        
        if (detail.getExpertiseDesc() != null) {
            existing.setExpertiseDesc(detail.getExpertiseDesc());
        }
        if (detail.getConsultationFee() != null) {
            existing.setConsultationFee(detail.getConsultationFee());
        }
        if (detail.getQualification() != null) {
            existing.setQualification(detail.getQualification());
        }
        if (detail.getWorkExperience() != null) {
            existing.setWorkExperience(detail.getWorkExperience());
        }
        if (detail.getEducation() != null) {
            existing.setEducation(detail.getEducation());
        }
        if (detail.getAwards() != null) {
            existing.setAwards(detail.getAwards());
        }
        if (detail.getIntroduction() != null) {
            existing.setIntroduction(detail.getIntroduction());
        }
        
        return doctorDetailRepository.save(existing);
    }

    public void delete(String doctorId) {
        doctorDetailRepository.deleteByDoctorId(doctorId);
    }

    public boolean exists(String doctorId) {
        return doctorDetailRepository.existsByDoctorId(doctorId);
    }
}