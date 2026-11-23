package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 腾讯云短信（SendSms，2021-01-11）直连实现，使用 TC3-HMAC-SHA256 签名。
 * 所有凭据、签名、模板来自环境变量；日志不打印验证码与 SecretKey。
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "tencent")
public class TencentSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsSender.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${sms.dev-mode:false}")
    private boolean devMode;

    @Value("${sms.tencent.secret-id:}")
    private String secretId;

    @Value("${sms.tencent.secret-key:}")
    private String secretKey;

    @Value("${sms.tencent.sdk-app-id:}")
    private String sdkAppId;

    @Value("${sms.tencent.sign-name:}")
    private String signName;

    @Value("${sms.tencent.template-id:}")
    private String templateId;

    @Value("${sms.tencent.region:ap-guangzhou}")
    private String region;

    @Value("${sms.tencent.endpoint:https://sms.tencentcloudapi.com/}")
    private String endpoint;

    @PostConstruct
    public void validateConfig() {
        if (devMode) {
            return;
        }
        if (blank(secretId) || blank(secretKey) || blank(sdkAppId) || blank(signName) || blank(templateId)) {
            throw new IllegalStateException("腾讯云短信配置不完整：需要 TENCENT_SMS_SECRET_ID / "
                    + "TENCENT_SMS_SECRET_KEY / TENCENT_SMS_SDK_APP_ID / TENCENT_SMS_SIGN_NAME / TENCENT_SMS_TEMPLATE_ID");
        }
    }

    @Override
    public void send(String phone, String code) {
        if (devMode) {
            log.info("[SMS][tencent] sms.dev-mode=true，跳过真实发送 phone={}", mask(phone));
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SendResult result = sendOnce(phone, code);
                if (result.success) {
                    log.info("[SMS][tencent] 发送成功 phone={} requestId={}", mask(phone), result.requestId);
                    return;
                }
                log.warn("[SMS][tencent] 发送失败 phone={} attempt={}/{} code={} requestId={}",
                        mask(phone), attempt, MAX_ATTEMPTS, result.code, result.requestId);
            } catch (Exception e) {
                log.warn("[SMS][tencent] 请求异常 phone={} attempt={}/{} error={}",
                        mask(phone), attempt, MAX_ATTEMPTS, e.getClass().getSimpleName());
            }
            if (attempt < MAX_ATTEMPTS) {
                sleep(200L * attempt);
            }
        }
        throw new SmsSendException("短信发送失败，请稍后重试");
    }

    private SendResult sendOnce(String phone, String code) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC).toString();
        URI uri = URI.create(endpoint);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("PhoneNumberSet", List.of(normalizePhone(phone)));
        payload.put("SmsSdkAppId", sdkAppId);
        payload.put("TemplateId", templateId);
        payload.put("SignName", signName);
        payload.put("TemplateParamSet", List.of(code));
        String payloadText = JSON.writeValueAsString(payload);

        String host = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        String canonicalRequest = "POST\n/\n\n"
                + "content-type:application/json\n"
                + "host:" + host + "\n\n"
                + "content-type;host\n"
                + sha256Hex(payloadText);
        String credentialScope = date + "/sms/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, "sms");
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = hex(hmac256(secretSigning, stringToSign));
        String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=content-type;host, Signature=" + signature;

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("X-TC-Action", "SendSms")
                .header("X-TC-Version", "2021-01-11")
                .header("X-TC-Timestamp", String.valueOf(timestamp))
                .header("Authorization", authorization);
        if (!blank(region)) {
            builder.header("X-TC-Region", region);
        }
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(payloadText, StandardCharsets.UTF_8)).build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return new SendResult(false, "http" + response.statusCode(), null);
        }
        JsonNode root = JSON.readTree(response.body()).path("Response");
        String requestId = root.path("RequestId").asText(null);
        if (root.has("Error")) {
            String errorCode = root.path("Error").path("Code").asText("error");
            log.debug("[SMS][tencent] 厂商返回失败 code={} message={}",
                    errorCode, root.path("Error").path("Message").asText(""));
            return new SendResult(false, errorCode, requestId);
        }
        JsonNode first = root.path("SendStatusSet").isArray() ? root.path("SendStatusSet").get(0) : null;
        if (first == null) {
            return new SendResult(false, "empty_status", requestId);
        }
        String statusCode = first.path("Code").asText("error");
        if ("Ok".equals(statusCode)) {
            return new SendResult(true, statusCode, requestId);
        }
        log.debug("[SMS][tencent] 发送状态失败 code={} message={}", statusCode, first.path("Message").asText(""));
        return new SendResult(false, statusCode, requestId);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        if (phone.startsWith("+")) {
            return phone;
        }
        return "+86" + phone;
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return hex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmac256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 11) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String provider() {
        return "tencent";
    }

    private static final class SendResult {
        private final boolean success;
        private final String code;
        private final String requestId;

        private SendResult(boolean success, String code, String requestId) {
            this.success = success;
            this.code = code;
            this.requestId = requestId;
        }
    }
}
