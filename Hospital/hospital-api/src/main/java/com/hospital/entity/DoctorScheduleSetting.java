package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedule_setting")
public class DoctorScheduleSetting {

    @Id
    @Column(name = "setting_id")
    private String settingId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "enabled")
    private Integer enabled = 1;

    @Column(name = "slot_duration")
    private Integer slotDuration = 20;

    @Column(name = "max_patients_per_slot")
    private Integer maxPatientsPerSlot = 2;

    public DoctorScheduleSetting() {}

    public DoctorScheduleSetting(String settingId, String doctorId, Integer dayOfWeek, 
                                 LocalTime startTime, LocalTime endTime, Integer enabled,
                                 Integer slotDuration, Integer maxPatientsPerSlot) {
        this.settingId = settingId;
        this.doctorId = doctorId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.enabled = enabled != null ? enabled : 1;
        this.slotDuration = slotDuration != null ? slotDuration : 20;
        this.maxPatientsPerSlot = maxPatientsPerSlot != null ? maxPatientsPerSlot : 2;
    }

    public String getSettingId() { return settingId; }
    public void setSettingId(String settingId) { this.settingId = settingId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getSlotDuration() { return slotDuration; }
    public void setSlotDuration(Integer slotDuration) { this.slotDuration = slotDuration; }
    public Integer getMaxPatientsPerSlot() { return maxPatientsPerSlot; }
    public void setMaxPatientsPerSlot(Integer maxPatientsPerSlot) { this.maxPatientsPerSlot = maxPatientsPerSlot; }
}