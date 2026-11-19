package com.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.Appointment;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Hospital;
import com.hospital.entity.Patient;
import com.hospital.entity.Schedule;
import com.hospital.entity.TriageLock;
import com.hospital.entity.TriageSession;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.ScheduleRepository;
import com.hospital.repository.TriageLockRepository;
import com.hospital.repository.TriageSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 分诊预约业务：会话生命周期、画像合并、医院/医生/号源匹配、锁号与出单。
 *
 * 设计约束：
 * - 红旗先于一切，命中后不返回任何预约推荐；
 * - 低置信度关闭预约入口，中置信度需用户确认；
 * - 锁号通过 booked_count 原子占位，出单不再重复扣减号源；
 * - 同一患者同时只有一个有效锁，重复锁号幂等返回同一个锁。
 */
@Service
public class TriageService {

    private static final int LOCK_TTL_MINUTES = 10;
    private static final int SEARCH_DAYS = 7;
    /** 分诊会话保留天数：超期未完成的会话标记为 EXPIRED。 */
    private static final int SESSION_RETENTION_DAYS = 30;

    @Autowired
    private TriageSessionRepository sessionRepository;
    @Autowired
    private TriageLockRepository lockRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private TriageRuleEngine ruleEngine;
    @Autowired
    private com.hospital.config.ScheduleRules scheduleRules;
    @Autowired
    private ObjectMapper objectMapper;

    /** 医生未配置挂号费时的兜底价格，可由 hospital.triage.default-fee 覆盖。 */
    @Value("${hospital.triage.default-fee:30}")
    private int defaultFee;

    /** 急救电话，默认 120，可由 hospital.triage.emergency-phone 覆盖。 */
    @Value("${hospital.triage.emergency-phone:120}")
    private String emergencyPhone;

    // ==================== 评估 ====================

    @Transactional
    public Map<String, Object> assess(String patientId, Map<String, Object> body) {
        String symptoms = text(body.get("symptoms"));
        if (symptoms == null) {
            throw new IllegalArgumentException("请先用一句话描述哪里不舒服");
        }
        String mode = text(body.get("mode")) == null ? "STANDARD" : text(body.get("mode")).toUpperCase();
        String subject = text(body.get("subject")) == null ? "SELF" : text(body.get("subject")).toUpperCase();
        if (!"SELF".equals(subject) && !"ELDER".equals(subject)) {
            throw new IllegalArgumentException("subject 仅支持 SELF 或 ELDER");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("就诊人信息不存在，请先完善就诊人信息"));

        TriageSession session = new TriageSession();
        session.setTriageId(newId());
        session.setPatientId(patientId);
        session.setSubject(subject);
        session.setMode("ELDER".equals(mode) ? "ELDER" : "STANDARD");
        session.setRawInput(symptoms);
        session.setRuleVersion(TriageRuleEngine.RULE_VERSION);
        session.setCreateTime(LocalDateTime.now());
        session.setStatus("ASSESSED");

        Integer age = integer(body.get("age"));
        String ageBand = text(body.get("ageBand"));
        String gender = normalizeGender(text(body.get("gender")));
        String pregnant = text(body.get("pregnant"));
        String medicalHistory = text(body.get("medicalHistory"));

        if ("SELF".equals(subject)) {
            if (age == null && patient.getAge() != null && patient.getAge() > 0) {
                age = patient.getAge();
            }
            if (gender == null) {
                gender = normalizeGender(patient.getGender());
            }
            if (medicalHistory == null) {
                medicalHistory = patient.getMedicalHistory();
            }
        }

        session.setAge(age);
        session.setAgeBand(ageBand);
        session.setGender(gender);
        session.setPregnant(pregnant);
        session.setMedicalHistory(medicalHistory);

        Double latitude = decimal(body.get("latitude"));
        Double longitude = decimal(body.get("longitude"));

        evaluate(session, patient, latitude, longitude);
        sessionRepository.save(session);
        return buildResponse(session, patient);
    }

    /** 重新评估：使用会话里已有的原文与画像，可被 profile 补资料后复用。 */
    private void evaluate(TriageSession session, Patient patient, Double latitude, Double longitude) {
        TriageRuleEngine.Result result = ruleEngine.assess(
                session.getRawInput(), session.getAge(), session.getAgeBand(), session.getGender(),
                session.getPregnant(), session.getMedicalHistory());

        session.setNormalizedInput(result.normalized == null || result.normalized.length() <= 512
                ? result.normalized : result.normalized.substring(0, 512));
        session.setRedFlag(result.redFlag ? 1 : 0);
        session.setRedFlagPattern(result.redFlagPattern);
        session.setRedFlagReason(result.redFlagReason);
        session.setConfidence(BigDecimal.valueOf(result.confidence));
        session.setConfidenceTier(result.tier);
        session.setNeedExpert(result.needExpert);
        session.setExpertReason(result.expertReason);
        session.setDeptName(result.redFlag ? null : result.deptName);
        session.setEvidence(toJson(result.evidence));
        session.setAlternatives(null);
        session.setHospitalId(null);
        session.setDoctorId(null);
        session.setScheduleId(null);
        session.setMatchScore(null);
        session.setMatchReason(null);

        Map<String, Object> structured = new LinkedHashMap<>(result.structured);
        structured.put("followUps", result.followUps);
        structured.put("expertScore", result.expertScore);
        structured.put("latitude", latitude);
        structured.put("longitude", longitude);
        structured.put("notices", new ArrayList<String>());
        session.setStructuredSymptoms(toJson(structured));

        if (result.redFlag) {
            session.setStatus("ASSESSED");
            session.setUpdateTime(LocalDateTime.now());
            return;
        }

        boolean missingAge = (session.getAge() == null || session.getAge() <= 0)
                && (session.getAgeBand() == null || session.getAgeBand().isEmpty());
        boolean missingGender = session.getGender() == null || session.getGender().isEmpty();
        if (missingAge || missingGender) {
            session.setStatus("NEED_PROFILE");
            session.setUpdateTime(LocalDateTime.now());
            return;
        }

        session.setStatus("ASSESSED");
        session.setUpdateTime(LocalDateTime.now());
        if ("LOW".equals(result.tier) || result.deptName == null) {
            return;
        }
        Map<String, Object> recommendation = recommend(session, latitude, longitude);
        if (recommendation != null) {
            applyRecommendation(session, recommendation);
        }
    }

    @Transactional
    public Map<String, Object> profile(String patientId, String triageId, Map<String, Object> body) {
        TriageSession session = requireOwnSession(patientId, triageId);
        if ("BOOKED".equals(session.getStatus()) || "LOCKED".equals(session.getStatus())) {
            throw new IllegalArgumentException("号源已为你保留，请先完成支付或等待号源过期");
        }

        Integer age = integer(body.get("age"));
        String ageBand = text(body.get("ageBand"));
        String gender = normalizeGender(text(body.get("gender")));
        String pregnant = text(body.get("pregnant"));

        if (age != null) {
            session.setAge(age);
            session.setAgeBand(null);
        } else if (ageBand != null) {
            session.setAgeBand(ageBand);
            session.setAge(null);
        }
        if (gender != null) {
            session.setGender(gender);
        }
        if (pregnant != null) {
            session.setPregnant(pregnant);
        }

        Patient patient = patientRepository.findById(patientId).orElse(null);
        // 仅本人问诊且档案为空时回填，避免覆盖既有档案或把长辈挂到子女档案
        if (patient != null && "SELF".equals(session.getSubject())) {
            boolean changed = false;
            if ((patient.getAge() == null || patient.getAge() <= 0) && session.getAge() != null) {
                patient.setAge(session.getAge());
                changed = true;
            }
            if ((patient.getGender() == null || patient.getGender().isEmpty()) && session.getGender() != null) {
                patient.setGender(session.getGender());
                changed = true;
            }
            if (changed) {
                patientRepository.save(patient);
            }
        }

        Double latitude = decimal(readStructured(session).get("latitude"));
        Double longitude = decimal(readStructured(session).get("longitude"));
        evaluate(session, patient, latitude, longitude);
        sessionRepository.save(session);
        return buildResponse(session, patient);
    }

    /**
     * 结构化补问：长辈说不清时，用按钮选项补「部位 / 时长 / 伴随症状」，
     * 把选项拼回主诉后重新评估，不要求用户再打一段长文字。
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> clarify(String patientId, String triageId, Map<String, Object> body) {
        TriageSession session = requireOwnSession(patientId, triageId);
        if ("BOOKED".equals(session.getStatus()) || "LOCKED".equals(session.getStatus())) {
            throw new IllegalArgumentException("号源已为你保留，请先完成支付或等待号源过期");
        }
        if (session.getRedFlag() != null && session.getRedFlag() == 1) {
            throw new IllegalArgumentException("红旗症状请立即就医，不能继续预约");
        }

        List<String> answers = new ArrayList<>();
        Object raw = body.get("answers");
        if (raw instanceof List) {
            for (Object item : (List<Object>) raw) {
                String value = text(item);
                if (value != null && !answers.contains(value)) {
                    answers.add(value);
                }
            }
        }
        String custom = text(body.get("text"));
        if (custom != null && !answers.contains(custom)) {
            answers.add(custom);
        }
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("请先选择一个选项");
        }

        String addition = String.join("；", answers);
        String merged = (session.getRawInput() == null ? "" : session.getRawInput()) + "；" + addition;
        if (merged.length() > 500) {
            merged = merged.substring(merged.length() - 500);
        }
        session.setRawInput(merged);

        Patient patient = patientRepository.findById(patientId).orElse(null);
        evaluate(session, patient, latitudeOf(session), longitudeOf(session));

        Map<String, Object> structured = readStructured(session);
        List<String> history = new ArrayList<>();
        Object previous = structured.get("clarifyAnswers");
        if (previous instanceof List) {
            for (Object item : (List<Object>) previous) {
                if (item != null) {
                    history.add(item.toString());
                }
            }
        }
        history.add(addition);
        structured.put("clarifyAnswers", history);
        session.setStructuredSymptoms(toJson(structured));

        sessionRepository.save(session);
        return buildResponse(session, patient);
    }

    @Transactional
    public Map<String, Object> confirm(String patientId, String triageId) {
        TriageSession session = requireOwnSession(patientId, triageId);
        if ("RED_FLAG".equals(session.getConfidenceTier())
                || (session.getRedFlag() != null && session.getRedFlag() == 1)) {
            throw new IllegalArgumentException("红旗症状请立即就医，不能预约门诊");
        }
        if ("NEED_PROFILE".equals(session.getStatus())) {
            throw new IllegalArgumentException("请先补充年龄和性别");
        }
        if ("LOW".equals(session.getConfidenceTier())) {
            throw new IllegalArgumentException("当前信息不足，暂不开放预约");
        }
        if ("BOOKED".equals(session.getStatus())) {
            throw new IllegalArgumentException("该分诊已完成预约，不能重复确认");
        }
        if ("LOCKED".equals(session.getStatus())) {
            throw new IllegalArgumentException("号源已为你保留，请直接支付或等待号源过期");
        }
        if ("REJECTED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
            throw new IllegalArgumentException("该分诊已结束，请重新描述症状");
        }
        if (!"CONFIRMED".equals(session.getStatus())) {
            session.setStatus("CONFIRMED");
            session.setConfirmTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
        }
        return buildResponse(session, patientRepository.findById(patientId).orElse(null));
    }

    @Transactional
    public Map<String, Object> reject(String patientId, String triageId) {
        TriageSession session = requireOwnSession(patientId, triageId);
        if ("BOOKED".equals(session.getStatus())) {
            throw new IllegalArgumentException("已完成预约，如需取消请到「我的预约」退号");
        }
        // 只释放本会话的锁，避免误释放该患者其他会话正在保留的号源
        for (TriageLock lock : lockRepository.findByTriageIdAndStatus(triageId, 1)) {
            releaseLock(lock, "USER_REJECT");
        }
        session.setStatus("REJECTED");
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("triageId", triageId);
        result.put("status", "REJECTED");
        result.put("sessionStatus", session.getStatus());
        result.put("message", "已记录，我们会为你重新判断科室");
        return result;
    }

    // ==================== 锁号与出单 ====================

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> lock(String patientId, String triageId) {
        TriageSession session = requireOwnSession(patientId, triageId);
        Patient patient = patientRepository.findById(patientId).orElse(null);

        // 已出单：不再创建新锁，直接归还已生成的预约，避免重复占号
        if ("BOOKED".equals(session.getStatus())) {
            TriageLock consumed = lockRepository
                    .findFirstByTriageIdAndStatusOrderByCreateTimeDesc(triageId, 2).orElse(null);
            Map<String, Object> booked = new LinkedHashMap<>();
            booked.put("success", true);
            booked.put("alreadyBooked", true);
            booked.put("appointmentId", consumed == null ? null : consumed.getAppointmentId());
            booked.put("message", "该会话已完成预约");
            return booked;
        }

        String block = bookingBlockReason(session, patient);
        if (block != null) {
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", block);
            return fail;
        }

        // 幂等：同一会话已有有效锁，直接返回
        TriageLock existing = lockRepository
                .findFirstByTriageIdAndStatusOrderByCreateTimeDesc(triageId, 1).orElse(null);
        if (existing != null && existing.isActive()) {
            return lockPayload(session, existing, false, null);
        }
        if (existing != null) {
            releaseLock(existing, "EXPIRED");
        }

        // 单患者单有效锁：释放该患者其他会话的锁
        for (TriageLock other : lockRepository.findByPatientIdAndStatus(patientId, 1)) {
            if (!other.getTriageId().equals(triageId)) {
                releaseLock(other, "REBOOK");
            }
        }

        Map<String, Object> recommendation = readRecommendation(session);
        // 若推荐医生当天已有有效预约，清空推荐，强制本次重新推荐（会排除已约医生）
        Map<String, Object> storedRec = (Map<String, Object>) recommendation.get("recommendation");
        if (storedRec != null) {
            String storedDoctorId = text(storedRec.get("doctorId"));
            String storedDate = text(storedRec.get("date"));
            if (storedDoctorId != null && storedDate != null) {
                try {
                    boolean alreadyBooked = appointmentRepository
                            .findByPatientIdAndDoctorIdAndAppointmentDate(patientId, storedDoctorId, LocalDate.parse(storedDate))
                            .stream().anyMatch(a -> a.getStatus() != null && a.getStatus() != 0);
                    if (alreadyBooked) {
                        recommendation = null;
                    }
                } catch (Exception ignored) {
                    // 日期字段异常时按正常推荐处理
                }
            }
        }
        String degraded = null;
        TriageLock created = null;
        for (int attempt = 0; attempt < 3 && created == null; attempt++) {
            if (attempt > 0 || recommendation == null) {
                Map<String, Object> previous = recommendation == null ? null
                        : (Map<String, Object>) recommendation.get("recommendation");
                String preferredHospitalId = previous == null ? null : text(previous.get("hospitalId"));
                recommendation = recommend(session, latitudeOf(session), longitudeOf(session), preferredHospitalId);
                if (recommendation == null) {
                    break;
                }
                applyRecommendation(session, recommendation);
            }
            Map<String, Object> rec = (Map<String, Object>) recommendation.get("recommendation");
            String scheduleId = rec == null ? null : text(rec.get("scheduleId"));
            Schedule schedule = scheduleId == null ? null
                    : scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null || schedule.getStatus() == null || schedule.getStatus() != 1
                    || schedule.getBookedCount() >= schedule.getMaxPatients()
                    || isPastSlot(schedule.getWorkDate(), schedule.getTimeSlot())) {
                continue;
            }
            // 兜底：急诊科号源不参与预约
            Doctor lockDoctor = schedule.getDoctorId() == null ? null
                    : doctorRepository.findById(schedule.getDoctorId()).orElse(null);
            Department lockDept = lockDoctor == null || lockDoctor.getDeptId() == null ? null
                    : departmentRepository.findById(lockDoctor.getDeptId()).orElse(null);
            if (lockDept != null && scheduleRules.isEmergencyDeptName(lockDept.getDeptName())) {
                continue;
            }
            int occupied = scheduleRepository.incrementBookedCount(scheduleId);
            if (occupied == 0) {
                continue;
            }
            created = new TriageLock();
            created.setLockId(newId());
            created.setTriageId(triageId);
            created.setPatientId(patientId);
            created.setScheduleId(scheduleId);
            created.setDoctorId(schedule.getDoctorId());
            created.setHospitalId(schedule.getHospitalId());
            created.setDeptId(rec == null || rec.get("deptId") == null ? null : rec.get("deptId").toString());
            created.setStatus(1);
            created.setExpireAt(LocalDateTime.now().plusMinutes(LOCK_TTL_MINUTES));
            created.setCreateTime(LocalDateTime.now());
            lockRepository.save(created);
            if (attempt > 0) {
                degraded = "原推荐号源已被占用，已为你调整到同科室的其他号源";
            }
        }

        if (created == null) {
            session.setStatus("FAILED");
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", "近 7 天内该科室暂无可用号源，请稍后重试或选择其他医院");
            return fail;
        }

        session.setStatus("LOCKED");
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);
        return lockPayload(session, created, degraded != null, degraded);
    }

    @Transactional
    public Map<String, Object> book(String patientId, String triageId) {
        TriageSession session = requireOwnSession(patientId, triageId);

        if ("BOOKED".equals(session.getStatus())) {
            TriageLock consumed = lockRepository
                    .findFirstByTriageIdAndStatusOrderByCreateTimeDesc(triageId, 2).orElse(null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("appointmentId", consumed == null ? null : consumed.getAppointmentId());
            result.put("alreadyBooked", true);
            return result;
        }

        TriageLock lock = lockRepository
                .findFirstByTriageIdAndStatusOrderByCreateTimeDesc(triageId, 1).orElse(null);
        if (lock == null) {
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", "号源保留已失效，请重新锁定");
            fail.put("needRelock", true);
            return fail;
        }
        if (!lock.isActive()) {
            releaseLock(lock, "EXPIRED");
            session.setStatus("ASSESSED");
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", "号源保留已过期，请重新锁定");
            fail.put("needRelock", true);
            return fail;
        }

        Schedule schedule = scheduleRepository.findById(lock.getScheduleId()).orElse(null);
        if (schedule == null) {
            releaseLock(lock, "SCHEDULE_GONE");
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", "号源已失效，请重新锁定");
            fail.put("needRelock", true);
            return fail;
        }

        List<Appointment> existing = appointmentRepository
                .findByPatientIdAndDoctorIdAndAppointmentDate(patientId, lock.getDoctorId(), schedule.getWorkDate());
        boolean duplicated = existing.stream().anyMatch(a -> a.getStatus() != null && a.getStatus() != 0);
        if (duplicated) {
            releaseLock(lock, "DUPLICATE");
            session.setStatus("ASSESSED");
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("success", false);
            fail.put("message", "您当天已预约该医生，不能重复预约");
            fail.put("needChooseAnother", true);
            return fail;
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(newId());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(lock.getDoctorId());
        appointment.setScheduleId(lock.getScheduleId());
        appointment.setAppointmentDate(schedule.getWorkDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setStatus(1);
        appointment.setCreateTime(LocalDateTime.now());
        appointmentRepository.save(appointment);

        lock.setStatus(2);
        lock.setAppointmentId(appointment.getAppointmentId());
        lock.setReleaseReason("BOOKED");
        lock.setReleaseTime(LocalDateTime.now());
        lockRepository.save(lock);

        session.setStatus("BOOKED");
        session.setBookTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);

        Doctor doctor = doctorRepository.findById(lock.getDoctorId()).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("appointmentId", appointment.getAppointmentId());
        result.put("hospitalId", lock.getHospitalId());
        result.put("doctorId", lock.getDoctorId());
        result.put("doctorName", doctor == null ? null : doctor.getName());
        result.put("doctorTitle", doctor == null ? null : doctor.getTitle());
        result.put("fee", doctor == null || doctor.getConsultationFee() == null ? defaultFee : doctor.getConsultationFee());
        result.put("date", schedule.getWorkDate().toString());
        result.put("dateText", dateText(schedule.getWorkDate()));
        result.put("timeSlot", schedule.getTimeSlot());
        result.put("departmentName", recommendedDeptName(session));
        return result;
    }

    /** 定时任务调用：释放所有超过 TTL 的锁并归还号源。 */
    @Transactional
    public int releaseExpiredLocks() {
        List<TriageLock> expired = lockRepository.findByStatusAndExpireAtBefore(1, LocalDateTime.now());
        for (TriageLock lock : expired) {
            releaseLock(lock, "EXPIRED");
            sessionRepository.findByTriageId(lock.getTriageId()).ifPresent(session -> {
                if ("LOCKED".equals(session.getStatus())) {
                    session.setStatus("ASSESSED");
                    session.setUpdateTime(LocalDateTime.now());
                    sessionRepository.save(session);
                }
            });
        }
        return expired.size();
    }

    /** 定时任务调用：把超过保留期且未完成的分诊会话标记为 EXPIRED。 */
    @Transactional
    public int expireStaleSessions() {
        List<TriageSession> stale = sessionRepository.findByStatusInAndUpdateTimeBefore(
                Arrays.asList("NEED_PROFILE", "ASSESSED", "FAILED", "REJECTED"),
                LocalDateTime.now().minusDays(SESSION_RETENTION_DAYS));
        int expired = 0;
        for (TriageSession session : stale) {
            if ("LOCKED".equals(session.getStatus()) || "BOOKED".equals(session.getStatus())) {
                continue;
            }
            session.setStatus("EXPIRED");
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            expired++;
        }
        return expired;
    }

    public Map<String, Object> detail(String patientId, String triageId, boolean admin) {
        TriageSession session = sessionRepository.findByTriageId(triageId)
                .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));
        if (!admin && !session.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("无权查看他人的分诊记录");
        }
        return buildResponse(session, patientRepository.findById(session.getPatientId()).orElse(null));
    }

    // ==================== 医院/医生/号源匹配 ====================

    private Map<String, Object> recommend(TriageSession session, Double latitude, Double longitude) {
        return recommend(session, latitude, longitude, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> recommend(TriageSession session, Double latitude, Double longitude,
                                          String preferredHospitalId) {
        String dept = session.getDeptName();
        if (dept == null) {
            return null;
        }
        // 急诊科不参与预约：分诊只给急诊信息（医院名称/地址/电话），不推荐号源
        if (scheduleRules.isEmergencyDeptName(dept)) {
            return null;
        }
        boolean expertNeeded = "REQUIRED".equals(session.getNeedExpert())
                || "RECOMMENDED".equals(session.getNeedExpert());

        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(SEARCH_DAYS);
        List<String> deptNames = new ArrayList<>();
        deptNames.add(dept);
        if ("外科".equals(dept)) {
            deptNames.add("普外科");
        } else if ("普外科".equals(dept)) {
            deptNames.add("外科");
        }

        List<Schedule> schedules = new ArrayList<>();
        for (String name : deptNames) {
            schedules.addAll(scheduleRepository.findAvailableForDept(name, null, today, until));
        }
        // 重锁降级链第一级：优先保留同院同科的下一可用号源
        if (preferredHospitalId != null && !preferredHospitalId.isEmpty()) {
            List<Schedule> sameHospital = new ArrayList<>();
            for (Schedule s : schedules) {
                if (preferredHospitalId.equals(s.getHospitalId())) {
                    sameHospital.add(s);
                }
            }
            if (!sameHospital.isEmpty()) {
                schedules = sameHospital;
            }
        }

        Map<String, Hospital> hospitalMap = new HashMap<>();
        for (Hospital h : hospitalRepository.findByStatus(1)) {
            hospitalMap.put(h.getHospitalId(), h);
        }
        Map<String, Doctor> doctorCache = new HashMap<>();
        // 批量加载候选排班涉及的医生，避免在下方循环中逐个查询（N+1）
        Set<String> candidateDoctorIds = new HashSet<>();
        for (Schedule s : schedules) {
            if (s.getDoctorId() != null) {
                candidateDoctorIds.add(s.getDoctorId());
            }
        }
        if (!candidateDoctorIds.isEmpty()) {
            for (Doctor d : doctorRepository.findAllById(candidateDoctorIds)) {
                doctorCache.put(d.getDoctorId(), d);
            }
        }
        // 患者当天已有效预约的「医生+日期」，避免重锁后再次落到同一医生
        Set<String> bookedSlots = new HashSet<>();
        for (Appointment a : appointmentRepository.findByPatientIdOrderByCreateTimeDesc(session.getPatientId())) {
            if (a.getStatus() != null && a.getStatus() != 0 && a.getDoctorId() != null
                    && a.getAppointmentDate() != null) {
                bookedSlots.add(a.getDoctorId() + "|" + a.getAppointmentDate());
            }
        }

        // 同一医院取一个最优号源（专家优先/普通优先 → 日期最早 → 时段最早）
        Map<String, Map<String, Object>> bestByHospital = new LinkedHashMap<>();
        // 医院是否设有目标科室只需判定一次，避免对每条排班都查一次库（N+1 导致数万次查询）
        Map<String, String> deptNameByHospital = new HashMap<>();
        for (Schedule s : schedules) {
            if (isPastSlot(s.getWorkDate(), s.getTimeSlot())) {
                continue;
            }
            if (bookedSlots.contains(s.getDoctorId() + "|" + s.getWorkDate())) {
                continue;
            }
            Hospital hospital = hospitalMap.get(s.getHospitalId());
            if (hospital == null) {
                continue;
            }
            Doctor doctor = doctorCache.get(s.getDoctorId());
            if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
                continue;
            }
            int rank = doctorRank(doctor.getTitle());
            boolean expertMatch = expertNeeded ? rank >= 2 : rank <= 1;

            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("schedule", s);
            candidate.put("doctor", doctor);
            candidate.put("hospital", hospital);
            String deptName = deptNameByHospital.get(s.getHospitalId());
            if (deptName == null && !deptNameByHospital.containsKey(s.getHospitalId())) {
                deptName = resolveDeptName(hospital.getHospitalId(), deptNames);
                deptNameByHospital.put(s.getHospitalId(), deptName);
            }
            candidate.put("deptName", deptName);
            candidate.put("expertMatch", expertMatch);

            Map<String, Object> current = bestByHospital.get(s.getHospitalId());
            if (current == null || better(candidate, current)) {
                bestByHospital.put(s.getHospitalId(), candidate);
            }
        }
        if (bestByHospital.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> candidate : bestByHospital.values()) {
            @SuppressWarnings("unchecked")
            Schedule s = (Schedule) candidate.get("schedule");
            @SuppressWarnings("unchecked")
            Doctor doctor = (Doctor) candidate.get("doctor");
            @SuppressWarnings("unchecked")
            Hospital hospital = (Hospital) candidate.get("hospital");
            String deptName = (String) candidate.get("deptName");
            boolean expertMatch = (Boolean) candidate.get("expertMatch");

            Department department = deptName == null ? null
                    : departmentRepository.findByHospitalIdAndName(hospital.getHospitalId(), deptName).orElse(null);

            double distance = distanceKm(latitude, longitude, hospital.getLatitude(), hospital.getLongitude());
            int distanceScore = distanceScore(distance);
            int specialtyScore = specialtyScore(department, deptName, hospital);
            int fee = doctor.getConsultationFee() == null ? defaultFee : doctor.getConsultationFee();
            int feeScore = feeScore(fee);
            long days = ChronoUnit.DAYS.between(today, s.getWorkDate());
            int availabilityScore = availabilityScore(days);

            int total = specialtyScore + distanceScore + feeScore + availabilityScore;
            int rank = doctorRank(doctor.getTitle());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hospitalId", hospital.getHospitalId());
            item.put("hospitalName", hospital.getHospitalName());
            item.put("level", hospital.getLevel());
            item.put("distanceKm", distance < 0 ? null : Math.round(distance * 10.0) / 10.0);
            item.put("distanceText", distanceText(distance));
            item.put("departmentName", deptName == null ? session.getDeptName() : deptName);
            item.put("deptId", department == null ? null : department.getDeptId());
            item.put("doctorId", doctor.getDoctorId());
            item.put("doctorName", doctor.getName());
            item.put("doctorTitle", doctor.getTitle());
            item.put("expert", rank >= 2);
            item.put("fee", fee);
            item.put("scheduleId", s.getScheduleId());
            item.put("date", s.getWorkDate().toString());
            item.put("dateText", dateText(s.getWorkDate()));
            item.put("timeSlot", s.getTimeSlot());
            item.put("score", total);
            item.put("expertMatch", expertMatch);
            item.put("matchReason", matchReason(hospital, deptName, department, distance, fee, s));
            item.put("_days", days);
            scored.add(item);
        }

        scored.sort((a, b) -> {
            int cmp = Integer.compare((Integer) b.get("score"), (Integer) a.get("score"));
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare((Long) a.get("_days"), (Long) b.get("_days"));
        });

        Map<String, Object> best = scored.get(0);
        List<Map<String, Object>> alternatives = new ArrayList<>();
        for (int i = 1; i < Math.min(scored.size(), 4); i++) {
            Map<String, Object> alt = new LinkedHashMap<>(scored.get(i));
            alt.remove("_days");
            alt.remove("expertMatch");
            alternatives.add(alt);
        }

        List<String> noticeList = new ArrayList<>();
        if (expertNeeded && !((Boolean) best.get("expertMatch"))) {
            noticeList.add("近 7 天内没有专家号，已为你安排普通号医生，就诊时可直接向医生申请转诊");
        }
        if ("REQUIRED".equals(session.getNeedExpert())) {
            noticeList.add("你的情况建议优先看专家，可到院后请医生评估是否需要转诊专家门诊");
        }
        best.put("notices", noticeList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recommendation", best);
        result.put("alternatives", alternatives);
        best.remove("_days");
        return result;
    }

    @SuppressWarnings("unchecked")
    private void applyRecommendation(TriageSession session, Map<String, Object> recommendationBox) {
        Map<String, Object> rec = (Map<String, Object>) recommendationBox.get("recommendation");
        if (rec == null) {
            return;
        }
        session.setHospitalId((String) rec.get("hospitalId"));
        session.setDoctorId((String) rec.get("doctorId"));
        session.setScheduleId((String) rec.get("scheduleId"));
        session.setMatchScore((Integer) rec.get("score"));
        session.setMatchReason((String) rec.get("matchReason"));
        session.setAlternatives(toJson(recommendationBox.get("alternatives")));

        Map<String, Object> structured = readStructured(session);
        structured.put("recommendation", rec);
        structured.put("notices", rec.get("notices"));
        session.setStructuredSymptoms(toJson(structured));
        session.setUpdateTime(LocalDateTime.now());
    }

    private boolean better(Map<String, Object> candidate, Map<String, Object> current) {
        Schedule a = (Schedule) candidate.get("schedule");
        Schedule b = (Schedule) current.get("schedule");
        boolean aMatch = (Boolean) candidate.get("expertMatch");
        boolean bMatch = (Boolean) current.get("expertMatch");
        if (aMatch != bMatch) {
            return aMatch;
        }
        if (!a.getWorkDate().equals(b.getWorkDate())) {
            return a.getWorkDate().isBefore(b.getWorkDate());
        }
        return a.getTimeSlot().compareTo(b.getTimeSlot()) < 0;
    }

    private String resolveDeptName(String hospitalId, List<String> deptNames) {
        for (String name : deptNames) {
            if (departmentRepository.findByHospitalIdAndName(hospitalId, name).isPresent()) {
                return name;
            }
        }
        return null;
    }

    private int specialtyScore(Department department, String deptName, Hospital hospital) {
        if (department != null && department.getFlagshipLevel() != null) {
            switch (department.getFlagshipLevel()) {
                case 1: return 35;
                case 2: return 28;
                case 3: return 22;
                default: break;
            }
            if (department.getIsFlagship() != null && department.getIsFlagship() == 1) {
                return 18;
            }
        }
        if (hospital.getFlagshipDept() != null && deptName != null
                && hospital.getFlagshipDept().contains(deptName)) {
            return 18;
        }
        return 10;
    }


    private int distanceScore(double distanceKm) {
        if (distanceKm < 0) {
            return 12;
        }
        if (distanceKm <= 3) {
            return 25;
        }
        if (distanceKm <= 5) {
            return 21;
        }
        if (distanceKm <= 10) {
            return 15;
        }
        if (distanceKm <= 20) {
            return 8;
        }
        if (distanceKm <= 30) {
            return 4;
        }
        return 0;
    }

    private int feeScore(int fee) {
        if (fee <= 20) {
            return 15;
        }
        if (fee <= 30) {
            return 12;
        }
        if (fee <= 50) {
            return 8;
        }
        if (fee <= 80) {
            return 4;
        }
        return 0;
    }

    private int availabilityScore(long days) {
        if (days <= 0) {
            return 25;
        }
        if (days == 1) {
            return 24;
        }
        if (days <= 3) {
            return 20;
        }
        if (days <= 5) {
            return 14;
        }
        if (days <= 7) {
            return 8;
        }
        return 0;
    }

    /** 医生职称等级：副主任医师及以上为专家。 */
    private int doctorRank(String title) {
        if (title == null || title.isEmpty()) {
            return 0;
        }
        if (title.contains("主任")) {
            return title.contains("副主任") ? 2 : 3;
        }
        if (title.contains("主治")) {
            return 1;
        }
        return 0;
    }

    private String matchReason(Hospital hospital, String deptName, Department department,
                               double distance, int fee, Schedule schedule) {
        List<String> parts = new ArrayList<>();
        if (hospital.getLevel() != null && !hospital.getLevel().isEmpty()) {
            parts.add(hospital.getLevel());
        }
        String deptText = deptName == null ? "相关科室" : deptName;
        if (department != null && department.getFlagshipLevel() != null && department.getFlagshipLevel() > 0) {
            String[] levels = {"", "国家级重点专科", "省级重点专科", "市级重点专科"};
            int lv = Math.min(department.getFlagshipLevel(), 3);
            deptText = deptText + "（" + levels[lv] + "）";
        } else if (department != null && department.getIsFlagship() != null && department.getIsFlagship() == 1) {
            deptText = deptText + "（院级重点专科）";
        }
        parts.add(deptText);
        if (distance >= 0) {
            parts.add(distanceText(distance));
        }
        parts.add(dateText(schedule.getWorkDate()) + " " + schedule.getTimeSlot());
        parts.add("挂号费 ¥" + fee);
        return String.join(" · ", parts);
    }

    private double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return -1;
        }
        double rad = Math.PI / 180.0;
        double dLat = (lat2 - lat1) * rad;
        double dLng = (lng2 - lng1) * rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * rad) * Math.cos(lat2 * rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String distanceText(double distanceKm) {
        if (distanceKm < 0) {
            return "距离未知";
        }
        if (distanceKm < 1) {
            return "约 " + (int) (distanceKm * 1000) + " 米";
        }
        return "约 " + (Math.round(distanceKm * 10.0) / 10.0) + " 公里";
    }

    private String dateText(LocalDate date) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, date);
        if (days == 0) {
            return "今天";
        }
        if (days == 1) {
            return "明天";
        }
        if (days == 2) {
            return "后天";
        }
        return (date.getMonthValue()) + "月" + date.getDayOfMonth() + "日";
    }

    private boolean isPastSlot(LocalDate date, String timeSlot) {
        if (date == null) {
            return true;
        }
        if (date.isBefore(LocalDate.now())) {
            return true;
        }
        if (!date.equals(LocalDate.now())) {
            return false;
        }
        LocalTime start = startTime(timeSlot);
        return start != null && LocalTime.now().isAfter(start);
    }

    private LocalTime startTime(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains("-")) {
            return null;
        }
        try {
            String start = timeSlot.split("-")[0].trim();
            if (start.contains(":")) {
                String[] p = start.split(":");
                int hour = Integer.parseInt(p[0].trim());
                int minute = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
                return LocalTime.of(hour, minute);
            }
            return LocalTime.of(Integer.parseInt(start), 0);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 锁号辅助 ====================

    private void releaseLock(TriageLock lock, String reason) {
        if (lock.getStatus() != null && lock.getStatus() == 1) {
            lock.setStatus(0);
            lock.setReleaseReason(reason);
            lock.setReleaseTime(LocalDateTime.now());
            if (lock.getScheduleId() != null) {
                scheduleRepository.decrementBookedCount(lock.getScheduleId());
            }
            lockRepository.save(lock);
        }
    }

    @SuppressWarnings("unchecked")
    private String recommendedDeptName(TriageSession session) {
        Map<String, Object> structured = readStructured(session);
        Object rec = structured.get("recommendation");
        if (rec instanceof Map) {
            Object name = ((Map<String, Object>) rec).get("departmentName");
            if (name != null && !name.toString().isEmpty()) {
                return name.toString();
            }
        }
        return session.getDeptName();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lockPayload(TriageSession session, TriageLock lock,
                                            boolean degraded, String degradeReason) {
        Schedule schedule = scheduleRepository.findById(lock.getScheduleId()).orElse(null);
        Doctor doctor = doctorRepository.findById(lock.getDoctorId()).orElse(null);
        Hospital hospital = hospitalRepository.findById(lock.getHospitalId()).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("lockId", lock.getLockId());
        result.put("triageId", lock.getTriageId());
        result.put("expireAt", lock.getExpireAt() == null ? null
                : lock.getExpireAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("ttlSeconds", 600);
        result.put("scheduleId", lock.getScheduleId());
        result.put("doctorId", lock.getDoctorId());
        result.put("hospitalId", lock.getHospitalId());
        result.put("hospitalName", hospital == null ? null : hospital.getHospitalName());
        result.put("hospitalPhone", hospital == null ? null : hospital.getPhone());
        result.put("departmentName", recommendedDeptName(session));
        result.put("doctorName", doctor == null ? null : doctor.getName());
        result.put("doctorTitle", doctor == null ? null : doctor.getTitle());
        result.put("date", schedule == null ? null : schedule.getWorkDate().toString());
        result.put("dateText", schedule == null ? null : dateText(schedule.getWorkDate()));
        result.put("timeSlot", schedule == null ? null : schedule.getTimeSlot());
        result.put("fee", doctor == null || doctor.getConsultationFee() == null ? defaultFee : doctor.getConsultationFee());
        result.put("degraded", degraded);
        result.put("degradeReason", degradeReason);
        result.put("expert", doctor != null && doctorRank(doctor.getTitle()) >= 2);
        return result;
    }

    // ==================== 响应组装 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildResponse(TriageSession session, Patient patient) {
        Map<String, Object> structured = readStructured(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("triageId", session.getTriageId());
        result.put("status", effectiveStatus(session));
        result.put("sessionStatus", session.getStatus());
        result.put("confidence", session.getConfidence() == null ? null : session.getConfidence().doubleValue());
        result.put("confidenceTier", session.getConfidenceTier());
        result.put("tierLabel", tierLabel(session.getConfidenceTier()));
        result.put("redFlag", redFlagPayload(session));
        result.put("department", departmentPayload(session));
        result.put("needExpert", session.getNeedExpert());
        result.put("expertNeeded", "REQUIRED".equals(session.getNeedExpert())
                || "RECOMMENDED".equals(session.getNeedExpert()));
        result.put("expertReason", session.getExpertReason());
        result.put("expertLabel", expertLabel(session.getNeedExpert()));
        result.put("evidence", parseJson(session.getEvidence()));
        result.put("followUpQuestions", structured.get("followUps") == null
                ? new ArrayList<>() : structured.get("followUps"));
        result.put("clarify", clarifyPayload(session, structured));

        boolean needProfile = "NEED_PROFILE".equals(session.getStatus());
        result.put("needProfile", needProfile);
        result.put("missingFields", missingFields(session, structured));
        result.put("profileCompleteness", profileCompleteness(patient));

        Map<String, Object> recommendation = recommendationForResponse(session, patient);
        result.put("recommendation", recommendation);
        result.put("alternatives", parseJson(session.getAlternatives()));

        List<Object> notices = new ArrayList<>();
        Object storedNotices = structured.get("notices");
        if (storedNotices instanceof List) {
            notices.addAll((List<Object>) storedNotices);
        }
        boolean judged = "HIGH".equals(session.getConfidenceTier()) || "MEDIUM".equals(session.getConfidenceTier());
        if (recommendation == null && judged && session.getDeptName() != null
                && !"RED_FLAG".equals(effectiveStatus(session))) {
            notices.add("近 7 天「" + session.getDeptName() + "」暂无可预约号源，可稍后重试或更换医院");
        }
        result.put("notices", notices);

        String block = bookingBlockReason(session, patient);
        if (block == null && recommendation == null && judged) {
            block = "近 7 天「" + session.getDeptName() + "」暂无可预约号源";
        }
        result.put("bookingAllowed", block == null && recommendation != null);
        result.put("bookingBlockedReason", block);
        result.put("emergency", emergencyPayload(session));
        result.put("copy", copyPayload(session, recommendation));
        result.put("createdAt", session.getCreateTime() == null ? null
                : session.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result;
    }

    private String effectiveStatus(TriageSession session) {
        if ("NEED_PROFILE".equals(session.getStatus())) {
            return "NEED_PROFILE";
        }
        if (session.getRedFlag() != null && session.getRedFlag() == 1) {
            return "RED_FLAG";
        }
        return "OK";
    }

    private Map<String, Object> redFlagPayload(TriageSession session) {
        if (session.getRedFlag() == null || session.getRedFlag() != 1) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pattern", session.getRedFlagPattern());
        map.put("reason", session.getRedFlagReason());
        map.put("advice", "请立即拨打 120 或前往最近医院急诊科，不要自行驾车");
        return map;
    }

    private Map<String, Object> departmentPayload(TriageSession session) {
        if (session.getDeptName() == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", session.getDeptName());
        return map;
    }

    private Map<String, Object> emergencyPayload(TriageSession session) {
        if (session.getRedFlag() == null || session.getRedFlag() != 1) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("phone", emergencyPhone);
        map.put("hospitalName", null);
        map.put("hospitalAddress", null);
        map.put("hospitalPhone", null);

        Double latitude = decimal(readStructured(session).get("latitude"));
        Double longitude = decimal(readStructured(session).get("longitude"));
        if (latitude != null && longitude != null) {
            Hospital nearest = null;
            double best = Double.MAX_VALUE;
            for (Hospital hospital : hospitalRepository.findByStatus(1)) {
                if (!filled(hospital.getPhone())) {
                    continue;
                }
                double distance = distanceKm(latitude, longitude, hospital.getLatitude(), hospital.getLongitude());
                if (distance >= 0 && distance < best) {
                    best = distance;
                    nearest = hospital;
                }
            }
            if (nearest != null) {
                map.put("hospitalId", nearest.getHospitalId());
                map.put("hospitalName", nearest.getHospitalName());
                map.put("hospitalAddress", nearest.getAddress());
                map.put("hospitalPhone", nearest.getPhone());
                map.put("hospitalDistanceKm", Math.round(best * 10) / 10.0);
            }
        }
        return map;
    }

    private List<Map<String, Object>> missingFields(TriageSession session, Map<String, Object> structured) {
        List<Map<String, Object>> list = new ArrayList<>();
        boolean missingAge = (session.getAge() == null || session.getAge() <= 0)
                && (session.getAgeBand() == null || session.getAgeBand().isEmpty());
        if (missingAge) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("key", "age");
            field.put("label", "年龄");
            List<Map<String, Object>> options = new ArrayList<>();
            options.add(option("儿童（0-14岁）", "CHILD"));
            options.add(option("15-44岁", "YOUNG"));
            options.add(option("45-64岁", "MIDDLE"));
            options.add(option("65岁及以上", "ELDER"));
            field.put("options", options);
            list.add(field);
        }
        if (session.getGender() == null || session.getGender().isEmpty()) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("key", "gender");
            field.put("label", "性别");
            List<Map<String, Object>> options = new ArrayList<>();
            options.add(option("男", "MALE"));
            options.add(option("女", "FEMALE"));
            field.put("options", options);
            list.add(field);
        }
        if (ruleEngine.needsPregnancyQuestion(session.getRawInput(), session.getAge(), session.getAgeBand(),
                session.getGender(), session.getPregnant())) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("key", "pregnant");
            field.put("label", "是否怀孕");
            List<Map<String, Object>> options = new ArrayList<>();
            options.add(option("已怀孕", "YES"));
            options.add(option("未怀孕", "NO"));
            field.put("options", options);
            list.add(field);
        }
        return list;
    }

    private Map<String, Object> option(String label, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", label);
        map.put("value", value);
        return map;
    }

    private Map<String, Object> profileCompleteness(Patient patient) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        int filled = 0;
        int total = 6;
        if (patient == null) {
            map.put("percent", 0);
            map.put("bookingMissing", new ArrayList<>());
            map.put("note", "就诊人档案不存在，请先创建");
            return map;
        }
        filled += filled(patient.getName()) ? 1 : 0;
        filled += filled(patient.getGender()) ? 1 : 0;
        filled += (patient.getAge() != null && patient.getAge() > 0) ? 1 : 0;
        filled += filled(patient.getPhone()) ? 1 : 0;
        filled += filled(patient.getIdCard()) ? 1 : 0;
        filled += filled(patient.getMedicalHistory()) ? 1 : 0;

        if (!filled(patient.getName())) {
            missing.add("name");
        }
        if (!filled(patient.getPhone())) {
            missing.add("phone");
        }
        map.put("percent", (int) Math.round(filled * 100.0 / total));
        map.put("bookingMissing", missing);
        map.put("note", missing.isEmpty()
                ? "就诊人信息已满足挂号要求"
                : "姓名或手机号缺失，挂号前需要先在「就诊人信息」补齐");
        return map;
    }

    private boolean filled(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> recommendationForResponse(TriageSession session, Patient patient) {
        Map<String, Object> structured = readStructured(session);
        Map<String, Object> rec = (Map<String, Object>) structured.get("recommendation");
        if (rec == null) {
            return null;
        }
        if (!"HIGH".equals(session.getConfidenceTier()) && !"MEDIUM".equals(session.getConfidenceTier())) {
            return null;
        }
        if ("REJECTED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
            return null;
        }
        return rec;
    }

    private String bookingBlockReason(TriageSession session, Patient patient) {
        if (session.getRedFlag() != null && session.getRedFlag() == 1) {
            return "红旗症状请立即就医，不能预约门诊";
        }
        if ("NEED_PROFILE".equals(session.getStatus())) {
            return "请先补充年龄和性别";
        }
        if ("LOW".equals(session.getConfidenceTier())) {
            return "信息不足，暂不开放预约，请补充症状或转人工";
        }
        if ("REJECTED".equals(session.getStatus())) {
            return "已取消该科室建议，请重新判断";
        }
        if ("EXPIRED".equals(session.getStatus())) {
            return "分诊记录已过期，请重新描述症状";
        }
        if (patient == null) {
            return "就诊人档案不存在";
        }
        if (!filled(patient.getName()) || !filled(patient.getPhone())) {
            return "请先在「就诊人信息」补全姓名和手机号";
        }
        if ("ELDER".equals(session.getSubject()) && patient.getAge() != null && patient.getAge() > 0
                && session.getAge() != null
                && Math.abs(patient.getAge() - session.getAge()) >= 20) {
            return "所填年龄与账号本人档案差异较大，请用长辈本人账号或由家人协助挂号";
        }
        return null;
    }

    /**
     * 仅对 LOW 且已具备年龄性别时给出结构化追问；按钮选项降低长辈表达负担。
     */
    private Map<String, Object> clarifyPayload(TriageSession session, Map<String, Object> structured) {
        if (!"LOW".equals(session.getConfidenceTier()) || "NEED_PROFILE".equals(session.getStatus())
                || (session.getRedFlag() != null && session.getRedFlag() == 1)) {
            return null;
        }
        List<Map<String, Object>> questions = new ArrayList<>();
        boolean duration = Boolean.TRUE.equals(structured.get("duration"));
        boolean accompanying = Boolean.TRUE.equals(structured.get("accompanying"));
        int keywordCount = structured.get("keywordCount") instanceof Number
                ? ((Number) structured.get("keywordCount")).intValue() : 0;

        if (!duration) {
            questions.add(question("duration", "这样不舒服多久了？", new String[]{
                    "就今天", "两三天了", "一周以上", "好长时间了"}));
        }
        if (keywordCount < 2) {
            questions.add(question("focus", "哪里最不舒服？", new String[]{
                    "头疼头晕", "胸口闷", "肚子疼或胀", "腰腿疼", "嗓子疼或鼻塞", "皮肤痒"}));
        }
        if (!accompanying) {
            questions.add(question("other", "还有别的症状吗？", new String[]{
                    "有点发热", "恶心想吐", "没有别的", "没力气"}));
        }
        if (questions.isEmpty()) {
            questions.add(question("detail", "能再说具体一点吗？", new String[]{
                    "心慌胸闷", "反酸或肚子胀", "头晕或睡不着", "皮肤痒或起疹子"}));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("questions", questions);
        payload.put("hint", "点一下最接近的选项，就能继续判断");
        return payload;
    }

    private Map<String, Object> question(String key, String questionText, String[] options) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("question", questionText);
        List<String> list = new ArrayList<>();
        java.util.Collections.addAll(list, options);
        map.put("options", list);
        return map;
    }

    private Map<String, Object> copyPayload(TriageSession session, Map<String, Object> recommendation) {
        Map<String, Object> copy = new LinkedHashMap<>();
        String tier = session.getConfidenceTier() == null ? "" : session.getConfidenceTier();
        String status = effectiveStatus(session);
        String dept = session.getDeptName() == null ? "相关科室" : session.getDeptName();

        if ("RED_FLAG".equals(status)) {
            copy.put("title", "请立即就医");
            copy.put("subtitle", session.getRedFlagReason());
            copy.put("primary", "拨打 120");
            copy.put("secondary", "查看急诊信息");
            copy.put("lowHint", "不要等待，也不要自行驾车");
            return copy;
        }
        if ("NEED_PROFILE".equals(status)) {
            copy.put("title", "还差一点信息");
            copy.put("subtitle", "补充年龄和性别后马上帮你判断");
            copy.put("primary", "继续");
            copy.put("secondary", null);
            copy.put("lowHint", "这些信息用于判断科室和用药禁忌");
            return copy;
        }
        if (("HIGH".equals(tier) || "MEDIUM".equals(tier)) && recommendation == null) {
            copy.put("title", "暂时没有号源");
            copy.put("subtitle", "已判断为" + dept + "，但近 7 天暂无可预约号源");
            copy.put("primary", "重新判断");
            copy.put("secondary", "不是这个科室");
            copy.put("lowHint", "可以稍后重试，或到院咨询现场号");
            return copy;
        }
        if ("HIGH".equals(tier)) {
            copy.put("title", "已经判断好了");
            copy.put("subtitle", "根据你的描述，建议到" + dept + "就诊");
            copy.put("primary", "去付款");
            copy.put("secondary", "不是这个科室");
            copy.put("lowHint", null);
            return copy;
        }
        if ("MEDIUM".equals(tier)) {
            copy.put("title", "还需要确认一下");
            copy.put("subtitle", "根据你的描述，更像是" + dept + "的问题");
            copy.put("primary", "继续挂号");
            copy.put("secondary", "不是这个科室");
            copy.put("lowHint", "也可以补充症状后再判断");
            return copy;
        }
        copy.put("title", "还需要再问几句");
        copy.put("subtitle", "目前信息还不足以判断科室");
        copy.put("primary", "重新判断");
        copy.put("secondary", "转人工");
        copy.put("lowHint", "先补充下面几个问题，判断会更准");
        return copy;
    }

    private String tierLabel(String tier) {
        if (tier == null) {
            return "";
        }
        switch (tier) {
            case "HIGH": return "判断明确";
            case "MEDIUM": return "需要确认";
            case "LOW": return "信息不足";
            case "RED_FLAG": return "紧急";
            default: return "";
        }
    }

    private String expertLabel(String needExpert) {
        if ("REQUIRED".equals(needExpert)) {
            return "建议专家号";
        }
        if ("RECOMMENDED".equals(needExpert)) {
            return "可以考虑专家号";
        }
        return "普通号即可";
    }

    // ==================== JSON / 基础工具 ====================

    private Map<String, Object> readStructured(TriageSession session) {
        Map<String, Object> parsed = parseObject(session.getStructuredSymptoms());
        return parsed == null ? new LinkedHashMap<>() : parsed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRecommendation(TriageSession session) {
        Map<String, Object> structured = readStructured(session);
        Object rec = structured.get("recommendation");
        Map<String, Object> recommendation = new LinkedHashMap<>();
        if (rec instanceof Map) {
            recommendation.put("recommendation", rec);
            recommendation.put("alternatives", parseJson(session.getAlternatives()));
        }
        return rec instanceof Map ? recommendation : null;
    }

    private Double latitudeOf(TriageSession session) {
        return decimal(readStructured(session).get("latitude"));
    }

    private Double longitudeOf(TriageSession session) {
        return decimal(readStructured(session).get("longitude"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return value == null ? new ArrayList<>() : value;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return value instanceof Map ? (Map<String, Object>) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private TriageSession requireOwnSession(String patientId, String triageId) {
        TriageSession session = sessionRepository.findByTriageId(triageId)
                .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));
        if (!session.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("无权操作他人的分诊记录");
        }
        return session;
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Integer integer(Object value) {
        if (value == null) {
            return null;
        }
        try {
            int v = Integer.parseInt(value.toString().trim());
            return v > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Double decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null) {
            return null;
        }
        String g = gender.trim().toUpperCase();
        if ("男".equals(gender) || "M".equals(g) || "MALE".equals(g)) {
            return "MALE";
        }
        if ("女".equals(gender) || "F".equals(g) || "FEMALE".equals(g)) {
            return "FEMALE";
        }
        return null;
    }
}
