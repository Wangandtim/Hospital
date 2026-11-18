package com.hospital.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 应急通道使用期间的写操作明细，用于结束报告自动生成改动范围。 */
@Entity
@Table(name = "emergency_operation")
public class EmergencyOperation {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "token")
    private String token;

    @Column(name = "method")
    private String method;

    @Column(name = "path")
    private String path;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "summary")
    private String summary;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public EmergencyOperation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
