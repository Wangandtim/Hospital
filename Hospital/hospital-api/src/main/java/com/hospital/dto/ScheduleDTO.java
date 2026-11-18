package com.hospital.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public class ScheduleDTO {
    
    private String scheduleId;
    private String hospitalId;
    private String doctorId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;
    private String timeSlot;
    private Integer maxPatients;
    private Integer bookedCount;
    private Integer status;
    
    public ScheduleDTO() {}
    
    public ScheduleDTO(String scheduleId, String hospitalId, String doctorId, 
                      LocalDate workDate, String timeSlot, Integer maxPatients,
                      Integer bookedCount, Integer status) {
        this.scheduleId = scheduleId;
        this.hospitalId = hospitalId;
        this.doctorId = doctorId;
        this.workDate = workDate;
        this.timeSlot = timeSlot;
        this.maxPatients = maxPatients;
        this.bookedCount = bookedCount;
        this.status = status;
    }
    
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
}