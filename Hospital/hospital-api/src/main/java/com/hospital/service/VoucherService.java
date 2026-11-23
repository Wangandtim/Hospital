package com.hospital.service;

import com.hospital.entity.Appointment;
import com.hospital.entity.VoucherUse;
import com.hospital.repository.VoucherUseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/**
 * 就诊凭证：V1.{appointmentId}.{issuedAt}.{expiresAt}.{nonce}.{sig}
 * 签名 = HMAC-SHA256(secret, 前五段)，十六进制小写；nonce 一次性。
 */
@Service
public class VoucherService {

    public static final String CODE_INVALID_SIGNATURE = "VOUCHER_INVALID_SIGNATURE";
    public static final String CODE_EXPIRED = "VOUCHER_EXPIRED";
    public static final String CODE_USED = "VOUCHER_USED";
    public static final String CODE_WRONG_HOSPITAL = "VOUCHER_WRONG_HOSPITAL";
    public static final String CODE_STATUS_INVALID = "VOUCHER_STATUS_INVALID";

    @Value("${hospital.voucher.secret:}")
    private String secret;

    @Value("${hospital.voucher.ttl-minutes:30}")
    private int ttlMinutes;

    @Autowired
    private VoucherUseRepository voucherUseRepository;

    private final SecureRandom random = new SecureRandom();

    public static class VoucherException extends RuntimeException {
        private final String code;

        public VoucherException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class Payload {
        private final String appointmentId;
        private final long issuedAt;
        private final long expiresAt;
        private final String nonce;

        public Payload(String appointmentId, long issuedAt, long expiresAt, String nonce) {
            this.appointmentId = appointmentId;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.nonce = nonce;
        }

        public String getAppointmentId() { return appointmentId; }
        public long getIssuedAt() { return issuedAt; }
        public long getExpiresAt() { return expiresAt; }
        public String getNonce() { return nonce; }
    }

    /** 仅未完成且未过期（预约日期不早于今天）的预约可签发。 */
    public boolean canIssue(Appointment appointment) {
        return appointment != null
                && appointment.getStatus() != null && appointment.getStatus() == 1
                && appointment.getAppointmentDate() != null
                && !appointment.getAppointmentDate().isBefore(LocalDate.now());
    }

    public String issue(Appointment appointment) {
        if (appointment == null || !canIssue(appointment)) {
            return null;
        }
        if (secret == null || secret.trim().isEmpty()) {
            return null;
        }
        long issued = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        long expires = issued + Math.max(ttlMinutes, 1) * 60L;
        String nonce = randomNonce();
        String base = "V1." + appointment.getAppointmentId() + "." + issued + "." + expires + "." + nonce;
        return base + "." + sign(base);
    }

    /** 校验签名与过期时间，不校验使用状态与业务状态。 */
    public Payload verify(String voucher) {
        if (voucher == null || voucher.trim().isEmpty()) {
            throw new VoucherException(CODE_INVALID_SIGNATURE, "凭证不能为空");
        }
        String[] parts = voucher.trim().split("\\.", 6);
        if (parts.length != 6 || !"V1".equals(parts[0])) {
            throw new VoucherException(CODE_INVALID_SIGNATURE, "凭证格式不正确");
        }
        String base = String.join(".", parts[0], parts[1], parts[2], parts[3], parts[4]);
        String expected = sign(base);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[5].toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new VoucherException(CODE_INVALID_SIGNATURE, "凭证签名无效");
        }
        long issued;
        long expires;
        try {
            issued = Long.parseLong(parts[2]);
            expires = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            throw new VoucherException(CODE_INVALID_SIGNATURE, "凭证格式不正确");
        }
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        if (now > expires) {
            throw new VoucherException(CODE_EXPIRED, "凭证已过期，请重新获取");
        }
        return new Payload(parts[1], issued, expires, parts[4]);
    }

    public boolean isUsed(String nonce) {
        return voucherUseRepository.existsById(nonce);
    }

    /** 标记凭证已使用；重复使用时抛出 USED。 */
    public void consume(Payload payload, String doctorId) {
        if (voucherUseRepository.existsById(payload.getNonce())) {
            throw new VoucherException(CODE_USED, "该凭证已被使用");
        }
        VoucherUse use = new VoucherUse();
        use.setVoucherNonce(payload.getNonce());
        use.setAppointmentId(payload.getAppointmentId());
        use.setDoctorId(doctorId);
        use.setUsedAt(LocalDateTime.now());
        try {
            voucherUseRepository.saveAndFlush(use);
        } catch (DataIntegrityViolationException e) {
            throw new VoucherException(CODE_USED, "该凭证已被使用");
        }
    }

    public int getTtlMinutes() {
        return Math.max(ttlMinutes, 1);
    }

    private String sign(String base) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("凭证签名失败", e);
        }
    }

    private String randomNonce() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
