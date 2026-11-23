package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Hospital;
import com.hospital.entity.Patient;
import com.hospital.entity.Schedule;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.ScheduleRepository;
import com.hospital.security.AuthContext;
import com.hospital.service.AppointmentService;
import com.hospital.service.ScheduleService;
import com.hospital.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private com.hospital.service.DemoMockService demoMockService;

    @Autowired
    private com.hospital.service.VoucherService voucherService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private com.hospital.repository.PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private com.hospital.config.ScheduleRules scheduleRules;

    /** 患者本人或管理员才可操作该预约。 */
    private void requireAppointmentAccess(Appointment appointment, String action) {
        String role = authContext.role();
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("PATIENT".equals(role)) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(appointment.getPatientId())) {
                throw new RuntimeException("无权" + action + "他人预约");
            }
            return;
        }
        throw new RuntimeException("当前账号无权" + action + "预约");
    }

    /** 患者只能查询自己的预约。 */
    private void requirePatientScope(String patientId) {
        String role = authContext.role();
        if ("ADMIN".equals(role) || "DOCTOR".equals(role)) {
            return;
        }
        String ownerId = authContext.ownerId();
        if (ownerId == null || !ownerId.equals(patientId)) {
            throw new RuntimeException("无权查看其他患者的预约");
        }
    }

    /** 医生只能查看自己的预约明细。 */
    private void requireDoctorScope(String doctorId) {
        if ("DOCTOR".equals(authContext.role())) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(doctorId)) {
                throw new RuntimeException("无权查看其他医生的预约");
            }
        }
    }

    // 查询我的预约（小程序用，通过 openid）
    @GetMapping
    public List<Map<String, Object>> getMyAppointmentsByOpenid(
            @RequestHeader(value = "X-Openid", required = false) String openid,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        List<Appointment> appointments;
        Object role = httpRequest.getAttribute("auth.role");
        String ownerId = authContext.ownerId();
        if ("PATIENT".equals(role) && ownerId != null && !ownerId.isEmpty()) {
            // 患者身份以登录令牌归属为准，忽略客户端传入的 openid，避免越权查询他人预约
            appointments = appointmentRepository.findByPatientIdOrderByCreateTimeDesc(ownerId);
        } else if (openid != null && !openid.isEmpty()) {
            appointments = appointmentRepository.findByOpenidOrderByCreateTimeDesc(openid);
        } else {
            // 未带 openid 时只有管理员/医生可以看全量，匿名请求返回空，避免患者信息泄露
            if (role == null) {
                return new ArrayList<>();
            }
            appointments = appointmentRepository.findAll();
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 检查并更新状态，填充关联数据
        for (Appointment app : appointments) {
            // 检查并更新状态
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
            }
            
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("appointmentId", app.getAppointmentId());
            appMap.put("scheduleId", app.getScheduleId());
            appMap.put("doctorId", app.getDoctorId());
            appMap.put("appointmentDate", app.getAppointmentDate());
            appMap.put("timeSlot", app.getTimeSlot());
            appMap.put("status", app.getStatus());
            appMap.put("createTime", app.getCreateTime());
            appMap.put("cancelTime", app.getCancelTime());
            appMap.put("questionnaireData", app.getQuestionnaireData());
            
            // 扁平化填充信息
            appMap.put("hospitalName", "");
            appMap.put("deptName", "");
            appMap.put("doctorName", "");
            appMap.put("doctorTitle", "");
            appMap.put("patientId", "");
            
            // 获取患者信息
            Patient patient = patientRepository.findById(app.getPatientId()).orElse(null);
            if (patient != null) {
                appMap.put("patientId", patient.getPatientId());
            }
            
            // 获取医生信息
            Doctor doctor = doctorRepository.findById(app.getDoctorId()).orElse(null);
            if (doctor != null) {
                appMap.put("doctorName", doctor.getName());
                appMap.put("doctorTitle", doctor.getTitle());
                
                // 获取医院信息
                if (doctor.getHospitalId() != null) {
                    Hospital hospital = hospitalRepository.findById(doctor.getHospitalId()).orElse(null);
                    if (hospital != null) {
                        appMap.put("hospitalName", hospital.getHospitalName());
                    }
                }
                
                // 获取科室信息
                if (doctor.getDeptId() != null) {
                    Department dept = departmentRepository.findById(doctor.getDeptId()).orElse(null);
                    if (dept != null && dept.getDeptName() != null && !dept.getDeptName().isEmpty()) {
                        appMap.put("deptName", dept.getDeptName());
                    }
                }
            }
            
            result.add(appMap);
        }
        return result;
    }

    // 查询我的预约（不分页）
    @GetMapping("/my/{patientId}")
    public List<Map<String, Object>> getMyAppointments(@PathVariable String patientId) {
        requirePatientScope(patientId);
        System.out.println("查询患者预约，patientId: " + patientId);
        List<Appointment> appointments = appointmentService.findByPatient(patientId);
        System.out.println("找到预约数量: " + appointments.size());
        return convertAppointmentsToMapList(appointments);
    }
    
    // 查询我的预约（分页版，支持状态分类筛选）
    @GetMapping("/my/{patientId}/page")
    public Map<String, Object> getMyAppointmentsWithPage(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean needPay,
            @RequestParam(required = false) String keyword) {
        
        requirePatientScope(patientId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Appointment> allAppointments = appointmentService.searchByPatient(patientId, keyword);
        
        // 检查并更新预约状态
        for (Appointment app : allAppointments) {
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
            }
        }

        // 服务端分类筛选：unpaid/upcoming/completed/cancelled；也支持 status + needPay 组合
        java.util.Set<String> paidIds = paidAppointmentIds(allAppointments);
        String normalizedFilter = filter == null || filter.isEmpty() ? "all" : filter.toLowerCase();
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment app : allAppointments) {
            int st = app.getStatus() == null ? 0 : app.getStatus();
            boolean paid = paidIds.contains(app.getAppointmentId());
            boolean keep;
            switch (normalizedFilter) {
                case "unpaid" -> keep = st == 1 && !paid;
                case "upcoming" -> keep = st == 1 && paid;
                case "completed" -> keep = st == 2;
                case "cancelled", "canceled" -> keep = st == 0;
                default -> {
                    keep = true;
                    if (status != null) {
                        keep = st == status;
                    }
                    if (keep && needPay != null) {
                        keep = needPay == (st == 1 && !paid);
                    }
                }
            }
            if (keep) {
                filtered.add(app);
            }
        }
        allAppointments = filtered;
        
        // 稳定排序：创建时间倒序，同时间按 appointmentId 倒序
        allAppointments.sort(Comparator
                .comparing(Appointment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Appointment::getAppointmentId, Comparator.nullsLast(Comparator.reverseOrder())));
        
        // 分页处理
        int total = allAppointments.size();
        int totalPages = (int) Math.ceil((double) total / safeSize);
        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, total);
        
        List<Appointment> pageAppointments = start < total ? allAppointments.subList(start, end) : new ArrayList<>();
        
        // 转换为Map列表
        List<Map<String, Object>> data = convertAppointmentsToMapList(pageAppointments);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filter", normalizedFilter);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("currentPage", safePage);
        result.put("pageSize", safeSize);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("hasMore", (long) (safePage + 1) * safeSize < total);
        result.put("data", data);
        return result;
    }
    
    // 将预约列表转换为Map列表（复用方法）
    private List<Map<String, Object>> convertAppointmentsToMapList(List<Appointment> appointments) {
        // 一次批量查缴费记录，避免逐条 N+1；无记录/未支付视为待付款
        java.util.Set<String> paidIds = paidAppointmentIds(appointments);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Appointment app : appointments) {
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("appointmentId", app.getAppointmentId());
            appMap.put("scheduleId", app.getScheduleId());
            appMap.put("doctorId", app.getDoctorId());
            appMap.put("appointmentDate", app.getAppointmentDate());
            appMap.put("timeSlot", app.getTimeSlot());
            appMap.put("status", app.getStatus());
            appMap.put("createTime", app.getCreateTime());
            appMap.put("cancelTime", app.getCancelTime());
            appMap.put("questionnaireData", app.getQuestionnaireData());
            
            // 扁平化填充信息
            appMap.put("hospitalName", "");
            appMap.put("deptName", "");
            appMap.put("doctorName", "");
            appMap.put("doctorTitle", "");
            appMap.put("patientName", "");
            
            // 获取患者信息
            Patient patient = patientRepository.findById(app.getPatientId()).orElse(null);
            if (patient != null) {
                appMap.put("patientId", patient.getPatientId());
                appMap.put("patientName", patient.getName());
                appMap.put("patientPhone", patient.getPhone());
            }
            
            // 获取医生信息
            Doctor doctor = doctorRepository.findById(app.getDoctorId()).orElse(null);
            if (doctor != null) {
                appMap.put("doctorName", doctor.getName());
                appMap.put("doctorTitle", doctor.getTitle());
                
                // 获取医院信息
                if (doctor.getHospitalId() != null) {
                    Hospital hospital = hospitalRepository.findById(doctor.getHospitalId()).orElse(null);
                    if (hospital != null) {
                        appMap.put("hospitalName", hospital.getHospitalName());
                    }
                }
                
                // 获取科室信息
                if (doctor.getDeptId() != null) {
                    Department dept = departmentRepository.findById(doctor.getDeptId()).orElse(null);
                    if (dept != null && dept.getDeptName() != null && !dept.getDeptName().isEmpty()) {
                        appMap.put("deptName", dept.getDeptName());
                    }
                }
            }
            
            putAppointmentPresentation(appMap, app, doctor, paidIds.contains(app.getAppointmentId()));
            result.add(appMap);
        }
        
        return result;
    }

    /** 预约列表/详情的展示字段：状态文案、流程节点、报到时限、缴费状态、排队号/费用拆分（mock）。 */
    /** 患者获取就诊凭证（带签名、一次性）。 */
    @PostMapping("/{appointmentId}/voucher")
    public Map<String, Object> issueVoucher(@PathVariable String appointmentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                result.put("success", false);
                result.put("code", "APPOINTMENT_NOT_FOUND");
                result.put("message", "预约不存在");
                return result;
            }
            requireAppointmentAccess(appointment, "获取就诊凭证");
            if (!voucherService.canIssue(appointment)) {
                result.put("success", false);
                result.put("code", VoucherService.CODE_STATUS_INVALID);
                result.put("message", "当前预约不可签发凭证：仅待就诊且未过期的预约可签发");
                return result;
            }
            String voucher = voucherService.issue(appointment);
            if (voucher == null) {
                result.put("success", false);
                result.put("code", "VOUCHER_CONFIG_ERROR");
                result.put("message", "凭证签发不可用：未配置 hospital.voucher.secret");
                return result;
            }
            result.put("success", true);
            result.put("voucher", voucher);
            result.put("appointmentId", appointment.getAppointmentId());
            result.put("ttlMinutes", voucherService.getTtlMinutes());
            result.put("expiresAt", LocalDateTime.now().plusMinutes(voucherService.getTtlMinutes())
                    .toString());
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生验码报到：只凭凭证串即可完成，无需预约ID。 */
    @PostMapping("/voucher/checkin")
    public Map<String, Object> checkin(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String voucher = request == null ? null : request.get("voucher");
            VoucherService.Payload payload = voucherService.verify(voucher);
            if (voucherService.isUsed(payload.getNonce())) {
                throw new VoucherService.VoucherException(VoucherService.CODE_USED, "该凭证已被使用");
            }
            Appointment appointment = appointmentRepository.findById(payload.getAppointmentId()).orElse(null);
            if (appointment == null) {
                throw new VoucherService.VoucherException(VoucherService.CODE_STATUS_INVALID, "预约不存在或已失效");
            }

            String appointmentHospitalId = scheduleRepository.findById(appointment.getScheduleId())
                    .map(Schedule::getHospitalId)
                    .orElse(null);
            if (appointmentHospitalId == null && appointment.getDoctorId() != null) {
                appointmentHospitalId = doctorRepository.findById(appointment.getDoctorId())
                        .map(Doctor::getHospitalId).orElse(null);
            }
            // 医生所属医院以医生档案为准（医生登录令牌不携带 hospitalId）
            String doctorHospitalId = doctorRepository.findById(authContext.ownerId())
                    .map(Doctor::getHospitalId).orElse(null);
            if (doctorHospitalId == null) {
                doctorHospitalId = authContext.hospitalId();
            }
            if (appointmentHospitalId == null || doctorHospitalId == null
                    || !appointmentHospitalId.equals(doctorHospitalId)) {
                throw new VoucherService.VoucherException(VoucherService.CODE_WRONG_HOSPITAL,
                        "非本院预约凭证，无法在本院报到");
            }

            if (appointment.getStatus() == null || appointment.getStatus() != 1
                    || appointment.getAppointmentDate() == null
                    || appointment.getAppointmentDate().isBefore(LocalDate.now())) {
                throw new VoucherService.VoucherException(VoucherService.CODE_STATUS_INVALID,
                        "预约状态不可报到（已取消、已完成或已过就诊日期）");
            }

            voucherService.consume(payload, authContext.ownerId());

            long checkedIn = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatus(
                    appointment.getDoctorId(), appointment.getAppointmentDate(), 3);
            String queueNo = "Q" + String.format("%03d", checkedIn + 1);
            appointment.setStatus(3);
            appointment.setQueueNo(queueNo);
            appointment.setCheckinTime(LocalDateTime.now());
            appointmentRepository.save(appointment);

            result.put("success", true);
            result.put("message", "报到成功");
            result.put("appointmentId", appointment.getAppointmentId());
            result.put("patientId", appointment.getPatientId());
            result.put("doctorId", appointment.getDoctorId());
            result.put("appointmentDate", appointment.getAppointmentDate());
            result.put("timeSlot", appointment.getTimeSlot());
            result.put("status", appointment.getStatus());
            result.put("queueNo", appointment.getQueueNo());
            result.put("checkinTime", appointment.getCheckinTime());
        } catch (VoucherService.VoucherException e) {
            result.put("success", false);
            result.put("code", e.getCode());
            result.put("message", e.getMessage());
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("code", "VOUCHER_ERROR");
            result.put("message", e.getMessage());
        }
        return result;
    }

    private void putAppointmentPresentation(Map<String, Object> appMap, Appointment app, Doctor doctor, boolean paid) {
        int status = app.getStatus() == null ? 0 : app.getStatus();
        String statusText;
        int processNode;
        String processText;
        boolean needPay = false;
        if (status == 0) {
            statusText = "已取消";
            processNode = 0;
            processText = "已取消";
        } else if (status == 2) {
            statusText = "已完成";
            processNode = 5;
            processText = "已完成";
        } else if (status == 3) {
            statusText = "进行中";
            processNode = 4;
            processText = "就诊中";
        } else if (!paid) {
            // status=1 且无有效缴费记录：前端四分类归入“待付款”
            statusText = "待付款";
            processNode = 2;
            processText = "待付款";
            needPay = true;
        } else {
            statusText = "待就诊";
            LocalTime start = parseSlotStart(app.getTimeSlot());
            LocalDate today = LocalDate.now();
            boolean started = start != null && app.getAppointmentDate() != null
                    && app.getAppointmentDate().equals(today) && !LocalTime.now().isBefore(start);
            processNode = started ? 4 : 3;
            processText = started ? "候诊中" : "待报到";
        }
        appMap.put("statusText", statusText);
        appMap.put("processNode", processNode);
        appMap.put("processText", "当前：" + processText);
        appMap.put("payStatus", paid ? "PAID" : "UNPAID");
        appMap.put("needPay", needPay);
        if (parseSlotStart(app.getTimeSlot()) != null) {
            appMap.put("checkinBefore", parseSlotStart(app.getTimeSlot()).minusMinutes(15)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        appMap.put("cancelPolicy", "就诊前 2 小时可免费取消");

        int fee = doctor != null && doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 30;
        Map<String, Object> feeDetail = new HashMap<>();
        feeDetail.put("total", fee);
        List<String> mockFields = new ArrayList<>();
        if (demoMockService.isEnabled()) {
            int insurance = (int) Math.floor(fee * 0.5);
            feeDetail.put("insurance", insurance);
            feeDetail.put("selfPay", fee - insurance);
            mockFields.add("feeDetail.insurance");
        }
        appMap.put("feeDetail", feeDetail);

        Map<String, Object> demo = new HashMap<>();
        demoMockService.putAppointmentExtras(demo, app.getAppointmentId());
        appMap.putAll(demo);
        if (app.getQueueNo() != null) {
            appMap.put("queueNo", app.getQueueNo());
            mockFields.remove("queueNo");
        }
        appMap.put("checkinTime", app.getCheckinTime());
        if (demoMockService.isEnabled()) {
            mockFields.addAll(List.of("location", "smsSentTo"));
        }
        appMap.put("_mock", mockFields);
    }

    /** 批量查询已支付成功的预约；一条 SQL 完成，避免逐条查缴费记录。 */
    private java.util.Set<String> paidAppointmentIds(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return new java.util.HashSet<>();
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Appointment app : appointments) {
            if (app.getAppointmentId() != null) {
                ids.add(app.getAppointmentId());
            }
        }
        java.util.Set<String> paid = new java.util.HashSet<>();
        for (com.hospital.entity.PaymentRecord record : paymentRecordRepository.findByAppointmentIdIn(ids)) {
            // status=1 为支付成功；其他状态不算已缴费
            if (record.getStatus() != null && record.getStatus() == 1) {
                paid.add(record.getAppointmentId());
            }
        }
        return paid;
    }

    private LocalTime parseSlotStart(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains(":")) {
            return null;
        }
        try {
            String start = timeSlot.contains("-") ? timeSlot.substring(0, timeSlot.indexOf('-')) : timeSlot;
            return LocalTime.parse(start);
        } catch (Exception e) {
            return null;
        }
    }

    // 获取单个预约详情
    @GetMapping("/{appointmentId}")
    public Map<String, Object> getAppointmentDetail(@PathVariable String appointmentId) {
        Map<String, Object> result = new HashMap<>();
        
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            result.put("success", false);
            result.put("message", "预约不存在");
            return result;
        }

        requireAppointmentAccess(appointment, "查看");
        
        // 更新状态
        Integer oldStatus = appointment.getStatus();
        appointment.checkAndUpdateStatus();
        if (oldStatus != appointment.getStatus()) {
            appointmentRepository.save(appointment);
        }
        
        result.put("success", true);
        result.put("appointmentId", appointment.getAppointmentId());
        result.put("patientId", appointment.getPatientId());
        result.put("doctorId", appointment.getDoctorId());
        result.put("appointmentDate", appointment.getAppointmentDate());
        result.put("timeSlot", appointment.getTimeSlot());
        result.put("status", appointment.getStatus());
        result.put("questionnaireData", appointment.getQuestionnaireData());
        result.put("createTime", appointment.getCreateTime());
        
        // 填充患者信息（只返回展示所需字段，避免密码等敏感字段外泄）
        if (appointment.getPatientId() != null) {
            Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
            if (patient != null) {
                Map<String, Object> patientMap = new HashMap<>();
                patientMap.put("patientId", patient.getPatientId());
                patientMap.put("name", patient.getName());
                patientMap.put("gender", patient.getGender());
                patientMap.put("age", patient.getAge());
                patientMap.put("phone", patient.getPhone());
                patientMap.put("medicalHistory", patient.getMedicalHistory());
                result.put("patient", patientMap);
            }
        }
        
        // 填充医生信息（同样只输出展示字段）
        if (appointment.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
            if (doctor != null) {
                Map<String, Object> doctorMap = new HashMap<>();
                doctorMap.put("doctorId", doctor.getDoctorId());
                doctorMap.put("name", doctor.getName());
                doctorMap.put("title", doctor.getTitle());
                doctorMap.put("hospitalId", doctor.getHospitalId());
                doctorMap.put("deptId", doctor.getDeptId());
                doctorMap.put("expertise", doctor.getExpertise());
                doctorMap.put("consultationFee", doctor.getConsultationFee());

                if (doctor.getDeptId() != null) {
                    Department dept = departmentRepository.findById(doctor.getDeptId()).orElse(null);
                    if (dept != null) {
                        Map<String, Object> deptMap = new HashMap<>();
                        deptMap.put("deptId", dept.getDeptId());
                        deptMap.put("deptName", dept.getDeptName());
                        deptMap.put("hospitalId", dept.getHospitalId());
                        doctorMap.put("department", deptMap);
                    }
                }
                result.put("doctor", doctorMap);
            }
        }
        
        Doctor detailDoctor = appointment.getDoctorId() == null ? null
                : doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        boolean paid = paymentRecordRepository.findByAppointmentId(appointmentId)
                .map(record -> record.getStatus() != null && record.getStatus() == 1)
                .orElse(false);
        putAppointmentPresentation(result, appointment, detailDoctor, paid);
        result.put("qrCode", voucherService.issue(appointment));
        if (demoMockService.isEnabled()) {
            result.put("guide", demoMockService.text("凭就诊卡/身份证到门诊 1 层自助机取号，提前 15 分钟到诊区报到"));
            result.put("hospitalPhones", Map.of(
                    "switchboard", "029-8763 1000",
                    "guideDesk", "029-8763 1126",
                    "emergency", "029-8763 1999"));
        }
        return result;
    }

    // 查询医生某天的预约（医生/管理员用）
    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> getByDoctorAndDate(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        requireDoctorScope(doctorId);
        List<Appointment> appointments = appointmentService.findByDoctorAndDate(doctorId, date);
        // 检查并更新状态
        for (Appointment app : appointments) {
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
            }
        }
        return appointments;
    }

    // 根据排班ID查询预约
    @GetMapping("/schedule/{scheduleId}")
    public List<Map<String, Object>> getAppointmentsByScheduleId(@PathVariable String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("排班不存在"));
        if ("DOCTOR".equals(authContext.role())) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(schedule.getDoctorId())) {
                throw new RuntimeException("无权查看其他医生的预约明细");
            }
        }
        authContext.requireHospitalScope(schedule.getHospitalId());

        List<Appointment> appointments = appointmentRepository.findByScheduleId(scheduleId);
        // 检查并更新状态；返回扁平化的展示字段，避免直接暴露患者实体
        for (Appointment app : appointments) {
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
            }
        }
        return convertAppointmentsToMapList(appointments);
    }

    // 根据医生用户名查询预约（医生登录后用）
    @GetMapping("/doctor/by-username/{username}")
    public List<Map<String, Object>> getAppointmentsByDoctorUsername(@PathVariable String username) {
        System.out.println("查询医生预约，username: " + username);
        
        // 先通过用户名找到医生
        Doctor doctor = doctorRepository.findByUsername(username).orElse(null);
        if (doctor == null) {
            System.out.println("医生不存在");
            throw new RuntimeException("医生不存在");
        }
        System.out.println("找到医生: " + doctor.getName() + ", doctorId: " + doctor.getDoctorId());
        
        // 医生只能查自己的预约明细
        requireDoctorScope(doctor.getDoctorId());

        // 查询该医生的所有预约
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getDoctorId());
        System.out.println("预约数量: " + appointments.size());
        
        // 检查并更新预约状态
        for (Appointment app : appointments) {
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
                System.out.println("更新预约状态: " + app.getAppointmentId() + " 从 " + oldStatus + " 变为 " + app.getStatus());
            }
        }
        
        // 转换为Map列表返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment app : appointments) {
            Map<String, Object> map = new HashMap<>();
            map.put("appointmentId", app.getAppointmentId());
            map.put("patientId", app.getPatientId());
            map.put("doctorId", app.getDoctorId());
            map.put("doctorName", doctor.getName());
            map.put("doctorTitle", doctor.getTitle());
            map.put("deptId", doctor.getDeptId());
            map.put("hospitalId", doctor.getHospitalId());
            map.put("appointmentDate", app.getAppointmentDate());
            map.put("timeSlot", app.getTimeSlot());
            map.put("status", app.getStatus());
            map.put("questionnaireData", app.getQuestionnaireData());

            // 医生信息对象，供小程序科室/医院展示使用
            Map<String, Object> doctorMap = new HashMap<>();
            doctorMap.put("doctorId", doctor.getDoctorId());
            doctorMap.put("name", doctor.getName());
            doctorMap.put("title", doctor.getTitle());
            doctorMap.put("deptId", doctor.getDeptId());
            doctorMap.put("hospitalId", doctor.getHospitalId());

            if (doctor.getHospitalId() != null) {
                Hospital hospital = hospitalRepository.findById(doctor.getHospitalId()).orElse(null);
                if (hospital != null) {
                    map.put("hospitalName", hospital.getHospitalName());
                    doctorMap.put("hospitalName", hospital.getHospitalName());
                }
            }
            if (doctor.getDeptId() != null) {
                Department dept = departmentRepository.findById(doctor.getDeptId()).orElse(null);
                if (dept != null) {
                    map.put("deptName", dept.getDeptName());
                    doctorMap.put("deptName", dept.getDeptName());
                }
            }
            map.put("doctor", doctorMap);

            // 报到后列表需要展示排队号与报到时间
            if (app.getQueueNo() != null) {
                map.put("queueNo", app.getQueueNo());
            }
            map.put("checkinTime", app.getCheckinTime());
            
            // 获取患者信息 - 返回 patient 对象格式，匹配前端期望
            if (app.getPatientId() != null) {
                Patient patient = patientRepository.findById(app.getPatientId()).orElse(null);
                if (patient != null) {
                    Map<String, Object> patientMap = new HashMap<>();
                    patientMap.put("patientId", patient.getPatientId());
                    patientMap.put("name", patient.getName());
                    patientMap.put("phone", patient.getPhone());
                    patientMap.put("gender", patient.getGender());
                    patientMap.put("age", patient.getAge());
                    map.put("patient", patientMap);
                }
            }
            
            result.add(map);
        }
        
        System.out.println("返回预约数量: " + result.size());
        return result;
    }

    // 创建预约（小程序端用，简化版）
    @PostMapping
    public Map<String, Object> createSimple(@RequestHeader(value = "X-Openid", required = false) String openid,
                                    @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        String patientId = (String) request.get("patientId");
        String doctorId = (String) request.get("doctorId");
        String scheduleId = (String) request.get("scheduleId");
        String appointmentDate = (String) request.get("appointmentDate");
        String timeSlot = (String) request.get("timeSlot");
        String questionnaireData = (String) request.get("questionnaireData");
        String patientName = (String) request.get("patientName");
        String patientPhone = (String) request.get("patientPhone");
        
        System.out.println("=== 预约请求 ===");
        System.out.println("patientId: " + patientId);
        System.out.println("doctorId: " + doctorId);
        System.out.println("scheduleId: " + scheduleId);
        System.out.println("appointmentDate: " + appointmentDate);
        System.out.println("timeSlot: " + timeSlot);
        System.out.println("patientName: " + patientName);
        System.out.println("patientPhone: " + patientPhone);
        
        // 从请求体获取openid作为备选
        if (openid == null || openid.isEmpty()) {
            openid = (String) request.get("openid");
        }
        // 如果还是没有，使用默认值
        if (openid == null || openid.isEmpty()) {
            openid = "default_openid";
        }

        try {
            // 患者身份以登录令牌为准：患者角色不能替他人挂号
            if ("PATIENT".equals(authContext.role())) {
                String ownerId = authContext.ownerId();
                if (ownerId == null || ownerId.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "登录状态异常，请重新登录");
                    return result;
                }
                patientId = ownerId;
            }

            // 验证患者（必须为系统中已存在的就诊人）
            if (patientId == null || patientId.isEmpty()) {
                result.put("success", false);
                result.put("message", "患者ID不能为空");
                return result;
            }
            if (!patientRepository.existsById(patientId)) {
                result.put("success", false);
                result.put("message", "就诊人信息不存在，请先完善就诊人信息");
                return result;
            }
            if (doctorId == null || doctorId.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择就诊医生");
                return result;
            }
            if (appointmentDate == null || appointmentDate.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择就诊日期");
                return result;
            }
            if (timeSlot == null || timeSlot.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择就诊时段");
                return result;
            }
            
            // 验证日期格式
            LocalDate workDate;
            try {
                workDate = LocalDate.parse(appointmentDate);
            } catch (Exception e) {
                System.out.println("日期格式错误: " + appointmentDate);
                result.put("success", false);
                result.put("message", "日期格式错误，应为yyyy-MM-dd格式");
                return result;
            }
            
            // 验证医生
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor == null) {
                System.out.println("医生不存在: " + doctorId);
                result.put("success", false);
                result.put("message", "医生不存在");
                return result;
            }
            System.out.println("找到医生: " + doctor.getName());

            // 急诊科不参与预约：只提供医院名称/地址/电话，不能生成挂号单
            Department doctorDept = doctor.getDeptId() == null ? null
                    : departmentRepository.findById(doctor.getDeptId()).orElse(null);
            if (doctorDept != null && scheduleRules.isEmergencyDeptName(doctorDept.getDeptName())) {
                result.put("success", false);
                result.put("message", "急诊科不提供预约挂号，请直接前往急诊或拨打 120");
                return result;
            }

            Schedule schedule = null;
            
            // 尝试查找排班
            if (scheduleId != null && !scheduleId.isEmpty()) {
                schedule = scheduleRepository.findById(scheduleId).orElse(null);
                if (schedule != null) {
                    System.out.println("通过scheduleId找到排班: " + schedule.getTimeSlot());
                }
            }
            
            // 如果没有提供scheduleId或没找到，根据医生ID和日期查找对应的排班
            if (schedule == null) {
                List<Schedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
                System.out.println("找到医生" + doctorId + "在" + appointmentDate + "的排班数: " + schedules.size());
                
                // 找到包含该时间段的排班（支持子时段选择）
                for (Schedule s : schedules) {
                    String scheduleTimeSlot = s.getTimeSlot();
                    System.out.println("检查排班时段: " + scheduleTimeSlot);
                    
                    if (scheduleTimeSlot != null && timeSlot != null) {
                        // 检查预约时段是否在排班时段范围内
                        if (isTimeSlotInRange(timeSlot, scheduleTimeSlot)) {
                            schedule = s;
                            scheduleId = s.getScheduleId();
                            System.out.println("找到包含该时段的排班: " + scheduleId);
                            break;
                        }
                    }
                }
            }
            
            // 如果还是没找到排班
            if (schedule == null) {
                System.out.println("未找到匹配的排班");
                result.put("success", false);
                result.put("message", "该医生在选择的日期和时间没有排班");
                return result;
            }

            // 排班必须与所选医生、日期、时段完全对应，避免拿其他号源抵扣
            if (!doctorId.equals(schedule.getDoctorId())
                    || !workDate.equals(schedule.getWorkDate())
                    || !isTimeSlotInRange(timeSlot, schedule.getTimeSlot())) {
                result.put("success", false);
                result.put("message", "排班信息与所选医生、日期或时段不一致，请刷新后重试");
                return result;
            }

            // 停用的号源不可预约
            if (schedule.getStatus() == null || schedule.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "该号源已停用，请选择其他时段");
                return result;
            }

            // 当天已开始/已过去的时段不可预约
            if (workDate.equals(LocalDate.now())) {
                String startTime = normalizeTime(timeSlot.split("-")[0]);
                if (LocalTime.now().isAfter(LocalTime.parse(startTime))) {
                    result.put("success", false);
                    result.put("message", "该时段已过，请选择后面的时段");
                    return result;
                }
            }
            
            // 检查人数限制
            if (schedule.getBookedCount() >= schedule.getMaxPatients()) {
                System.out.println("该时段已约满: " + schedule.getTimeSlot() + 
                                   " 已约: " + schedule.getBookedCount() + 
                                   " 最大: " + schedule.getMaxPatients());
                result.put("success", false);
                result.put("message", "该时段已约满");
                return result;
            }
            
            // 检查是否重复预约（同一天同一医生；已取消的不算重复）
            List<Appointment> existingAppointments = appointmentRepository
                .findByPatientIdAndDoctorIdAndAppointmentDate(patientId, doctorId, workDate);
            boolean duplicated = existingAppointments.stream()
                .anyMatch(a -> a.getStatus() != null && a.getStatus() != 0);
            if (duplicated) {
                System.out.println("患者" + patientId + "已在" + appointmentDate + "预约了医生" + doctorId);
                result.put("success", false);
                result.put("message", "您当天已预约该医生，不能重复预约");
                return result;
            }

            // 创建预约记录
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(UUID.randomUUID().toString().replace("-", ""));
            appointment.setOpenid(openid);
            appointment.setPatientId(patientId);
            appointment.setDoctorId(doctorId);
            appointment.setScheduleId(scheduleId);
            appointment.setAppointmentDate(workDate);
            appointment.setTimeSlot(timeSlot);
            appointment.setQuestionnaireData(questionnaireData);
            appointment.setStatus(1); // 1=待就诊
            appointment.setCreateTime(java.time.LocalDateTime.now());

            // 使用数据库乐观锁更新号源（通过Service层的事务方法）
            int updated = scheduleService.decrementAvailableSlot(scheduleId);
            if (updated == 0) {
                System.out.println("预约失败，号源已被抢完");
                result.put("success", false);
                result.put("message", "预约失败，号源已被抢完");
                return result;
            }
            System.out.println("排班已更新，号源扣减成功");

            // 保存到数据库
            Appointment saved = appointmentRepository.save(appointment);
            System.out.println("预约创建成功: " + saved.getAppointmentId());

            result.put("success", true);
            result.put("appointmentId", saved.getAppointmentId());
            result.put("data", saved);
            
        } catch (Exception e) {
            System.out.println("预约失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    // 标准化时间格式为24小时制（统一格式便于比较）
    private String normalizeTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return timeStr;
        }
        
        timeStr = timeStr.trim().toLowerCase();
        
        // 处理12小时制格式：8am, 8:20am, 2pm, 2:20pm
        if (timeStr.contains("am") || timeStr.contains("pm")) {
            boolean isPM = timeStr.contains("pm");
            timeStr = timeStr.replace("am", "").replace("pm", "").trim();
            
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            
            if (isPM && hour < 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                hour = 0;
            }
            
            return String.format("%02d:%02d", hour, minute);
        }
        
        // 处理24小时制格式：8:00, 08:00, 14:00
        String[] parts = timeStr.split(":");
        if (parts.length == 1) {
            return String.format("%02d:00", Integer.parseInt(timeStr.trim()));
        } else {
            return String.format("%02d:%02d", 
                Integer.parseInt(parts[0].trim()), 
                Integer.parseInt(parts[1].trim()));
        }
    }

    // 检查时间段是否在排班范围内（支持多种时间格式）
    private boolean isTimeSlotInRange(String timeSlot, String scheduleRange) {
        if (timeSlot == null || scheduleRange == null) {
            return false;
        }
        
        try {
            String[] slotParts = timeSlot.split("-");
            String[] rangeParts = scheduleRange.split("-");
            
            if (slotParts.length != 2 || rangeParts.length != 2) {
                return false;
            }
            
            // 标准化时间格式
            String slotStart = normalizeTime(slotParts[0]);
            String slotEnd = normalizeTime(slotParts[1]);
            String rangeStart = normalizeTime(rangeParts[0]);
            String rangeEnd = normalizeTime(rangeParts[1]);
            
            // 比较时间
            return slotStart.compareTo(rangeStart) >= 0 && slotEnd.compareTo(rangeEnd) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 取消预约（小程序端用）
    @PostMapping("/{appointmentId}/cancel")
    public Map<String, Object> cancel(@PathVariable String appointmentId,
                       @RequestHeader(value = "X-Openid", required = false) String openid) {
        Map<String, Object> result = new HashMap<>();
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("预约不存在"));
            requireAppointmentAccess(appointment, "取消");
            // 走完整取消逻辑：校验状态与提前 1 天限制，并回收号源
            appointmentService.cancelAppointment(appointmentId, appointment.getPatientId());
            result.put("success", true);
            result.put("message", "预约已取消");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // 取消预约（Web端用，DELETE方式）
    @DeleteMapping("/{appointmentId}")
    public Map<String, Object> cancelByDelete(@PathVariable String appointmentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("预约不存在"));
            requireAppointmentAccess(appointment, "取消");
            appointmentService.cancelAppointment(appointmentId, appointment.getPatientId());
            result.put("success", true);
            result.put("message", "预约取消成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 完成就诊（医生调用）
    @PostMapping("/{appointmentId}/complete")
    public Map<String, Object> complete(@PathVariable String appointmentId) {
        Map<String, Object> result = new HashMap<>();
        String role = authContext.role();
        if (!"ADMIN".equals(role) && !"DOCTOR".equals(role)) {
            result.put("success", false);
            result.put("message", "仅医生或管理员可完成就诊");
            return result;
        }
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            result.put("success", false);
            result.put("message", "预约不存在");
            return result;
        }
        if ("DOCTOR".equals(role)) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(appointment.getDoctorId())) {
                result.put("success", false);
                result.put("message", "无权完成其他医生的预约");
                return result;
            }
        }
        authContext.requireHospitalScope(doctorRepository.findById(appointment.getDoctorId())
                .map(Doctor::getHospitalId).orElse(null));
        if (appointment.getStatus() == null
                || appointment.getStatus() == 0 || appointment.getStatus() == 2) {
            result.put("success", false);
            result.put("message", "该预约当前状态不可完成");
            return result;
        }
        appointmentService.completeAppointment(appointmentId);
        result.put("success", true);
        return result;
    }

    // ==================== 管理员接口 ====================

    // 管理员：获取所有预约
    @GetMapping("/admin/all")
    public List<Appointment> getAllAppointmentsForAdmin() {
        List<Appointment> all = appointmentRepository.findAll();
        List<Appointment> appointments = new ArrayList<>();
        // 院管只能看到本院医生的预约，卫健委总管理员可见全部
        String scopedHospitalId = authContext.isHospitalAdmin() ? authContext.hospitalId() : null;
        for (Appointment app : all) {
            Doctor doctor = app.getDoctorId() == null ? null
                    : doctorRepository.findById(app.getDoctorId()).orElse(null);
            if (scopedHospitalId != null) {
                if (doctor == null || !scopedHospitalId.equals(doctor.getHospitalId())) {
                    continue;
                }
            }
            appointments.add(app);

            // 检查并更新状态
            Integer oldStatus = app.getStatus();
            app.checkAndUpdateStatus();
            if (oldStatus != app.getStatus()) {
                appointmentRepository.save(app);
            }

            // 填充关联数据
            if (doctor != null) {
                app.setDoctor(doctor);
                if (doctor.getDeptId() != null) {
                    Department dept = departmentRepository.findById(doctor.getDeptId()).orElse(null);
                    if (dept != null) {
                        doctor.setDepartment(dept);
                    }
                }
            }
        }
        return appointments;
    }

    // 管理员：更新预约状态
    @PostMapping("/admin/{appointmentId}/status")
    public Appointment updateAppointmentStatus(@PathVariable String appointmentId,
                                        @RequestBody Map<String, Object> request) {
        Integer status = (Integer) request.get("status");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        authContext.requireHospitalScope(doctorRepository.findById(appointment.getDoctorId())
                .map(Doctor::getHospitalId).orElse(null));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }
}