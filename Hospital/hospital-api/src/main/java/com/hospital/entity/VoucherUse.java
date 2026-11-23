package com.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 就诊凭证使用记录：nonce 为主键，保证同一凭证只能使用一次。 */
@Entity
@Table(name = "appointment_voucher_use")
public class VoucherUse {

    @Id
    @Column(name = "voucher_nonce")
    private String voucherNonce;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    public String getVoucherNonce() { return voucherNonce; }
    public void setVoucherNonce(String voucherNonce) { this.voucherNonce = voucherNonce; }
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
