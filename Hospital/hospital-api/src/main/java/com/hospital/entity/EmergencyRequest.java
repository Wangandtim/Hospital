package com.hospital.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 医生应急通道申请。
 * 状态机：0 使用中·待核对 / 1 已结束·待报告 / 2 待卫健委确认 / 3 已确认归档 /
 * 4 已驳回异常 / 5 已中止 / 6 逾期未交报告 / 7 逾期未确认。
 */
@Entity
@Table(name = "emergency_request")
public class EmergencyRequest {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "doctor_title")
    private String doctorTitle;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status")
    private Integer status = 0;

    /** 旧审批流字段，保留兼容历史数据，不再使用。 */
    @Column(name = "approver_id")
    private String approverId;

    @Column(name = "approver_name")
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "token")
    private String token;

    @Column(name = "token_expire_time")
    private LocalDateTime tokenExpireTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "cert_id_path")
    private String certIdPath;

    @Column(name = "cert_license_path")
    private String certLicensePath;

    /** 0 未核对 / 1 已核对一致 / 2 核对不一致 */
    @Column(name = "cert_verified")
    private Integer certVerified = 0;

    @Column(name = "cert_verified_by")
    private String certVerifiedBy;

    @Column(name = "cert_verified_time")
    private LocalDateTime certVerifiedTime;

    @Column(name = "cert_verify_remark")
    private String certVerifyRemark;

    @Column(name = "auto_grant")
    private Integer autoGrant = 1;

    @Column(name = "report_scope", columnDefinition = "TEXT")
    private String reportScope;

    @Column(name = "report_impact", columnDefinition = "TEXT")
    private String reportImpact;

    @Column(name = "report_complete")
    private Integer reportComplete = 0;

    @Column(name = "report_time")
    private LocalDateTime reportTime;

    @Column(name = "report_deadline")
    private LocalDateTime reportDeadline;

    @Column(name = "confirm_deadline")
    private LocalDateTime confirmDeadline;

    @Column(name = "confirm_by")
    private String confirmBy;

    @Column(name = "confirm_by_name")
    private String confirmByName;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "confirm_remark")
    private String confirmRemark;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "end_reason")
    private String endReason;

    public EmergencyRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorTitle() { return doctorTitle; }
    public void setDoctorTitle(String doctorTitle) { this.doctorTitle = doctorTitle; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getTokenExpireTime() { return tokenExpireTime; }
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) { this.tokenExpireTime = tokenExpireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getCertIdPath() { return certIdPath; }
    public void setCertIdPath(String certIdPath) { this.certIdPath = certIdPath; }

    public String getCertLicensePath() { return certLicensePath; }
    public void setCertLicensePath(String certLicensePath) { this.certLicensePath = certLicensePath; }

    public Integer getCertVerified() { return certVerified; }
    public void setCertVerified(Integer certVerified) { this.certVerified = certVerified; }

    public String getCertVerifiedBy() { return certVerifiedBy; }
    public void setCertVerifiedBy(String certVerifiedBy) { this.certVerifiedBy = certVerifiedBy; }

    public LocalDateTime getCertVerifiedTime() { return certVerifiedTime; }
    public void setCertVerifiedTime(LocalDateTime certVerifiedTime) { this.certVerifiedTime = certVerifiedTime; }

    public String getCertVerifyRemark() { return certVerifyRemark; }
    public void setCertVerifyRemark(String certVerifyRemark) { this.certVerifyRemark = certVerifyRemark; }

    public Integer getAutoGrant() { return autoGrant; }
    public void setAutoGrant(Integer autoGrant) { this.autoGrant = autoGrant; }

    public String getReportScope() { return reportScope; }
    public void setReportScope(String reportScope) { this.reportScope = reportScope; }

    public String getReportImpact() { return reportImpact; }
    public void setReportImpact(String reportImpact) { this.reportImpact = reportImpact; }

    public Integer getReportComplete() { return reportComplete; }
    public void setReportComplete(Integer reportComplete) { this.reportComplete = reportComplete; }

    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }

    public LocalDateTime getReportDeadline() { return reportDeadline; }
    public void setReportDeadline(LocalDateTime reportDeadline) { this.reportDeadline = reportDeadline; }

    public LocalDateTime getConfirmDeadline() { return confirmDeadline; }
    public void setConfirmDeadline(LocalDateTime confirmDeadline) { this.confirmDeadline = confirmDeadline; }

    public String getConfirmBy() { return confirmBy; }
    public void setConfirmBy(String confirmBy) { this.confirmBy = confirmBy; }

    public String getConfirmByName() { return confirmByName; }
    public void setConfirmByName(String confirmByName) { this.confirmByName = confirmByName; }

    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }

    public String getConfirmRemark() { return confirmRemark; }
    public void setConfirmRemark(String confirmRemark) { this.confirmRemark = confirmRemark; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}
