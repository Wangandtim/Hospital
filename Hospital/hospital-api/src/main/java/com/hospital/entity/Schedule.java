package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "schedule")
@Where(clause = "is_deleted = 0")
public class Schedule {

    @Id
    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "time_slot", nullable = false)
    private String timeSlot;

    @Column(name = "max_patients")
    private Integer maxPatients = 2;

    @Column(name = "booked_count")
    private Integer bookedCount = 0;

    @Column(name = "status")
    private Integer status = 1;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @Column(name = "is_deleted")
    private Integer isDeleted = 0;

    @Column(name = "delete_time")
    private java.time.LocalDateTime deleteTime;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Transient
    private Doctor doctor;

    public Schedule() {}

    public Schedule(String scheduleId, String hospitalId, String doctorId, LocalDate workDate,
                    String timeSlot, Integer maxPatients) {
        this.scheduleId = scheduleId;
        this.hospitalId = hospitalId;
        this.doctorId = doctorId;
        this.workDate = workDate;
        this.timeSlot = timeSlot;
        this.maxPatients = maxPatients;
        this.bookedCount = 0;
        this.status = 1;
    }

    // 业务方法
    public boolean hasAvailableSlot() {
        return bookedCount < maxPatients && status == 1;
    }

    public Integer getRemainingSlots() {
        return maxPatients - bookedCount;
    }

    // Getters and Setters
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public Integer getMaxPatients() { return maxPatients; }
    public void setMaxPatients(Integer maxPatients) { this.maxPatients = maxPatients; }
    public Integer getBookedCount() { return bookedCount; }
    public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public java.time.LocalDateTime getDeleteTime() { return deleteTime; }
    public void setDeleteTime(java.time.LocalDateTime deleteTime) { this.deleteTime = deleteTime; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}
