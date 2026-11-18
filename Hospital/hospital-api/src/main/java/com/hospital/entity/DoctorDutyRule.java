package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生出诊规则：按「具体日期 + 起止时段」设定。
 * 医院管理员依据它核对医生当天是否在岗、本周时段是否达标。
 */
@Entity
@Table(name = "doctor_duty_rule")
public class DoctorDutyRule {

    @Id
    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    /** HH:mm */
    @Column(name = "start_time", nullable = false)
    private String startTime;

    /** HH:mm */
    @Column(name = "end_time", nullable = false)
    private String endTime;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public LocalDate getDutyDate() { return dutyDate; }
    public void setDutyDate(LocalDate dutyDate) { this.dutyDate = dutyDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
