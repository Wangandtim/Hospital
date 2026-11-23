package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "openid")
    private String openid;

    @Column(name = "schedule_id", nullable = false)
    private String scheduleId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "time_slot")
    private String timeSlot;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Column(name = "questionnaire_data", columnDefinition = "TEXT")
    private String questionnaireData;

    @Column(name = "queue_no")
    private String queueNo;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Transient
    private Patient patient;

    @Transient
    private Doctor doctor;

    @Transient
    private Schedule schedule;

    public Appointment() {}

    public Appointment(String appointmentId, String patientId, String scheduleId,
                       String doctorId, LocalDate appointmentDate, String timeSlot) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.scheduleId = scheduleId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.status = 1;
        this.createTime = LocalDateTime.now();
    }

    // 业务方法：是否可以取消（提前1天）
    public boolean canCancel() {
        if (status != 1) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentDateTime = appointmentDate.atStartOfDay();
        return now.isBefore(appointmentDateTime.minusDays(1));
    }

    // 检查并更新预约状态 - 只有待就诊(1)和进行中(3)才参与自动状态变化
    public void checkAndUpdateStatus() {
        // 【关键：已取消(0)或已完成(2)，绝对不参与自动状态更新
        if (status == 0 || status == 2) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 解析时间槽 (格式: "09:00-10:00")
        LocalDateTime appointmentDateTime = null;
        if (timeSlot != null && timeSlot.contains("-")) {
            try {
                // 先按 "-" 分割获取开始时间
                String[] parts = timeSlot.split("-");
                if (parts.length > 0 && parts[0].contains(":")) {
                    String[] timeParts = parts[0].split(":");
                    if (timeParts.length >= 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        appointmentDateTime = appointmentDate.atTime(hour, minute);
                    }
                }
            } catch (Exception e) {
                // 解析失败，使用默认值
            }
        }
        
        // 如果解析失败，使用当天开始时间
        if (appointmentDateTime == null) {
            appointmentDateTime = appointmentDate.atStartOfDay();
        }

        // 已报到（有排队号与报到时间）：不按时间回退为待就诊，
        // 就诊窗口结束后转已完成，窗口内保持进行中。
        if (queueNo != null && checkinTime != null) {
            this.status = now.isAfter(appointmentDateTime.plusHours(2)) ? 2 : 3;
            return;
        }

        // 判断状态
        if (now.isAfter(appointmentDateTime)) {
            // 时间已过，设为已完成
            this.status = 2;
        } else if (now.isAfter(appointmentDateTime.minusMinutes(30)) && now.isBefore(appointmentDateTime.plusHours(2))) {
            // 预约前30分钟到开始后2小时内，设为进行中
            this.status = 3;
        } else {
            // 其他情况，设为待就诊（防止异常状态回到待就诊）
            this.status = 1;
        }
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    public String getQuestionnaireData() { return questionnaireData; }
    public void setQuestionnaireData(String questionnaireData) { this.questionnaireData = questionnaireData; }
    public String getQueueNo() { return queueNo; }
    public void setQueueNo(String queueNo) { this.queueNo = queueNo; }
    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }
}