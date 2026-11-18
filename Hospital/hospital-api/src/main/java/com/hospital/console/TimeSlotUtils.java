package com.hospital.console;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间段工具类
 */
public class TimeSlotUtils {
    
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("h:mma");
    
    /**
     * 解析时间字符串，支持多种格式
     * 支持: "8am", "8:00am", "8:00", "8"
     */
    public static LocalTime parseTime(String input) {
        input = input.trim().toLowerCase();
        
        // 移除空格
        input = input.replace(" ", "");
        
        // 判断是否有am/pm
        boolean isPM = input.contains("pm");
        boolean hasAmPm = input.contains("am") || input.contains("pm");
        
        // 移除am/pm
        if (hasAmPm) {
            input = input.replace("am", "").replace("pm", "");
        }
        
        int hour, minute = 0;
        
        if (input.contains(":")) {
            String[] parts = input.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } else {
            hour = Integer.parseInt(input);
        }
        
        // 转换为24小时制
        if (isPM && hour < 12) {
            hour += 12;
        } else if (!isPM && hour == 12) {
            hour = 0;
        } else if (!hasAmPm && hour < 6) {
            // 如果没有am/pm且小时<6，视为下午
            hour += 12;
        }
        
        return LocalTime.of(hour, minute);
    }
    
    /**
     * 格式化时间为 8am, 9:30pm 格式
     */
    public static String formatTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        
        boolean isPM = hour >= 12;
        if (hour > 12) {
            hour -= 12;
        }
        if (hour == 0) {
            hour = 12;
        }
        
        if (minute == 0) {
            return hour + (isPM ? "pm" : "am");
        } else {
            return hour + ":" + String.format("%02d", minute) + (isPM ? "pm" : "am");
        }
    }
    
    /**
     * 将医生的大时间段拆分成20分钟一段
     * 例如: "8am-10am" -> ["8am-8:20am", "8:20am-8:40am", "8:40am-9am", ...]
     * 每个20分钟段最多2人
     */
    public static List<String> splitTo20MinuteSlots(String startStr, String endStr) {
        LocalTime start = parseTime(startStr);
        LocalTime end = parseTime(endStr);
        
        List<String> slots = new ArrayList<>();
        LocalTime current = start;
        
        while (current.plusMinutes(20).compareTo(end) <= 0) {
            LocalTime next = current.plusMinutes(20);
            slots.add(formatTime(current) + "-" + formatTime(next));
            current = next;
        }
        
        return slots;
    }
    
    /**
     * 验证时间格式是否正确
     */
    public static boolean isValidTime(String input) {
        try {
            parseTime(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查结束时间是否晚于开始时间
     */
    public static boolean isEndAfterStart(String startStr, String endStr) {
        try {
            LocalTime start = parseTime(startStr);
            LocalTime end = parseTime(endStr);
            return end.isAfter(start);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 将时间段转换为显示格式
     */
    public static String formatSlot(String slot) {
        // 如果已经是友好格式，直接返回
        if (slot.matches(".*[ap]m.*")) {
            return slot;
        }
        
        // 否则转换
        String[] parts = slot.split("-");
        if (parts.length == 2) {
            try {
                String start = formatTime(parseTime(parts[0]));
                String end = formatTime(parseTime(parts[1]));
                return start + "-" + end;
            } catch (Exception e) {
                return slot;
            }
        }
        return slot;
    }
}
