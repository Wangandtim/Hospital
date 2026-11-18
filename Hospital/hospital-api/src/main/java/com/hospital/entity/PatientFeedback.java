package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 患者服务反馈（针对具体医院或平台）。
 * 表结构由 docs/sql 或手工迁移创建，ddl-auto 为 none。
 */
@Entity
@Table(name = "patient_feedback")
public class PatientFeedback {

    @Id
    @Column(name = "feedback_id", length = 32)
    private String feedbackId;

    @Column(name = "patient_id", length = 32)
    private String patientId;

    @Column(name = "hospital_id", length = 32)
    private String hospitalId;

    @Column(name = "hospital_name", length = 100)
    private String hospitalName;

    @Column(name = "appointment_id", length = 32)
    private String appointmentId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "tags", length = 200)
    private String tags;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "contact", length = 50)
    private String contact;

    @Column(name = "status")
    private Integer status = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
