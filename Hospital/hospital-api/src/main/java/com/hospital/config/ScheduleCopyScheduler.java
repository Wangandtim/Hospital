package com.hospital.config;

import com.hospital.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

@Component
public class ScheduleCopyScheduler {

    private static final Logger logger = Logger.getLogger(ScheduleCopyScheduler.class.getName());

    @Autowired
    private ScheduleService scheduleService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void copyTodayScheduleToTomorrow() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        logger.info("开始自动复制排班：" + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + " -> " + tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        try {
            int copiedCount = scheduleService.copyScheduleToNextDay();
            logger.info("排班自动复制完成，共复制 " + copiedCount + " 条排班");
        } catch (Exception e) {
            logger.severe("排班自动复制失败: " + e.getMessage());
        }
    }
}