package com.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 默认短信通道：不真正发短信，只把验证码写入服务端日志。
 * 生产环境请替换为短信平台实现（如阿里云 / 腾讯云），并关闭 sms.dev-mode。
 */
@Service
public class LogSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public boolean send(String phone, String code) {
        log.info("[SMS] 手机号 {} 的验证码为 {}（未配置短信通道，仅打印日志）", mask(phone), code);
        return true;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 11) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
