package com.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 日志短信通道：只把验证码写入服务端日志，不真正发送。
 * 仅在 sms.provider=log（且 sms.dev-mode=true）时启用；
 * 生产环境误配 log + dev-mode=false 会由 SmsProviderGuard 拒绝启动。
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public void send(String phone, String code) {
        log.info("[SMS][log] 手机号 {} 的验证码为 {}（仅日志通道，未真实发送）", mask(phone), code);
    }

    @Override
    public String provider() {
        return "log";
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 11) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
