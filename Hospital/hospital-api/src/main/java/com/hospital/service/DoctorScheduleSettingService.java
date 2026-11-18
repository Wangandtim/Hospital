package com.hospital.service;

import com.hospital.entity.DoctorScheduleSetting;
import com.hospital.repository.DoctorScheduleSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleSettingService {

    @Autowired
    private DoctorScheduleSettingRepository settingRepository;

    @Autowired
    private DoctorService doctorService;

    public List<DoctorScheduleSetting> getSettingsByDoctorId(String doctorId) {
        doctorService.findById(doctorId);
        List<DoctorScheduleSetting> settings = settingRepository.findByDoctorId(doctorId);
        return settings.stream()
                .sorted(Comparator.comparing(DoctorScheduleSetting::getDayOfWeek))
                .collect(Collectors.toList());
    }

    public List<DoctorScheduleSetting> getEnabledSettings(String doctorId) {
        return settingRepository.findByDoctorIdAndEnabled(doctorId, 1);
    }

    @Transactional
    public DoctorScheduleSetting createSetting(DoctorScheduleSetting setting) {
        doctorService.findById(setting.getDoctorId());
        
        if (setting.getSettingId() == null) {
            setting.setSettingId("DSS" + System.currentTimeMillis() % 100000);
        }
        
        if (setting.getEnabled() == null) {
            setting.setEnabled(1);
        }
        if (setting.getSlotDuration() == null) {
            setting.setSlotDuration(20);
        }
        if (setting.getMaxPatientsPerSlot() == null) {
            setting.setMaxPatientsPerSlot(2);
        }

        return settingRepository.save(setting);
    }

    @Transactional
    public DoctorScheduleSetting updateSetting(String settingId, DoctorScheduleSetting setting) {
        DoctorScheduleSetting existing = settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设置不存在"));

        if (setting.getDayOfWeek() != null) {
            existing.setDayOfWeek(setting.getDayOfWeek());
        }
        if (setting.getStartTime() != null) {
            existing.setStartTime(setting.getStartTime());
        }
        if (setting.getEndTime() != null) {
            existing.setEndTime(setting.getEndTime());
        }
        if (setting.getEnabled() != null) {
            existing.setEnabled(setting.getEnabled());
        }
        if (setting.getSlotDuration() != null) {
            existing.setSlotDuration(setting.getSlotDuration());
        }
        if (setting.getMaxPatientsPerSlot() != null) {
            existing.setMaxPatientsPerSlot(setting.getMaxPatientsPerSlot());
        }

        return settingRepository.save(existing);
    }

    @Transactional
    public void deleteSetting(String settingId) {
        if (!settingRepository.existsById(settingId)) {
            throw new RuntimeException("设置不存在");
        }
        settingRepository.deleteById(settingId);
    }

    public DoctorScheduleSetting getSettingById(String settingId) {
        return settingRepository.findById(settingId)
                .orElseThrow(() -> new RuntimeException("设置不存在"));
    }

    @Transactional
    public void deleteSettingsByDoctorId(String doctorId) {
        settingRepository.deleteByDoctorId(doctorId);
    }

    @Transactional
    public void saveSettings(String doctorId, List<DoctorScheduleSetting> settings) {
        doctorService.findById(doctorId);
        
        settingRepository.deleteByDoctorId(doctorId);
        
        for (DoctorScheduleSetting setting : settings) {
            setting.setDoctorId(doctorId);
            if (setting.getSettingId() == null) {
                setting.setSettingId("DSS" + System.currentTimeMillis() % 100000 + System.nanoTime() % 1000);
            }
            settingRepository.save(setting);
        }
    }

    public boolean isDoctorAvailableOnDate(String doctorId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<DoctorScheduleSetting> settings = settingRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        
        if (settings.isEmpty()) {
            return false;
        }
        
        return settings.stream().anyMatch(s -> s.getEnabled() == 1);
    }

    public List<String> getAvailableTimeSlots(String doctorId, LocalDate date) {
        List<String> slots = new ArrayList<>();
        
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<DoctorScheduleSetting> settings = settingRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        
        for (DoctorScheduleSetting setting : settings) {
            if (setting.getEnabled() != 1 || setting.getStartTime() == null || setting.getEndTime() == null) {
                continue;
            }
            
            LocalTime current = setting.getStartTime();
            LocalTime end = setting.getEndTime();
            int duration = setting.getSlotDuration() != null ? setting.getSlotDuration() : 20;
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            
            while (!current.isAfter(end)) {
                LocalTime slotEnd = current.plusMinutes(duration);
                if (slotEnd.isAfter(end)) {
                    break;
                }
                slots.add(current.format(formatter) + "-" + slotEnd.format(formatter));
                current = slotEnd;
            }
        }
        
        return slots;
    }

    public Map<String, Object> getWeeklySchedule(String doctorId) {
        Map<String, Object> result = new HashMap<>();
        List<DoctorScheduleSetting> settings = getSettingsByDoctorId(doctorId);
        
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (int i = 1; i <= 7; i++) {
            int currentDay = i;
            List<DoctorScheduleSetting> daySettings = new ArrayList<>();
            for (DoctorScheduleSetting s : settings) {
                if (s.getDayOfWeek() != null && s.getDayOfWeek().equals(currentDay)) {
                    daySettings.add(s);
                }
            }
            
            List<Map<String, Object>> timeSlots = new ArrayList<>();
            boolean enabled = false;
            for (DoctorScheduleSetting s : daySettings) {
                if (s.getEnabled() == 1) {
                    enabled = true;
                    break;
                }
            }
            
            for (DoctorScheduleSetting setting : daySettings) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("settingId", setting.getSettingId());
                slot.put("startTime", setting.getStartTime() != null ? setting.getStartTime().format(formatter) : null);
                slot.put("endTime", setting.getEndTime() != null ? setting.getEndTime().format(formatter) : null);
                slot.put("enabled", setting.getEnabled() == 1);
                slot.put("slotDuration", setting.getSlotDuration());
                slot.put("maxPatientsPerSlot", setting.getMaxPatientsPerSlot());
                timeSlots.add(slot);
            }
            
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("dayOfWeek", i);
            dayInfo.put("dayName", days[i - 1]);
            dayInfo.put("enabled", enabled);
            dayInfo.put("timeSlots", timeSlots);
            
            result.put("day" + i, dayInfo);
        }
        
        return result;
    }
}