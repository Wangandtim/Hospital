package com.hospital.controller;

import com.hospital.entity.HospitalKey;
import com.hospital.exception.ForbiddenException;
import com.hospital.repository.HospitalKeyRepository;
import com.hospital.security.AuthContext;
import com.hospital.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "*")
public class KeyController {

    @Autowired
    private HospitalKeyRepository hospitalKeyRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuthContext authContext;

    /** 应急账号不能改密钥；院管只能操作本院，超管不限。 */
    private void requireKeyAccess(String hospitalId) {
        if (authContext.isEmergencyAdmin()) {
            throw new ForbiddenException("应急账号无权管理密钥");
        }
        authContext.requireHospitalScope(hospitalId);
    }

    /** 常设应急码：管理员账号不可用时使用，首次访问时自动生成。 */
    private String ensureRecoveryKey(HospitalKey hospitalKey) {
        if (hospitalKey.getRecoveryKey() == null || hospitalKey.getRecoveryKey().isEmpty()) {
            hospitalKey.setRecoveryKey(
                    UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            hospitalKey.setRecoveryStatus(1);
            hospitalKeyRepository.save(hospitalKey);
        }
        return hospitalKey.getRecoveryKey();
    }

    @GetMapping("/hospital/{hospitalId}")
    public Map<String, Object> getHospitalKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            requireKeyAccess(hospitalId);
            HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId).orElse(null);
            
            if (hospitalKey != null) {
                String recoveryKey = ensureRecoveryKey(hospitalKey);
                result.put("success", true);
                result.put("hospitalId", hospitalKey.getHospitalId());
                result.put("dailyKey", hospitalKey.getDailyKey());
                result.put("expireTime", hospitalKey.getExpireTime().toString());
                result.put("status", hospitalKey.getStatus() == 1 ? "有效" : "无效");
                result.put("recoveryKey", recoveryKey);
                result.put("recoveryStatus", hospitalKey.getRecoveryStatus());
                
                if (hospitalKey.getExpireTime().isBefore(LocalDateTime.now())) {
                    result.put("isExpired", true);
                    result.put("message", "当日密钥已过期，可重新生成；常设应急码仍可用");
                } else {
                    result.put("isExpired", false);
                }
            } else {
                result.put("success", false);
                result.put("message", "该医院暂无应急密钥，请先生成");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/all")
    public Map<String, Object> getAllKeys() {
        Map<String, Object> result = new HashMap<>();

        try {
            authContext.requireSuperAdmin();
            List<HospitalKey> keys = hospitalKeyRepository.findAll();
            for (HospitalKey key : keys) {
                ensureRecoveryKey(key);
            }
            result.put("success", true);
            result.put("data", keys);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/hospital/{hospitalId}/generate")
    public Map<String, Object> generateKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            requireKeyAccess(hospitalId);
            String dailyKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            LocalDateTime expireTime = LocalDateTime.now().plusDays(1);

            HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId).orElse(null);
            
            if (hospitalKey != null) {
                hospitalKey.setDailyKey(dailyKey);
                hospitalKey.setExpireTime(expireTime);
                hospitalKey.setLastRenewTime(LocalDateTime.now());
                hospitalKey.setStatus(1);
            } else {
                hospitalKey = new HospitalKey(hospitalId, dailyKey, expireTime);
            }

            String recoveryKey = ensureRecoveryKey(hospitalKey);
            hospitalKeyRepository.save(hospitalKey);
            auditLogService.record("KEY_GENERATE", "hospital", hospitalId, "expire=" + expireTime);

            result.put("success", true);
            result.put("message", "密钥生成成功");
            result.put("hospitalId", hospitalId);
            result.put("dailyKey", dailyKey);
            result.put("expireTime", expireTime.toString());
            result.put("recoveryKey", recoveryKey);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/hospital/{hospitalId}/renew")
    public Map<String, Object> renewKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            requireKeyAccess(hospitalId);
            HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId).orElse(null);
            
            if (hospitalKey != null) {
                hospitalKey.setExpireTime(LocalDateTime.now().plusDays(1));
                hospitalKey.setLastRenewTime(LocalDateTime.now());
                hospitalKey.setStatus(1);
                hospitalKeyRepository.save(hospitalKey);
                auditLogService.record("KEY_RENEW", "hospital", hospitalId, null);

                result.put("success", true);
                result.put("message", "密钥已续期");
                result.put("expireTime", hospitalKey.getExpireTime().toString());
            } else {
                result.put("success", false);
                result.put("message", "该医院暂无密钥，请先生成");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/hospital/{hospitalId}/invalidate")
    public Map<String, Object> invalidateKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            requireKeyAccess(hospitalId);
            HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId).orElse(null);
            
            if (hospitalKey != null) {
                hospitalKey.setStatus(0);
                hospitalKeyRepository.save(hospitalKey);
                auditLogService.record("KEY_INVALIDATE", "hospital", hospitalId, null);

                result.put("success", true);
                result.put("message", "密钥已失效");
            } else {
                result.put("success", false);
                result.put("message", "该医院暂无密钥");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /** 重置常设应急码：本院管理员可自助刷新，超管可代为重置；同一医院始终只有一把。 */
    @PostMapping("/hospital/{hospitalId}/recovery/regenerate")
    public Map<String, Object> regenerateRecoveryKey(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();

        try {
            requireKeyAccess(hospitalId);
            HospitalKey hospitalKey = hospitalKeyRepository.findById(hospitalId).orElse(null);
            if (hospitalKey == null) {
                result.put("success", false);
                result.put("message", "该医院暂无应急密钥，请先生成");
                return result;
            }

            String recoveryKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            hospitalKey.setRecoveryKey(recoveryKey);
            hospitalKey.setRecoveryStatus(1);
            hospitalKeyRepository.save(hospitalKey);
            auditLogService.record("RECOVERY_KEY_REGENERATE", "hospital", hospitalId, null);

            result.put("success", true);
            result.put("message", "常设应急码已重置，请重新线下封存");
            result.put("recoveryKey", recoveryKey);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}