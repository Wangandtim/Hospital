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

    /** 管理端保存医生介绍：全量覆盖（允许清空），不生成默认文案。 */
    public DoctorDetail saveForDoctor(String doctorId, DoctorDetail source) {
        DoctorDetail existing = findByDoctorId(doctorId);
        if (existing == null) {
            DoctorDetail created = new DoctorDetail();
            created.setDetailId(UUID.randomUUID().toString().replace("-", ""));
            created.setDoctorId(doctorId);
            created.setIntroduction(source.getIntroduction());
            created.setExpertiseDesc(source.getExpertiseDesc());
            created.setWorkExperience(source.getWorkExperience());
            created.setEducation(source.getEducation());
            created.setAwards(source.getAwards());
            created.setQualification(source.getQualification());
            created.setAcademicAppointments(source.getAcademicAppointments());
            created.setPhotoUrl(source.getPhotoUrl());
            created.setConsultationFee(source.getConsultationFee());
            return doctorDetailRepository.save(created);
        }
        existing.setIntroduction(source.getIntroduction());
        existing.setExpertiseDesc(source.getExpertiseDesc());
        existing.setWorkExperience(source.getWorkExperience());
        existing.setEducation(source.getEducation());
        existing.setAwards(source.getAwards());
        existing.setQualification(source.getQualification());
        existing.setAcademicAppointments(source.getAcademicAppointments());
        existing.setPhotoUrl(source.getPhotoUrl());
        existing.setConsultationFee(source.getConsultationFee());
        return doctorDetailRepository.save(existing);
    }

    /** 批量取医生照片 URL，用于列表，避免逐条查询。 */
    public java.util.Map<String, String> photoUrls(java.util.Collection<String> doctorIds) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (doctorIds == null || doctorIds.isEmpty()) {
            return result;
        }
        for (DoctorDetail detail : doctorDetailRepository.findByDoctorIdIn(doctorIds)) {
            if (detail.getPhotoUrl() != null) {
                result.put(detail.getDoctorId(), detail.getPhotoUrl());
            }
        }
        return result;
    }

    public void delete(String doctorId) {
        doctorDetailRepository.deleteByDoctorId(doctorId);
    }

    public boolean exists(String doctorId) {
        return doctorDetailRepository.existsByDoctorId(doctorId);
    }
}