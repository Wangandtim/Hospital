package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospital")
public class Hospital {

    @Id
    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "hospital_name", nullable = false)
    private String hospitalName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "level")
    private String level;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "flagship_dept", columnDefinition = "TEXT")
    private String flagshipDept;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // 新增地区字段
    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    /** 医院级公告开关：1 启用、0 关闭；默认关闭，患者端不展示任何公告。 */
    @Column(name = "notice_enabled", nullable = false)
    private Boolean noticeEnabled = Boolean.FALSE;

    public Hospital() {}

    public Hospital(String hospitalId, String hospitalName, String address, String phone,
                    String level, String description, String flagshipDept, String websiteUrl) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.address = address;
        this.phone = phone;
        this.level = level;
        this.description = description;
        this.flagshipDept = flagshipDept;
        this.websiteUrl = websiteUrl;
        this.status = 1;
        this.createTime = LocalDateTime.now();
    }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFlagshipDept() { return flagshipDept; }
    public void setFlagshipDept(String flagshipDept) { this.flagshipDept = flagshipDept; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // 新增地区字段的getter/setter
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public Boolean getNoticeEnabled() {
        return noticeEnabled == null ? Boolean.FALSE : noticeEnabled;
    }
    public void setNoticeEnabled(Boolean noticeEnabled) { this.noticeEnabled = noticeEnabled; }
}
