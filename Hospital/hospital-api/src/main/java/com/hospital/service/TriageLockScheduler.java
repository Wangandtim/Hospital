package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 60 秒释放超过 10 分钟 TTL 的分诊锁并归还号源；
 * 每天 03:15 清理超过保留期的未完成会话。
 */
@Component
public class TriageLockScheduler {

    @Autowired
    private TriageService triageService;

    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredLocks() {
        int released = triageService.releaseExpiredLocks();
        if (released > 0) {
            System.out.println("[TriageLockScheduler] 释放过期分诊锁 " + released + " 个");
        }
    }

    @Scheduled(cron = "0 15 3 * * ?")
    public void expireStaleSessions() {
        int expired = triageService.expireStaleSessions();
        if (expired > 0) {
            System.out.println("[TriageLockScheduler] 归档过期分诊会话 " + expired + " 个");
        }
    }
}
