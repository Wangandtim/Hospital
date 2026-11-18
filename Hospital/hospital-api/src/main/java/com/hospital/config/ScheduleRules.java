package com.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 排班时间与预约范围规则（统一配置来源）。
 * - 号源只生成在正常工作时段：默认 08:00-12:00、14:00-17:30，20 分钟一段；
 * - 急诊科不参与预约，只提供医院名称/地址/电话等急诊信息。
 */
@Component
public class ScheduleRules {

    private final List<String> windows = new ArrayList<>();
    private final int slotMinutes;
    private final int maxPatientsPerSlot;
    private final Set<String> emergencyDeptNames = new LinkedHashSet<>();

    public ScheduleRules(
            @Value("${hospital.schedule.work-hours:08:00-12:00,14:00-17:30}") String workHours,
            @Value("${hospital.schedule.slot-minutes:20}") int slotMinutes,
            @Value("${hospital.schedule.max-patients-per-slot:2}") int maxPatientsPerSlot,
            @Value("${hospital.schedule.emergency-dept-names:急诊科,急诊}") String emergencyDeptNames) {
        for (String window : workHours.split(",")) {
            if (!window.trim().isEmpty()) {
                windows.add(window.trim());
            }
        }
        this.slotMinutes = Math.max(slotMinutes, 5);
        this.maxPatientsPerSlot = Math.max(maxPatientsPerSlot, 1);
        for (String name : emergencyDeptNames.split(",")) {
            if (!name.trim().isEmpty()) {
                this.emergencyDeptNames.add(name.trim());
            }
        }
    }

    public int getSlotMinutes() { return slotMinutes; }

    public int getMaxPatientsPerSlot() { return maxPatientsPerSlot; }

    public String getWorkHoursText() { return String.join(",", windows); }

    /** 按工作时段展开的 20 分钟号源段，例如 08:00-08:20、08:20-08:40。 */
    public List<String> timeSlots() {
        List<String> slots = new ArrayList<>();
        for (String window : windows) {
            String[] parts = window.split("-");
            if (parts.length != 2) {
                continue;
            }
            LocalTime start;
            LocalTime end;
            try {
                start = LocalTime.parse(parts[0].trim());
                end = LocalTime.parse(parts[1].trim());
            } catch (RuntimeException e) {
                continue;
            }
            while (!start.plusMinutes(slotMinutes).isAfter(end)) {
                LocalTime next = start.plusMinutes(slotMinutes);
                slots.add(format(start) + "-" + format(next));
                start = next;
            }
        }
        return slots;
    }

    /** 判断一个时段是否完整落在工作时段内（用于续排时跳过历史遗留的全天时段）。 */
    public boolean isWithinWorkHours(String timeSlot) {
        if (timeSlot == null) {
            return false;
        }
        String[] parts = timeSlot.trim().split("-");
        if (parts.length != 2) {
            return false;
        }
        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(parts[0].trim());
            end = LocalTime.parse(parts[1].trim());
        } catch (RuntimeException e) {
            return false;
        }
        for (String window : windows) {
            String[] w = window.split("-");
            if (w.length != 2) {
                continue;
            }
            LocalTime windowStart;
            LocalTime windowEnd;
            try {
                windowStart = LocalTime.parse(w[0].trim());
                windowEnd = LocalTime.parse(w[1].trim());
            } catch (RuntimeException e) {
                continue;
            }
            if (!start.isBefore(windowStart) && !end.isAfter(windowEnd)) {
                return true;
            }
        }
        return false;
    }

    /** 是否为急诊类科室（默认含“急诊科”“急诊”，如“急诊医学科”也会命中）。 */
    public boolean isEmergencyDeptName(String deptName) {
        if (deptName == null || deptName.trim().isEmpty()) {
            return false;
        }
        String normalized = deptName.trim();
        for (String name : emergencyDeptNames) {
            if (normalized.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private String format(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }
}
