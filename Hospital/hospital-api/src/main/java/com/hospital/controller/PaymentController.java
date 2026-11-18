package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.entity.Doctor;
import com.hospital.entity.PaymentRecord;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PaymentRecordRepository;
import com.hospital.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final String[] ALLOWED_METHODS = {"wechat", "alipay", "card"};

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    // 获取支付信息
    @GetMapping("/prepare/{appointmentId}")
    public Map<String, Object> preparePayment(@PathVariable String appointmentId, HttpServletRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        Map<String, Object> result = new HashMap<>();

        if (appointment == null) {
            result.put("error", "预约不存在");
            result.put("message", "无法找到指定的预约记录");
            return result;
        }

        // 患者令牌只能查看自己的待缴费用
        if (noPatientAccess(appointment, request)) {
            result.put("error", "无权查看");
            result.put("message", "无权查看他人的缴费信息");
            return result;
        }

        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        double fee = doctor != null && doctor.getConsultationFee() != null ?
                     doctor.getConsultationFee() : 30.0;

        result.put("appointmentId", appointmentId);
        result.put("fee", fee);
        result.put("doctorName", doctor != null ? doctor.getName() : "未知医生");
        result.put("appointmentDate", appointment.getAppointmentDate().toString());
        result.put("timeSlot", appointment.getTimeSlot());
        result.put("orderNo", "ORDER_" + System.currentTimeMillis());

        return result;
    }

    /**
     * 挂号缴费：登录患者只能为自己的预约缴费；同一预约重复提交幂等返回原订单。
     */
    @PostMapping("/pay")
    public Map<String, Object> pay(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String appointmentId = stringValue(body.get("appointmentId"));
        String paymentMethod = stringValue(body.get("paymentMethod"));
        paymentMethod = paymentMethod == null ? "wechat" : paymentMethod.trim().toLowerCase();

        if (appointmentId == null) {
            result.put("success", false);
            result.put("message", "缺少预约编号");
            return result;
        }

        if (!Arrays.asList(ALLOWED_METHODS).contains(paymentMethod)) {
            result.put("success", false);
            result.put("message", "不支持的支付方式");
            return result;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            result.put("success", false);
            result.put("message", "预约不存在");
            return result;
        }

        if (appointment.getStatus() != null && appointment.getStatus() == 0) {
            result.put("success", false);
            result.put("message", "预约已取消，无法缴费");
            return result;
        }

        // 患者令牌只能支付自己的预约
        String role = attr(request, AuthInterceptor.ATTR_ROLE);
        String ownerId = attr(request, AuthInterceptor.ATTR_OWNER_ID);
        if ("PATIENT".equals(role) && appointment.getPatientId() != null
                && ownerId != null && !ownerId.equals(appointment.getPatientId())) {
            result.put("success", false);
            result.put("message", "无权支付他人的预约");
            return result;
        }

        // 幂等：已支付直接返回原订单号
        PaymentRecord existing = paymentRecordRepository.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
            result.put("success", true);
            result.put("appointmentId", appointmentId);
            result.put("orderNo", existing.getOrderNo());
            result.put("amount", existing.getAmount());
            result.put("paymentMethod", existing.getPaymentMethod());
            result.put("paidAt", format(existing.getPayTime()));
            result.put("alreadyPaid", true);
            return result;
        }

        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        double fee = doctor != null && doctor.getConsultationFee() != null ?
                     doctor.getConsultationFee() : 30.0;

        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(UUID.randomUUID().toString().replace("-", ""));
        record.setAppointmentId(appointmentId);
        record.setPatientId(appointment.getPatientId());
        record.setAmount(fee);
        record.setPaymentMethod(paymentMethod);
        record.setStatus(1);
        record.setOrderNo("PAY" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        record.setPayTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        result.put("success", true);
        result.put("message", "缴费成功");
        result.put("appointmentId", appointmentId);
        result.put("orderNo", record.getOrderNo());
        result.put("amount", fee);
        result.put("paymentMethod", paymentMethod);
        result.put("paidAt", format(record.getPayTime()));

        return result;
    }

    // 查询支付状态
    @GetMapping("/status/{appointmentId}")
    public Map<String, Object> checkPaymentStatus(@PathVariable String appointmentId, HttpServletRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

        // 患者令牌只能查询自己的缴费状态
        if (noPatientAccess(appointment, request)) {
            throw new RuntimeException("无权查看他人的缴费状态");
        }

        Map<String, Object> result = new HashMap<>();
        PaymentRecord record = paymentRecordRepository.findByAppointmentId(appointmentId).orElse(null);
        boolean paid = record != null && record.getStatus() != null && record.getStatus() == 1;

        result.put("appointmentId", appointmentId);
        result.put("paid", paid);
        result.put("orderNo", record != null ? record.getOrderNo() : null);
        result.put("amount", record != null ? record.getAmount() : null);
        result.put("paidAt", record != null ? format(record.getPayTime()) : null);
        result.put("status", appointment.getStatus());
        result.put("statusText", paid ? "已支付" : getStatusText(appointment.getStatus()));

        return result;
    }

    // 预约状态语义：0=已取消，1=待就诊，2=已完成，3=进行中
    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "已取消";
            case 1: return "待就诊";
            case 2: return "已完成";
            case 3: return "进行中";
            default: return "未知";
        }
    }

    /** 登录患者只能访问自己的预约；医生/管理员不受限。 */
    private boolean noPatientAccess(Appointment appointment, HttpServletRequest request) {
        String role = attr(request, AuthInterceptor.ATTR_ROLE);
        String ownerId = attr(request, AuthInterceptor.ATTR_OWNER_ID);
        return "PATIENT".equals(role) && ownerId != null
                && appointment.getPatientId() != null
                && !ownerId.equals(appointment.getPatientId());
    }

    private String attr(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
