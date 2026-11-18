package com.hospital.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @Column(name = "admin_id")
    private String adminId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "role", nullable = false)
    private String role = "HOSPITAL_ADMIN";

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_key_expire")
    private LocalDateTime apiKeyExpire;

    @Column(name = "id_card_image")
    private String idCardImage;

    @Column(name = "work_card_image")
    private String workCardImage;

    @Column(name = "audit_status")
    private Integer auditStatus = 0;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @Column(name = "audit_comment")
    private String auditComment;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
    private Hospital hospital;

    public Admin() {}

    public Admin(String adminId, String username, String password, String realName, String hospitalId, String role) {
        this.adminId = adminId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.hospitalId = hospitalId;
        this.role = role != null ? role : "HOSPITAL_ADMIN";
        this.status = 1;
        this.createTime = LocalDateTime.now();
    }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public LocalDateTime getApiKeyExpire() { return apiKeyExpire; }
    public void setApiKeyExpire(LocalDateTime apiKeyExpire) { this.apiKeyExpire = apiKeyExpire; }

    public String getIdCardImage() { return idCardImage; }
    public void setIdCardImage(String idCardImage) { this.idCardImage = idCardImage; }

    public String getWorkCardImage() { return workCardImage; }
    public void setWorkCardImage(String workCardImage) { this.workCardImage = workCardImage; }

    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }

    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }

    public String getAuditComment() { return auditComment; }
    public void setAuditComment(String auditComment) { this.auditComment = auditComment; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }
}