package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_log")
public class EmergencyLog {

    @Id
    @Column(name = "log_id")
    private String logId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "token_expire_time")
    private LocalDateTime tokenExpireTime;

    @Column(name = "token")
    private String token;

    @Column(name = "status")
    private Integer status = 1;

    /** 授权来源：KEY=密钥登录，DOCTOR=医生紧急申请 */
    @Column(name = "operator_type")
    private String operatorType;

    @Column(name = "operator_id")
    private String operatorId;

    public EmergencyLog() {}

    public EmergencyLog(String logId, String hospitalId, String hospitalName, 
                       String ipAddress, LocalDateTime tokenExpireTime) {
        this.logId = logId;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.loginTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.tokenExpireTime = tokenExpireTime;
        this.status = 1;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }

    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getOperatorType() { return operatorType; }
    public void setOperatorType(String operatorType) { this.operatorType = operatorType; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
}