package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 手机验证码服务（单实例内存实现）。
 * 约束：5 分钟有效、60 秒重发冷却、同一手机号每日 10 条上限、最多 5 次校验尝试、校验成功即失效。
 */
@Service
public class SmsCodeService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int DAILY_LIMIT = 10;

    @Value("${sms.code-ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${sms.resend-interval-seconds:60}")
    private int resendIntervalSeconds;

    @Autowired
    private SmsSender smsSender;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /**
     * 生成并发送验证码。
     *
     * @return 成功返回 null，失败返回可直接展示给用户的错误文案
     */
    public String sendCode(String phone) {
        if (!isValidPhone(phone)) {
            return "手机号格式不正确";
        }

        long now = System.currentTimeMillis();
        purgeExpired(now);

        CodeEntry existing = codes.get(phone);
        if (existing != null) {
            long waitSeconds = (existing.lastSendAt + resendIntervalSeconds * 1000L - now) / 1000;
            if (waitSeconds > 0) {
                return "请 " + waitSeconds + " 秒后再获取验证码";
            }
        }

        long today = now / 86_400_000L;
        int sentToday = existing != null && existing.dayStamp == today ? existing.sendCount : 0;
        if (sentToday >= DAILY_LIMIT) {
            return "该手机号今日获取验证码次数过多，请明天再试";
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        if (!smsSender.send(phone, code)) {
            return "验证码发送失败，请稍后重试";
        }

        codes.put(phone, new CodeEntry(code, now + ttlSeconds * 1000L, now, sentToday + 1, today));
        return null;
    }

    /**
     * 校验验证码，成功即失效。
     */
    public boolean verify(String phone, String code) {
        if (!isValidPhone(phone) || code == null || code.length() != 6) {
            return false;
        }

        CodeEntry entry = codes.get(phone);
        if (entry == null) {
            return false;
        }
        if (entry.expired()) {
            codes.remove(phone);
            return false;
        }
        if (entry.attempts >= MAX_VERIFY_ATTEMPTS) {
            codes.remove(phone);
            return false;
        }
        if (!entry.code.equals(code)) {
            entry.attempts++;
            return false;
        }

        codes.remove(phone);
        return true;
    }

    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /** 仅开发模式用于回显验证码，生产环境不应调用。 */
    public String peekCode(String phone) {
        CodeEntry entry = codes.get(phone);
        return entry == null || entry.expired() ? null : entry.code;
    }

    private void purgeExpired(long now) {
        if (codes.size() < 1000) {
            return;
        }
        Iterator<Map.Entry<String, CodeEntry>> iterator = codes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expired()) {
                iterator.remove();
            }
        }
    }

    private static final class CodeEntry {
        private final String code;
        private final long expireAt;
        private final long lastSendAt;
        private final int sendCount;
        private final long dayStamp;
        private int attempts;

        private CodeEntry(String code, long expireAt, long lastSendAt, int sendCount, long dayStamp) {
            this.code = code;
            this.expireAt = expireAt;
            this.lastSendAt = lastSendAt;
            this.sendCount = sendCount;
            this.dayStamp = dayStamp;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
