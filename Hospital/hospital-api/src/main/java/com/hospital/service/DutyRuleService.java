package com.hospital.service;

import com.hospital.entity.DoctorDutyRule;
import com.hospital.entity.Schedule;
import com.hospital.repository.DoctorDutyRuleRepository;
import com.hospital.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 出诊规则与达标核对。
 * 只输出事实：某天有没有排班、时段是否覆盖规则窗口、本周是否达标，不生成评分。
 */
@Service
public class DutyRuleService {

    private static final String[] WEEKDAY_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    @Autowired
    private DoctorDutyRuleRepository ruleRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    public DoctorDutyRule findById(String ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("出诊规则不存在"));
    }

    public List<DoctorDutyRule> findByDoctor(String doctorId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);
        return ruleRepository.findByDoctorIdAndDutyDateBetweenOrderByDutyDateAscStartTimeAsc(doctorId, start, end);
    }

    public List<DoctorDutyRule> findByHospital(String hospitalId, LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ruleRepository.findByHospitalIdAndDutyDateOrderByStartTimeAsc(hospitalId, target);
    }

    public DoctorDutyRule create(DoctorDutyRule rule) {
        if (rule.getDoctorId() == null || rule.getDoctorId().isEmpty()) {
            throw new RuntimeException("请选择医生");
        }
        if (rule.getDutyDate() == null) {
            throw new RuntimeException("请选择出诊日期");
        }
        validateWindow(rule.getStartTime(), rule.getEndTime());

        if (rule.getRuleId() == null || rule.getRuleId().isEmpty()) {
            rule.setRuleId("DUTY" + System.currentTimeMillis() % 1000000);
        }
        rule.setStatus(1);
        rule.setCreatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    public DoctorDutyRule update(String ruleId, DoctorDutyRule update) {
        DoctorDutyRule existing = findById(ruleId);
        if (update.getDutyDate() != null) {
            existing.setDutyDate(update.getDutyDate());
        }
        if (update.getStartTime() != null) {
            existing.setStartTime(update.getStartTime());
        }
        if (update.getEndTime() != null) {
            existing.setEndTime(update.getEndTime());
        }
        if (update.getStatus() != null) {
            existing.setStatus(update.getStatus());
        }
        validateWindow(existing.getStartTime(), existing.getEndTime());
        return ruleRepository.save(existing);
    }

    public void delete(String ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    public LocalDate defaultWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 某医生一周的在岗事实与达标情况。
     */
    public Map<String, Object> buildWeekSummary(String doctorId, LocalDate weekStart) {
        LocalDate start = weekStart != null ? weekStart : defaultWeekStart();
        LocalDate end = start.plusDays(6);

        List<DoctorDutyRule> rules = ruleRepository
                .findByDoctorIdAndDutyDateBetweenOrderByDutyDateAscStartTimeAsc(doctorId, start, end);
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndWorkDateBetween(doctorId, start, end);

        Map<LocalDate, List<DoctorDutyRule>> rulesByDate = new LinkedHashMap<>();
        for (DoctorDutyRule rule : rules) {
            rulesByDate.computeIfAbsent(rule.getDutyDate(), k -> new ArrayList<>()).add(rule);
        }
        Map<LocalDate, List<Schedule>> schedulesByDate = new LinkedHashMap<>();
        for (Schedule schedule : schedules) {
            schedulesByDate.computeIfAbsent(schedule.getWorkDate(), k -> new ArrayList<>()).add(schedule);
        }

        List<Map<String, Object>> days = new ArrayList<>();
        int weekRequired = 0;
        int weekCovered = 0;
        int daysWithRules = 0;
        int daysOnDuty = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            List<DoctorDutyRule> dayRules = rulesByDate.getOrDefault(date, List.of());
            List<Schedule> daySchedules = schedulesByDate.getOrDefault(date, List.of());

            List<int[]> ruleIntervals = new ArrayList<>();
            List<Map<String, Object>> ruleViews = new ArrayList<>();
            for (DoctorDutyRule rule : dayRules) {
                if (rule.getStatus() != null && rule.getStatus() == 0) {
                    continue;
                }
                int[] interval = new int[]{toMinutes(rule.getStartTime()), toMinutes(rule.getEndTime())};
                ruleIntervals.add(interval);
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("ruleId", rule.getRuleId());
                view.put("startTime", rule.getStartTime());
                view.put("endTime", rule.getEndTime());
                ruleViews.add(view);
            }

            List<int[]> scheduleIntervals = new ArrayList<>();
            List<Map<String, Object>> scheduleViews = new ArrayList<>();
            for (Schedule schedule : daySchedules) {
                int[] interval = parseSlot(schedule.getTimeSlot());
                if (interval == null) {
                    continue;
                }
                scheduleIntervals.add(interval);
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("scheduleId", schedule.getScheduleId());
                view.put("timeSlot", schedule.getTimeSlot());
                view.put("maxPatients", schedule.getMaxPatients());
                view.put("bookedCount", schedule.getBookedCount());
                view.put("status", schedule.getStatus());
                scheduleViews.add(view);
            }

            List<int[]> ruleUnion = union(ruleIntervals);
            int required = totalMinutes(ruleUnion);
            int covered = overlapMinutes(ruleUnion, union(scheduleIntervals));

            boolean hasRules = !ruleUnion.isEmpty();
            Boolean onDuty = hasRules ? covered >= required : null;
            String statusText;
            if (!hasRules) {
                statusText = "未设出诊规则";
            } else if (onDuty) {
                statusText = "在岗";
            } else if (covered > 0) {
                statusText = "在岗时段不足";
            } else {
                statusText = "未在岗";
            }

            if (hasRules) {
                daysWithRules++;
                weekRequired += required;
                weekCovered += covered;
                if (Boolean.TRUE.equals(onDuty)) {
                    daysOnDuty++;
                }
            }

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("weekday", WEEKDAY_NAMES[i]);
            day.put("rules", ruleViews);
            day.put("schedules", scheduleViews);
            day.put("requiredMinutes", required);
            day.put("coveredMinutes", covered);
            day.put("onDuty", onDuty);
            day.put("statusText", statusText);
            days.add(day);
        }

        String weekStatusText;
        if (daysWithRules == 0) {
            weekStatusText = "本周未设出诊规则";
        } else if (daysOnDuty == daysWithRules) {
            weekStatusText = "本周时段达标";
        } else {
            weekStatusText = "本周时段未达标";
        }

        Map<String, Object> week = new LinkedHashMap<>();
        week.put("requiredMinutes", weekRequired);
        week.put("coveredMinutes", weekCovered);
        week.put("daysWithRules", daysWithRules);
        week.put("daysOnDuty", daysOnDuty);
        week.put("statusText", weekStatusText);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("doctorId", doctorId);
        result.put("weekStart", start.toString());
        result.put("weekEnd", end.toString());
        result.put("days", days);
        result.put("week", week);
        return result;
    }

    /**
     * 今日在岗情况（用于医生列表概览）。
     */
    public Map<String, Object> todayStatus(String doctorId) {
        LocalDate today = LocalDate.now();
        List<DoctorDutyRule> rules = ruleRepository.findByDoctorIdAndDutyDateOrderByStartTimeAsc(doctorId, today);
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, today);

        List<int[]> ruleIntervals = new ArrayList<>();
        for (DoctorDutyRule rule : rules) {
            if (rule.getStatus() != null && rule.getStatus() == 0) {
                continue;
            }
            ruleIntervals.add(new int[]{toMinutes(rule.getStartTime()), toMinutes(rule.getEndTime())});
        }
        List<int[]> scheduleIntervals = new ArrayList<>();
        for (Schedule schedule : schedules) {
            int[] interval = parseSlot(schedule.getTimeSlot());
            if (interval != null) {
                scheduleIntervals.add(interval);
            }
        }

        int required = totalMinutes(union(ruleIntervals));
        int covered = overlapMinutes(union(ruleIntervals), union(scheduleIntervals));
        Map<String, Object> status = new HashMap<>();
        status.put("requiredMinutes", required);
        status.put("coveredMinutes", covered);
        status.put("hasRules", required > 0);
        status.put("onDuty", required > 0 ? covered >= required : null);
        status.put("statusText", required == 0 ? "今日未设出诊规则" : (covered >= required ? "在岗" : (covered > 0 ? "在岗时段不足" : "未在岗")));
        return status;
    }

    private void validateWindow(String startTime, String endTime) {
        int start = toMinutes(startTime);
        int end = toMinutes(endTime);
        if (start < 0 || end < 0) {
            throw new RuntimeException("出诊时段格式应为 HH:mm");
        }
        if (start >= end) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
    }

    private int toMinutes(String time) {
        if (time == null || time.isEmpty()) {
            return -1;
        }
        String[] parts = time.split(":");
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return -1;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 解析 "08:00-10:00" 这类排班时段。 */
    private int[] parseSlot(String timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        String[] parts = timeSlot.split("-");
        if (parts.length != 2) {
            return null;
        }
        int start = toMinutes(parts[0].trim());
        int end = toMinutes(parts[1].trim());
        if (start < 0 || end < 0 || start >= end) {
            return null;
        }
        return new int[]{start, end};
    }

    private List<int[]> union(List<int[]> intervals) {
        if (intervals.isEmpty()) {
            return new ArrayList<>();
        }
        List<int[]> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        int[] current = new int[]{sorted.get(0)[0], sorted.get(0)[1]};
        for (int i = 1; i < sorted.size(); i++) {
            int[] next = sorted.get(i);
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                merged.add(current);
                current = new int[]{next[0], next[1]};
            }
        }
        merged.add(current);
        return merged;
    }

    private int totalMinutes(List<int[]> intervals) {
        int total = 0;
        for (int[] interval : intervals) {
            total += interval[1] - interval[0];
        }
        return total;
    }

    /** 两组互不相交区间的交集总分钟数。 */
    private int overlapMinutes(List<int[]> left, List<int[]> right) {
        int total = 0;
        for (int[] a : left) {
            for (int[] b : right) {
                total += Math.max(0, Math.min(a[1], b[1]) - Math.max(a[0], b[0]));
            }
        }
        return total;
    }
}
