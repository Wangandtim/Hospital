package com.hospital.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 应急操作前的数据快照，用于管理员一键回滚。 */
@Entity
@Table(name = "emergency_snapshot")
public class EmergencySnapshot {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "record_id")
    private String recordId;

    @Column(name = "data_json", columnDefinition = "LONGTEXT")
    private String dataJson;

    @Column(name = "token")
    private String token;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public EmergencySnapshot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
