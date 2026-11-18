package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生下一周排班确认记录。
 * 周日提醒医生确认；未确认（pending）时由定时任务自动沿用上一周排班并置为 inherited。
 */
@Entity
@Table(name = "schedule_weekly_confirm")
public class ScheduleWeeklyConfirm {

    @Id
    @Column(name = "confirm_id")
    private String confirmId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** pending 待确认 / confirmed 医生已确认 / inherited 超时自动沿用 */
    @Column(name = "status", nullable = false)
    private String status = "pending";

    @Column(name = "remind_time")
    private LocalDateTime remindTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public ScheduleWeeklyConfirm() {}

    public String getConfirmId() { return confirmId; }
    public void setConfirmId(String confirmId) { this.confirmId = confirmId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRemindTime() { return remindTime; }
    public void setRemindTime(LocalDateTime remindTime) { this.remindTime = remindTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
