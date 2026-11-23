package com.hospital.service;

import com.hospital.entity.Appointment;
import com.hospital.entity.Schedule;
import com.hospital.entity.WechatUser;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.ScheduleRepository;
import com.hospital.repository.WechatUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private ScheduleService scheduleService;

    // 通过 openid 查询预约
    public List<Appointment> findByPatientOpenid(String openid) {
        WechatUser user = wechatUserRepository.findById(openid).orElse(null);
        if (user != null && user.getPatient() != null) {
            return appointmentRepository.findByPatientIdOrderByCreateTimeDesc(user.getPatient().getPatientId());
        }
        return new ArrayList<>();
    }

    // 查询患者的预约历史
    public List<Appointment> findByPatient(String patientId) {
        return appointmentRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
    }

    /** 我的预约：keyword 非空时走数据库过滤（医院/科室/医生名）。 */
    public List<Appointment> searchByPatient(String patientId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByPatient(patientId);
        }
        return appointmentRepository.searchByPatientAndKeyword(patientId, keyword.trim());
    }

    // 查询医生某天的预约
    public List<Appointment> findByDoctorAndDate(String doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date);
    }

    // 核心：创建预约（乐观锁防超卖）
    @Transactional
    public Appointment createAppointment(String patientId, String scheduleId) {
        // 1. 校验患者
        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("患者不存在"));

        // 2. 获取排班
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("号源不存在"));

        // 3. 校验是否可预约
        if (!schedule.hasAvailableSlot()) {
            throw new RuntimeException("号源已满");
        }
        if (schedule.getWorkDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("不能预约过去的日期");
        }

        // 4. 检查是否重复预约
        if (appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDate(
                patientId, schedule.getDoctorId(), schedule.getWorkDate())) {
            throw new RuntimeException("您今天已预约该医生，不能重复预约");
        }

        // 5. 乐观锁扣减号源（核心防超卖）
        int updated = scheduleRepository.incrementBookedCount(scheduleId);
        if (updated == 0) {
            throw new RuntimeException("号源已被抢完，请刷新重试");
        }

        // 6. 创建预约记录
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(UUID.randomUUID().toString().replace("-", ""));
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getWorkDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setStatus(1); // 1=待就诊
        appointment.setCreateTime(LocalDateTime.now());

        return appointmentRepository.save(appointment);
    }

    // 取消预约
    @Transactional
    public void cancelAppointment(String appointmentId, String patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

        // 权限校验：只能取消自己的预约（管理员除外）
        if (!appointment.getPatientId().equals(patientId)) {
            throw new RuntimeException("无权取消他人预约");
        }

        // 业务校验：只能取消待就诊的
        if (appointment.getStatus() != 1) {
            throw new RuntimeException("当前状态不能取消");
        }

        // 提前1天限制
        if (!appointment.canCancel()) {
            throw new RuntimeException("就诊前1天内不能取消");
        }

        // 更新预约状态
        appointment.setStatus(0); // 0=已取消
        appointment.setCancelTime(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // 恢复号源
        scheduleRepository.decrementBookedCount(appointment.getScheduleId());
    }

    // 简化取消预约
    @Transactional
    public void cancelAppointmentSimple(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        appointment.setStatus(0); // 0=已取消
        appointment.setCancelTime(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    // 完成就诊（医生调用）
    @Transactional
    public void completeAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

        if (appointment.getStatus() != 1) {
            throw new RuntimeException("状态异常，无法完成");
        }

        appointment.setStatus(2); // 2=已完成
        appointmentRepository.save(appointment);
    }

    /**
     * 创建预约（控制台使用，简化版）
     */
    @Transactional
    public Appointment create(Appointment appointment) {
        if (appointment.getAppointmentId() == null) {
            appointment.setAppointmentId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (appointment.getStatus() == null) {
            appointment.setStatus(1);
        }
        if (appointment.getCreateTime() == null) {
            appointment.setCreateTime(java.time.LocalDateTime.now());
        }
        return appointmentRepository.save(appointment);
    }

    /**
     * 查询医生的预约（控制台使用）
     */
    public List<Appointment> findByDoctorId(String doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
    }

    /**
     * 查询患者的预约（控制台使用）
     */
    public List<Appointment> findByPatientId(String patientId) {
        return appointmentRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
    }
}