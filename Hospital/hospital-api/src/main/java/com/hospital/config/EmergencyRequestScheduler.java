package com.hospital.config;

import com.hospital.service.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每分钟处理应急权限到期、报告逾期、卫健委确认逾期。 */
@Component
public class EmergencyRequestScheduler {

    @Autowired
    private EmergencyService emergencyService;

    @Scheduled(fixedDelay = 60000)
    public void processTimeouts() {
        try {
            emergencyService.processTimeouts();
        } catch (Exception e) {
            System.out.println("应急超时处理失败: " + e.getMessage());
        }
    }
}
