package com.hospital.config;

import com.hospital.service.ScheduleWeeklyConfirmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * 周排班确认定时任务：
 * - 每周日 20:00 生成下周排班确认提醒
 * - 每周一 00:10 对仍未确认的医生自动沿用上一周排班
 */
@Component
public class WeeklyScheduleConfirmScheduler {

    private static final Logger logger = Logger.getLogger(WeeklyScheduleConfirmScheduler.class.getName());

    @Autowired
    private ScheduleWeeklyConfirmService confirmService;

    @Scheduled(cron = "0 0 20 * * SUN")
    public void remindNextWeek() {
        try {
            int created = confirmService.createRemindersForAllDoctors();
            logger.info("周日排班确认提醒完成，新增 " + created + " 条");
        } catch (Exception e) {
            logger.severe("周日排班确认提醒失败: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void inheritUnconfirmed() {
        try {
            int inherited = confirmService.autoInheritExpiredPending();
            logger.info("超时未确认排班自动沿用完成，处理 " + inherited + " 位医生");
        } catch (Exception e) {
            logger.severe("超时排班自动沿用失败: " + e.getMessage());
        }
    }
}
