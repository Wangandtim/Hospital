package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_key")
public class HospitalKey {

    @Id
    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "daily_key", nullable = false, unique = true)
    private String dailyKey;

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "last_renew_time")
    private LocalDateTime lastRenewTime;

    @Column(name = "status")
    private Integer status = 1;

    /** 常设应急码：管理员账号不可用时使用，由医院长期封存，卫健委可重置 */
    @Column(name = "recovery_key")
    private String recoveryKey;

    @Column(name = "recovery_status")
    private Integer recoveryStatus = 1;

    public HospitalKey() {}

    public HospitalKey(String hospitalId, String dailyKey, LocalDateTime expireTime) {
        this.hospitalId = hospitalId;
        this.dailyKey = dailyKey;
        this.expireTime = expireTime;
        this.createTime = LocalDateTime.now();
        this.status = 1;
    }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDailyKey() { return dailyKey; }
    public void setDailyKey(String dailyKey) { this.dailyKey = dailyKey; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastRenewTime() { return lastRenewTime; }
    public void setLastRenewTime(LocalDateTime lastRenewTime) { this.lastRenewTime = lastRenewTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRecoveryKey() { return recoveryKey; }
    public void setRecoveryKey(String recoveryKey) { this.recoveryKey = recoveryKey; }

    public Integer getRecoveryStatus() { return recoveryStatus; }
    public void setRecoveryStatus(Integer recoveryStatus) { this.recoveryStatus = recoveryStatus; }
}