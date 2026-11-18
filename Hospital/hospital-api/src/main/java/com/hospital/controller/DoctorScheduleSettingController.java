package com.hospital.controller;

import com.hospital.entity.DoctorScheduleSetting;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorScheduleSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor-schedule-settings")
@CrossOrigin(origins = "*")
public class DoctorScheduleSettingController {

    @Autowired
    private DoctorScheduleSettingService settingService;

    @Autowired
    private AuthContext authContext;

    /** 医生只能操作自己的排班模板；管理员不受限。 */
    private void requireDoctorScope(String doctorId) {
        if ("DOCTOR".equals(authContext.role())) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(doctorId)) {
                throw new RuntimeException("无权操作其他医生的排班");
            }
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public Map<String, Object> getSettings(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DoctorScheduleSetting> settings = settingService.getSettingsByDoctorId(doctorId);
            result.put("success", true);
            result.put("data", settings);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/doctor/{doctorId}/weekly")
    public Map<String, Object> getWeeklySchedule(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> schedule = settingService.getWeeklySchedule(doctorId);
            result.put("success", true);
            result.put("data", schedule);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/doctor/{doctorId}/available-slots")
    public Map<String, Object> getAvailableSlots(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> slots = settingService.getAvailableTimeSlots(doctorId, date);
            result.put("success", true);
            result.put("data", slots);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/doctor/{doctorId}/available")
    public Map<String, Object> checkAvailability(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean available = settingService.isDoctorAvailableOnDate(doctorId, date);
            result.put("success", true);
            result.put("available", available);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> createSetting(@RequestBody DoctorScheduleSetting setting) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(setting.getDoctorId());
            DoctorScheduleSetting created = settingService.createSetting(setting);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{settingId}")
    public Map<String, Object> updateSetting(
            @PathVariable String settingId,
            @RequestBody DoctorScheduleSetting setting) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(settingService.getSettingById(settingId).getDoctorId());
            DoctorScheduleSetting updated = settingService.updateSetting(settingId, setting);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{settingId}")
    public Map<String, Object> deleteSetting(@PathVariable String settingId) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(settingService.getSettingById(settingId).getDoctorId());
            settingService.deleteSetting(settingId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/doctor/{doctorId}/batch")
    public Map<String, Object> saveSettings(
            @PathVariable String doctorId,
            @RequestBody List<DoctorScheduleSetting> settings) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            settingService.saveSettings(doctorId, settings);
            result.put("success", true);
            result.put("message", "保存成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/doctor/{doctorId}/add-time-slot")
    public Map<String, Object> addTimeSlot(
            @PathVariable String doctorId,
            @RequestParam Integer dayOfWeek,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(defaultValue = "20") Integer slotDuration,
            @RequestParam(defaultValue = "2") Integer maxPatientsPerSlot) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            DoctorScheduleSetting setting = new DoctorScheduleSetting();
            setting.setDoctorId(doctorId);
            setting.setDayOfWeek(dayOfWeek);
            setting.setStartTime(startTime);
            setting.setEndTime(endTime);
            setting.setSlotDuration(slotDuration);
            setting.setMaxPatientsPerSlot(maxPatientsPerSlot);
            setting.setEnabled(1);

            DoctorScheduleSetting created = settingService.createSetting(setting);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/doctor/{doctorId}/toggle-day")
    public Map<String, Object> toggleDay(
            @PathVariable String doctorId,
            @RequestParam Integer dayOfWeek,
            @RequestParam Boolean enabled) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            List<DoctorScheduleSetting> settings = settingService.getSettingsByDoctorId(doctorId);
            
            for (DoctorScheduleSetting setting : settings) {
                if (setting.getDayOfWeek().equals(dayOfWeek)) {
                    setting.setEnabled(enabled ? 1 : 0);
                    settingService.updateSetting(setting.getSettingId(), setting);
                }
            }
            
            result.put("success", true);
            result.put("message", enabled ? "已启用" : "已禁用");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}