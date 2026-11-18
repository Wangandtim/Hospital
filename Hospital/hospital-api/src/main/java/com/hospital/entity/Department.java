package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "department")
@Where(clause = "is_deleted = 0")
public class Department {

    @Id
    @Column(name = "dept_id")
    private String deptId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "dept_name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "diseases", columnDefinition = "TEXT")
    private String diseases;

    @Column(name = "service_time")
    private String serviceTime;

    @Column(name = "is_flagship")
    private Integer isFlagship = 0;

    /** 重点专科级别：0 未设置 / 1 国家级 / 2 省级 / 3 市级 */
    @Column(name = "flagship_level")
    private Integer flagshipLevel = 0;

    /** 科室获得的奖项（每行一条） */
    @Column(name = "awards", columnDefinition = "TEXT")
    private String awards;

    /** 典型/具体案例（每行一条） */
    @Column(name = "cases", columnDefinition = "TEXT")
    private String cases;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "is_deleted")
    private Integer isDeleted = 0;

    @Column(name = "delete_time")
    private LocalDateTime deleteTime;

    @Column(name = "deleted_by")
    private String deletedBy;

    public Department() {}

    public Department(String deptId, String hospitalId, String deptName, String description,
                     String diseases, String serviceTime, Integer isFlagship) {
        this.deptId = deptId;
        this.hospitalId = hospitalId;
        this.name = deptName;
        this.description = description;
        this.diseases = diseases;
        this.serviceTime = serviceTime;
        this.isFlagship = isFlagship != null ? isFlagship : 0;
        this.createTime = LocalDateTime.now();
    }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public String getDeptName() { return name; }
    public void setDeptName(String deptName) { this.name = deptName; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiseases() { return diseases; }
    public void setDiseases(String diseases) { this.diseases = diseases; }

    public String getServiceTime() { return serviceTime; }
    public void setServiceTime(String serviceTime) { this.serviceTime = serviceTime; }

    public Integer getIsFlagship() { return isFlagship; }
    public void setIsFlagship(Integer isFlagship) { this.isFlagship = isFlagship; }

    public Integer getFlagshipLevel() { return flagshipLevel; }
    public void setFlagshipLevel(Integer flagshipLevel) { this.flagshipLevel = flagshipLevel; }

    public String getAwards() { return awards; }
    public void setAwards(String awards) { this.awards = awards; }

    public String getCases() { return cases; }
    public void setCases(String cases) { this.cases = cases; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getDeleteTime() { return deleteTime; }
    public void setDeleteTime(LocalDateTime deleteTime) { this.deleteTime = deleteTime; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
}