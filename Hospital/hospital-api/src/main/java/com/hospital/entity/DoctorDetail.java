package com.hospital.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctor_detail")
public class DoctorDetail {

    @Id
    @Column(name = "detail_id")
    private String detailId;

    @Column(name = "doctor_id", nullable = false, unique = true)
    private String doctorId;

    @Column(name = "expertise_desc", columnDefinition = "TEXT")
    private String expertiseDesc;

    @Column(name = "consultation_fee")
    private Integer consultationFee;

    @Column(name = "qualification", columnDefinition = "TEXT")
    private String qualification;

    @Column(name = "work_experience", columnDefinition = "TEXT")
    private String workExperience;

    @Column(name = "education")
    private String education;

    @Column(name = "awards", columnDefinition = "TEXT")
    private String awards;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "academic_appointments", columnDefinition = "TEXT")
    private String academicAppointments;

    @Column(name = "photo_url")
    private String photoUrl;

    public DoctorDetail() {}

    public DoctorDetail(String detailId, String doctorId, String expertiseDesc, 
                        Integer consultationFee, String qualification) {
        this.detailId = detailId;
        this.doctorId = doctorId;
        this.expertiseDesc = expertiseDesc;
        this.consultationFee = consultationFee;
        this.qualification = qualification;
    }

    public String getDetailId() { return detailId; }
    public void setDetailId(String detailId) { this.detailId = detailId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getExpertiseDesc() { return expertiseDesc; }
    public void setExpertiseDesc(String expertiseDesc) { this.expertiseDesc = expertiseDesc; }
    public Integer getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Integer consultationFee) { this.consultationFee = consultationFee; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getWorkExperience() { return workExperience; }
    public void setWorkExperience(String workExperience) { this.workExperience = workExperience; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getAwards() { return awards; }
    public void setAwards(String awards) { this.awards = awards; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public String getAcademicAppointments() { return academicAppointments; }
    public void setAcademicAppointments(String academicAppointments) { this.academicAppointments = academicAppointments; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}