package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 挂号缴费记录：一条预约对应一条支付记录，用于线上支付与状态查询。
 */
@Entity
@Table(name = "payment_record")
public class PaymentRecord {

    @Id
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    /** 1=已支付 */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public PaymentRecord() {}

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
