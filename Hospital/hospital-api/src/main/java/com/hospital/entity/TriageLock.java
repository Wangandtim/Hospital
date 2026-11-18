package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 分诊号源锁：10 分钟 TTL，锁号即原子占位，出单后置为已消费。
 */
@Entity
@Table(name = "triage_lock")
public class TriageLock {

    @Id
    @Column(name = "lock_id")
    private String lockId;

    @Column(name = "triage_id", nullable = false)
    private String triageId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "dept_id")
    private String deptId;

    /** 1 锁定 / 2 已出单 / 0 已释放或过期 */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "release_reason")
    private String releaseReason;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "release_time")
    private LocalDateTime releaseTime;

    public boolean isActive() {
        return status != null && status == 1 && expireAt != null && expireAt.isAfter(LocalDateTime.now());
    }

    public String getLockId() { return lockId; }
    public void setLockId(String lockId) { this.lockId = lockId; }
    public String getTriageId() { return triageId; }
    public void setTriageId(String triageId) { this.triageId = triageId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getReleaseTime() { return releaseTime; }
    public void setReleaseTime(LocalDateTime releaseTime) { this.releaseTime = releaseTime; }
}
