package com.hospital.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dept_id")
    private String deptId;

    @Column(name = "title")
    private String title;

    @Column(name = "expertise")
    private String expertise;

    @Column(name = "phone")
    private String phone;

    @Column(name = "education", columnDefinition = "TEXT")
    private String education;

    @Column(name = "moral", columnDefinition = "TEXT")
    private String moral;

    @Column(name = "experience", columnDefinition = "TEXT")
    private String experience;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "consultation_fee")
    private Integer consultationFee = 30;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Transient
    private Department department;

    public Doctor() {}

    public Doctor(String doctorId, String hospitalId, String name, String gender, String deptId,
                  String title, String expertise, String phone, String education, String moral,
                  String experience, Integer consultationFee) {
        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.gender = gender;
        this.deptId = deptId;
        this.title = title;
        this.expertise = expertise;
        this.phone = phone;
        this.education = education;
        this.moral = moral;
        this.experience = experience;
        this.consultationFee = consultationFee != null ? consultationFee : 30;
        this.status = 1;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getMoral() { return moral; }
    public void setMoral(String moral) { this.moral = moral; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Integer consultationFee) { this.consultationFee = consultationFee; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
}