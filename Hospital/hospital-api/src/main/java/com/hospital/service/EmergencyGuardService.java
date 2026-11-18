package com.hospital.service;

import com.hospital.exception.ForbiddenException;
import com.hospital.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应急权限自保护：限制单次会话的写操作数量、医院每日写入总量，
 * 一旦出现删除等破坏性操作立即冻结全院应急权限并吊销令牌。
 */
@Service
public class EmergencyGuardService {

    private static final int TOKEN_WRITE_LIMIT = 20;
    private static final int HOSPITAL_DAILY_WRITE_LIMIT = 40;

    private final Map<String, Integer> tokenWriteCounts = new ConcurrentHashMap<>();
    private final Map<String, DailyCounter> hospitalCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> hospitalLocks = new ConcurrentHashMap<>();
    private final Map<String, String> hospitalLockReasons = new ConcurrentHashMap<>();

    public synchronized boolean isLocked(String hospitalId) {
        LocalDateTime until = hospitalLocks.get(hospitalId);
        if (until == null) {
            return false;
        }
        if (until.isBefore(LocalDateTime.now())) {
            hospitalLocks.remove(hospitalId);
            hospitalLockReasons.remove(hospitalId);
            return false;
        }
        return true;
    }

    public String getLockReason(String hospitalId) {
        return hospitalLockReasons.get(hospitalId);
    }

    public LocalDateTime getLockUntil(String hospitalId) {
        LocalDateTime until = hospitalLocks.get(hospitalId);
        if (until != null && until.isBefore(LocalDateTime.now())) {
            return null;
        }
        return until;
    }

    @Autowired
    private TokenService tokenService;

    public synchronized void freeze(String hospitalId, int hours, String reason) {
        hospitalLocks.put(hospitalId, LocalDateTime.now().plusHours(hours));
        hospitalLockReasons.put(hospitalId, reason);
        tokenService.revokeEmergencyByHospital(hospitalId);
    }

    public synchronized void unfreeze(String hospitalId) {
        hospitalLocks.remove(hospitalId);
        hospitalLockReasons.remove(hospitalId);
    }

    public synchronized int getTodayWrites(String hospitalId) {
        DailyCounter counter = hospitalCounters.get(hospitalId);
        if (counter == null || !counter.date.equals(LocalDate.now())) {
            return 0;
        }
        return counter.count;
    }

    /**
     * 校验一次应急写操作；不通过时抛出 ForbiddenException，破坏性操作会直接冻结全院。
     */
    public synchronized void assertWriteAllowed(String token, String hospitalId, String method) {
        if (isLocked(hospitalId)) {
            throw new ForbiddenException("本院应急权限已冻结：" + getLockReason(hospitalId));
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            freeze(hospitalId, 2, "检测到应急账号发起删除操作");
            throw new ForbiddenException("应急权限禁止删除数据，本次操作已拦截并冻结全院应急权限");
        }

        DailyCounter counter = hospitalCounters.computeIfAbsent(hospitalId, id -> new DailyCounter());
        if (!counter.date.equals(LocalDate.now())) {
            counter.date = LocalDate.now();
            counter.count = 0;
        }
        if (counter.count >= HOSPITAL_DAILY_WRITE_LIMIT) {
            freeze(hospitalId, 2, "本院当日应急修改次数超过上限");
            throw new ForbiddenException("本院今日应急修改次数已达上限，应急权限已自动冻结");
        }
        counter.count++;

        int tokenCount = tokenWriteCounts.merge(token, 1, Integer::sum);
        if (tokenCount > TOKEN_WRITE_LIMIT) {
            freeze(hospitalId, 2, "单次应急会话修改次数异常");
            throw new ForbiddenException("单次应急会话修改次数异常，权限已自动冻结");
        }
    }

    public void clearToken(String token) {
        tokenWriteCounts.remove(token);
    }

    private static class DailyCounter {
        LocalDate date = LocalDate.now();
        int count = 0;
    }
}
