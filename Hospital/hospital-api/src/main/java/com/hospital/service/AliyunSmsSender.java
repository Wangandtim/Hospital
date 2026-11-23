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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 阿里云短信（SendSms）直连实现，使用 HMAC-SHA1 RPC 签名。
 * 所有凭据、签名、模板来自环境变量；日志不打印验证码与 AccessKeySecret。
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;
    private static final DateTimeFormatter UTC_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${sms.dev-mode:false}")
    private boolean devMode;

    @Value("${sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.aliyun.sign-name:}")
    private String signName;

    @Value("${sms.aliyun.template-code:}")
    private String templateCode;

    @Value("${sms.aliyun.region-id:cn-hangzhou}")
    private String regionId;

    @Value("${sms.aliyun.endpoint:https://dysmsapi.aliyuncs.com/}")
    private String endpoint;

    @PostConstruct
    public void validateConfig() {
        if (devMode) {
            return;
        }
        if (blank(accessKeyId) || blank(accessKeySecret) || blank(signName) || blank(templateCode)) {
            throw new IllegalStateException("阿里云短信配置不完整：需要 ALIYUN_SMS_ACCESS_KEY_ID / "
                    + "ALIYUN_SMS_ACCESS_KEY_SECRET / ALIYUN_SMS_SIGN_NAME / ALIYUN_SMS_TEMPLATE_CODE");
        }
    }

    @Override
    public void send(String phone, String code) {
        if (devMode) {
            log.info("[SMS][aliyun] sms.dev-mode=true，跳过真实发送 phone={}", mask(phone));
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SendResult result = sendOnce(phone, code);
                if (result.success) {
                    log.info("[SMS][aliyun] 发送成功 phone={} requestId={}", mask(phone), result.requestId);
                    return;
                }
                log.warn("[SMS][aliyun] 发送失败 phone={} attempt={}/{} code={} requestId={}",
                        mask(phone), attempt, MAX_ATTEMPTS, result.code, result.requestId);
            } catch (Exception e) {
                log.warn("[SMS][aliyun] 请求异常 phone={} attempt={}/{} error={}",
                        mask(phone), attempt, MAX_ATTEMPTS, e.getClass().getSimpleName());
            }
            if (attempt < MAX_ATTEMPTS) {
                sleep(200L * attempt);
            }
        }
        throw new SmsSendException("短信发送失败，请稍后重试");
    }

    private SendResult sendOnce(String phone, String code) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("PhoneNumbers", phone);
        params.put("RegionId", regionId);
        params.put("SignName", signName);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
        params.put("Timestamp", UTC_TIMESTAMP.format(Instant.now()));
        params.put("Version", "2017-05-25");

        String canonical = canonicalQuery(params);
        String stringToSign = "POST&" + percentEncode("/") + "&" + percentEncode(canonical);
        String signature = hmacSha1Base64(accessKeySecret + "&", stringToSign);
        String body = canonical + "&Signature=" + percentEncode(signature);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return new SendResult(false, "http" + response.statusCode(), null);
        }
        JsonNode node = JSON.readTree(response.body());
        String respCode = node.path("Code").asText("");
        String requestId = node.path("RequestId").asText(null);
        String message = node.path("Message").asText("");
        if (!"OK".equals(respCode)) {
            log.debug("[SMS][aliyun] 厂商返回失败 code={} message={}", respCode, message);
            return new SendResult(false, respCode, requestId);
        }
        return new SendResult(true, respCode, requestId);
    }

    private static String canonicalQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(percentEncode(e.getKey())).append('=').append(percentEncode(e.getValue()));
        }
        return sb.toString();
    }

    static String percentEncode(String value) {
        String encoded = URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static String hmacSha1Base64(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
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
        return "aliyun";
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
