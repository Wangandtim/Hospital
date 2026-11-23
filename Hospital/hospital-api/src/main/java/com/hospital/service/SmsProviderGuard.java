package com.hospital.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 短信通道启动兜底：
 * - provider 只允许 log / aliyun / tencent；
 * - dev-mode=false 时禁止使用 log 通道，避免上线后验证码静默丢失（直接拒绝启动）；
 * - dev-mode=true 时只允许日志/跳过发送，不会真实发短信。
 */
@Component
public class SmsProviderGuard {

    private static final Logger log = LoggerFactory.getLogger(SmsProviderGuard.class);

    @Value("${sms.provider:log}")
    private String provider;

    @Value("${sms.dev-mode:false}")
    private boolean devMode;

    @PostConstruct
    public void validate() {
        String normalized = provider == null ? "log" : provider.trim().toLowerCase(Locale.ROOT);
        if (!"log".equals(normalized) && !"aliyun".equals(normalized) && !"tencent".equals(normalized)) {
            throw new IllegalStateException("SMS_PROVIDER 配置无效：" + provider + "，仅支持 log|aliyun|tencent");
        }
        if ("log".equals(normalized) && !devMode) {
            throw new IllegalStateException(
                    "SMS_PROVIDER=log 且 sms.dev-mode=false：验证码只会写日志、不会真实发送，拒绝启动。"
                            + "请将 SMS_PROVIDER 改为 aliyun 或 tencent 并配置对应凭据，或仅本地开发时设置 SMS_DEV_MODE=true。");
        }
        if ("log".equals(normalized)) {
            log.warn("[SMS] 当前使用 log 通道（sms.dev-mode=true），仅限开发/演示，不会真实发送短信");
        } else if (devMode) {
            log.warn("[SMS] sms.dev-mode=true：已选择 {} 通道，但不会真实发送短信", normalized);
        } else {
            log.info("[SMS] 短信通道已启用：{}", normalized);
        }
    }
}
