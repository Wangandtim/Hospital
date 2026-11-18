package com.hospital.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次分诊会话：记录原始症状、规则结论、专家判定与唯一推荐。
 * 对应表 triage_session（见 resources/triage_v1.sql）。
 */
@Entity
@Table(name = "triage_session")
public class TriageSession {

    @Id
    @Column(name = "triage_id")
    private String triageId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "subject")
    private String subject = "SELF";

    @Column(name = "mode")
    private String mode = "STANDARD";

    @Column(name = "raw_input", columnDefinition = "TEXT")
    private String rawInput;

    @Column(name = "normalized_input")
    private String normalizedInput;

    @Column(name = "structured_symptoms", columnDefinition = "TEXT")
    private String structuredSymptoms;

    @Column(name = "red_flag")
    private Integer redFlag = 0;

    @Column(name = "red_flag_pattern")
    private String redFlagPattern;

    @Column(name = "red_flag_reason")
    private String redFlagReason;

    @Column(name = "age")
    private Integer age;

    @Column(name = "age_band")
    private String ageBand;

    @Column(name = "gender")
    private String gender;

    @Column(name = "pregnant")
    private String pregnant;

    @Column(name = "medical_history")
    private String medicalHistory;

    @Column(name = "dept_name")
    private String deptName;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "confidence_tier")
    private String confidenceTier;

    @Column(name = "need_expert")
    private String needExpert;

    @Column(name = "expert_reason")
    private String expertReason;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "match_reason")
    private String matchReason;

    @Column(name = "alternatives", columnDefinition = "TEXT")
    private String alternatives;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "status")
    private String status = "ASSESSED";

    @Column(name = "rule_version")
    private String ruleVersion;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "book_time")
    private LocalDateTime bookTime;

    public String getTriageId() { return triageId; }
    public void setTriageId(String triageId) { this.triageId = triageId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }
    public String getNormalizedInput() { return normalizedInput; }
    public void setNormalizedInput(String normalizedInput) { this.normalizedInput = normalizedInput; }
    public String getStructuredSymptoms() { return structuredSymptoms; }
    public void setStructuredSymptoms(String structuredSymptoms) { this.structuredSymptoms = structuredSymptoms; }
    public Integer getRedFlag() { return redFlag; }
    public void setRedFlag(Integer redFlag) { this.redFlag = redFlag; }
    public String getRedFlagPattern() { return redFlagPattern; }
    public void setRedFlagPattern(String redFlagPattern) { this.redFlagPattern = redFlagPattern; }
    public String getRedFlagReason() { return redFlagReason; }
    public void setRedFlagReason(String redFlagReason) { this.redFlagReason = redFlagReason; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getAgeBand() { return ageBand; }
    public void setAgeBand(String ageBand) { this.ageBand = ageBand; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPregnant() { return pregnant; }
    public void setPregnant(String pregnant) { this.pregnant = pregnant; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getConfidenceTier() { return confidenceTier; }
    public void setConfidenceTier(String confidenceTier) { this.confidenceTier = confidenceTier; }
    public String getNeedExpert() { return needExpert; }
    public void setNeedExpert(String needExpert) { this.needExpert = needExpert; }
    public String getExpertReason() { return expertReason; }
    public void setExpertReason(String expertReason) { this.expertReason = expertReason; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
    public String getAlternatives() { return alternatives; }
    public void setAlternatives(String alternatives) { this.alternatives = alternatives; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public LocalDateTime getBookTime() { return bookTime; }
    public void setBookTime(LocalDateTime bookTime) { this.bookTime = bookTime; }
}
