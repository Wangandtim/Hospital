package com.hospital.service;

import com.hospital.config.ScheduleRules;
import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.DoctorScheduleSetting;
import com.hospital.entity.Schedule;
import com.hospital.entity.ScheduleWeeklyConfirm;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.DoctorScheduleSettingRepository;
import com.hospital.repository.ScheduleRepository;
import com.hospital.repository.ScheduleWeeklyConfirmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.logging.Logger;

/**
 * 医生排班「周确认」：周日提醒下周排班，未确认则自动沿用上一周。
 */
@Service
public class ScheduleWeeklyConfirmService {

    private static final Logger logger = Logger.getLogger(ScheduleWeeklyConfirmService.class.getName());

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_INHERITED = "inherited";

    private static final String[] DAY_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ScheduleWeeklyConfirmRepository confirmRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleSettingRepository settingRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ScheduleRules scheduleRules;

    /** 急诊科只做信息展示，不参与预约，自动沿用排班时直接跳过。 */
    private boolean isEmergencyDoctor(Doctor doctor) {
        if (doctor == null || doctor.getDeptId() == null) {
            return false;
        }
        return departmentRepository.findById(doctor.getDeptId())
                .map(Department::getName)
                .map(scheduleRules::isEmergencyDeptName)
                .orElse(false);
    }

    /** 即将开始的一周（下周一）。 */
    public static LocalDate upcomingWeekStart(LocalDate today) {
        return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /** 若下周确认记录不存在则创建待确认记录（打开页面/定时提醒共用）。 */
    @Transactional
    public ScheduleWeeklyConfirm ensurePendingConfirm(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        LocalDate weekStart = upcomingWeekStart(LocalDate.now());
        Optional<ScheduleWeeklyConfirm> existing =
                confirmRepository.findByDoctorIdAndWeekStart(doctorId, weekStart);
        if (existing.isPresent()) {
            return existing.get();
        }
        ScheduleWeeklyConfirm record = new ScheduleWeeklyConfirm();
        record.setConfirmId("SWC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        record.setDoctorId(doctorId);
        record.setWeekStart(weekStart);
        record.setStatus(STATUS_PENDING);
        record.setRemindTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        logger.info("生成下周排班确认提醒：" + doctor.getName() + " (" + doctorId + ") 周起始 " + weekStart);
        return confirmRepository.save(record);
    }

    /** 医生端查询下周排班待确认状态与预览。 */
    @Transactional
    public Map<String, Object> getPendingInfo(String doctorId) {
        ScheduleWeeklyConfirm record = ensurePendingConfirm(doctorId);
        LocalDate weekStart = record.getWeekStart();
        Plan plan = buildPlan(doctorId, weekStart);

        Map<String, Object> data = new HashMap<>();
        data.put("confirmId", record.getConfirmId());
        data.put("doctorId", doctorId);
        data.put("weekStart", weekStart.toString());
        data.put("weekEnd", weekStart.plusDays(6).toString());
        data.put("weekLabel", weekLabel(weekStart));
        data.put("status", record.getStatus());
        data.put("pending", STATUS_PENDING.equals(record.getStatus()));
        data.put("remindTime", record.getRemindTime() != null ? record.getRemindTime().toString() : null);
        data.put("source", plan.source);
        data.put("days", buildDayViews(plan, weekStart));
        return data;
    }

    /** 医生确认：inherit=沿用上周，modify=需要修改（后续自行调整模板）。 */
    @Transactional
    public Map<String, Object> confirm(String doctorId, String action) {
        if (!"inherit".equals(action) && !"modify".equals(action)) {
            throw new RuntimeException("不支持的操作");
        }
        ScheduleWeeklyConfirm record = ensurePendingConfirm(doctorId);

        Map<String, Object> result = new HashMap<>();
        if (!STATUS_PENDING.equals(record.getStatus())) {
            result.put("status", record.getStatus());
            result.put("generated", 0);
            result.put("skipped", 0);
            result.put("message", "下周排班已确认");
            return result;
        }

        int generated = 0;
        int skipped = 0;
        if ("inherit".equals(action)) {
            ApplyResult apply = inheritSchedules(doctorId, record.getWeekStart());
            generated = apply.generated;
            skipped = apply.skipped;
        }

        record.setStatus(STATUS_CONFIRMED);
        record.setConfirmTime(LocalDateTime.now());
        confirmRepository.save(record);

        result.put("status", record.getStatus());
        result.put("generated", generated);
        result.put("skipped", skipped);
        result.put("message", "inherit".equals(action) ? "已沿用上周排班" : "已标记需要修改");
        return result;
    }

    /** 周日 20:00 为所有在岗医生生成下周待确认提醒。 */
    @Transactional
    public int createRemindersForAllDoctors() {
        LocalDate weekStart = upcomingWeekStart(LocalDate.now());
        int created = 0;
        for (Doctor doctor : doctorRepository.findAll()) {
            if (doctor.getStatus() != null && doctor.getStatus() != 1) {
                continue;
            }
            // 急诊科不参与预约，不生成周确认提醒
            if (isEmergencyDoctor(doctor)) {
                continue;
            }
            if (confirmRepository.existsByDoctorIdAndWeekStart(doctor.getDoctorId(), weekStart)) {
                continue;
            }
            ScheduleWeeklyConfirm record = new ScheduleWeeklyConfirm();
            record.setConfirmId("SWC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            record.setDoctorId(doctor.getDoctorId());
            record.setWeekStart(weekStart);
            record.setStatus(STATUS_PENDING);
            record.setRemindTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            confirmRepository.save(record);
            created++;
        }
        logger.info("下周排班提醒生成完成，周起始 " + weekStart + "，新增 " + created + " 条");
        return created;
    }

    /** 超时未确认的记录自动沿用上一周排班。 */
    @Transactional
    public int autoInheritExpiredPending() {
        LocalDate today = LocalDate.now();
        List<ScheduleWeeklyConfirm> expired =
                confirmRepository.findByStatusAndWeekStartLessThanEqual(STATUS_PENDING, today);
        int inherited = 0;
        for (ScheduleWeeklyConfirm record : expired) {
            ApplyResult apply = inheritSchedules(record.getDoctorId(), record.getWeekStart());
            record.setStatus(STATUS_INHERITED);
            record.setConfirmTime(LocalDateTime.now());
            confirmRepository.save(record);
            inherited++;
            logger.info("自动沿用下周排班：" + record.getDoctorId() + " 周起始 " + record.getWeekStart()
                    + "，生成 " + apply.generated + " 个时段");
        }
        return inherited;
    }

    /** 将上一周排班（无则用排班模板）复制到目标周，已有排班不覆盖。 */
    @Transactional
    public ApplyResult inheritSchedules(String doctorId, LocalDate weekStart) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        if (isEmergencyDoctor(doctor)) {
            ApplyResult skippedResult = new ApplyResult();
            skippedResult.source = "emergency-skip";
            return skippedResult;
        }
        Plan plan = buildPlan(doctorId, weekStart);
        int generated = 0;
        int skipped = 0;
        for (PlanSlot slot : plan.slots) {
            Optional<Schedule> existing = scheduleRepository
                    .findByHospitalIdAndDoctorIdAndWorkDateAndTimeSlot(
                            doctor.getHospitalId(), doctorId, slot.date, slot.timeSlot);
            if (existing.isPresent()) {
                skipped++;
                continue;
            }
            Schedule schedule = new Schedule();
            schedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
            schedule.setHospitalId(doctor.getHospitalId());
            schedule.setDoctorId(doctorId);
            schedule.setWorkDate(slot.date);
            schedule.setTimeSlot(slot.timeSlot);
            schedule.setMaxPatients(slot.maxPatients);
            schedule.setBookedCount(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
            generated++;
        }
        ApplyResult result = new ApplyResult();
        result.generated = generated;
        result.skipped = skipped;
        result.source = plan.source;
        return result;
    }

    /** 生成下周排班计划：优先沿用上一周实际排班，否则回退到医生排班模板。 */
    private Plan buildPlan(String doctorId, LocalDate weekStart) {
        // 急诊科不参与预约，预览与沿用都不产生号源
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (isEmergencyDoctor(doctor)) {
            return new Plan(new ArrayList<>(), "emergency-skip");
        }
        LocalDate previousWeekStart = weekStart.minusWeeks(1);
        List<Schedule> previous = scheduleRepository
                .findByDoctorIdAndWorkDateBetween(doctorId, previousWeekStart, weekStart.minusDays(1));
        if (!previous.isEmpty()) {
            List<PlanSlot> slots = new ArrayList<>();
            for (Schedule schedule : previous) {
                if (!scheduleRules.isWithinWorkHours(schedule.getTimeSlot())) {
                    continue;
                }
                PlanSlot slot = new PlanSlot();
                slot.date = weekStart.plusDays(ChronoUnit.DAYS.between(previousWeekStart, schedule.getWorkDate()));
                slot.timeSlot = schedule.getTimeSlot();
                slot.maxPatients = schedule.getMaxPatients() != null ? schedule.getMaxPatients() : 2;
                slots.add(slot);
            }
            sortSlots(slots);
            return new Plan(slots, "previous");
        }

        List<DoctorScheduleSetting> settings = settingRepository.findByDoctorIdAndEnabled(doctorId, 1);
        if (settings.isEmpty()) {
            return new Plan(new ArrayList<>(), "none");
        }
        List<PlanSlot> slots = new ArrayList<>();
        for (DoctorScheduleSetting setting : settings) {
            if (setting.getDayOfWeek() == null || setting.getStartTime() == null || setting.getEndTime() == null) {
                continue;
            }
            LocalDate date = weekStart.plusDays(setting.getDayOfWeek() - 1);
            int duration = setting.getSlotDuration() != null ? setting.getSlotDuration() : 20;
            int maxPatients = setting.getMaxPatientsPerSlot() != null ? setting.getMaxPatientsPerSlot() : 2;
            LocalTime current = setting.getStartTime();
            while (!current.plusMinutes(duration).isAfter(setting.getEndTime())) {
                LocalTime next = current.plusMinutes(duration);
                String timeSlot = current.format(SLOT_FORMATTER) + "-" + next.format(SLOT_FORMATTER);
                if (scheduleRules.isWithinWorkHours(timeSlot)) {
                    PlanSlot slot = new PlanSlot();
                    slot.date = date;
                    slot.timeSlot = timeSlot;
                    slot.maxPatients = maxPatients;
                    slots.add(slot);
                }
                current = next;
            }
        }
        sortSlots(slots);
        return new Plan(slots, "template");
    }

    private void sortSlots(List<PlanSlot> slots) {
        slots.sort(Comparator.comparing((PlanSlot s) -> s.date)
                .thenComparing(s -> parseStart(s.timeSlot)));
    }

    private LocalTime parseStart(String timeSlot) {
        try {
            return LocalTime.parse(timeSlot.split("-")[0].trim(), SLOT_FORMATTER);
        } catch (Exception e) {
            return LocalTime.MIN;
        }
    }

    private List<Map<String, Object>> buildDayViews(Plan plan, LocalDate weekStart) {
        Map<LocalDate, List<PlanSlot>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            grouped.put(weekStart.plusDays(i), new ArrayList<>());
        }
        for (PlanSlot slot : plan.slots) {
            grouped.computeIfAbsent(slot.date, k -> new ArrayList<>()).add(slot);
        }
        List<Map<String, Object>> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<PlanSlot>> entry : grouped.entrySet()) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", entry.getKey().toString());
            day.put("dayName", DAY_NAMES[entry.getKey().getDayOfWeek().getValue() - 1]);
            List<Map<String, Object>> slots = new ArrayList<>();
            for (PlanSlot slot : entry.getValue()) {
                Map<String, Object> slotView = new HashMap<>();
                slotView.put("timeSlot", slot.timeSlot);
                slotView.put("maxPatients", slot.maxPatients);
                slots.add(slotView);
            }
            day.put("slots", slots);
            day.put("slotCount", slots.size());
            days.add(day);
        }
        return days;
    }

    private String weekLabel(LocalDate weekStart) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月d日");
        return formatter.format(weekStart) + " - " + formatter.format(weekStart.plusDays(6));
    }

    public static class ApplyResult {
        public int generated;
        public int skipped;
        public String source;
    }

    private static class Plan {
        final List<PlanSlot> slots;
        final String source;

        Plan(List<PlanSlot> slots, String source) {
            this.slots = slots;
            this.source = source;
        }
    }

    private static class PlanSlot {
        LocalDate date;
        String timeSlot;
        int maxPatients;
    }
}
