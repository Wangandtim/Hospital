package com.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 语音转写网关（可插拔）。
 *
 * 对接 OpenAI 兼容的转写接口：POST multipart（字段 file），返回 {"text": "..."}。
 * 该协议可直接对接 whisper.cpp server、faster-whisper-server、SenseVoice/FunASR 的
 * OpenAI 兼容封装，也可对接云厂商自建薄适配层。
 * 未配置 hospital.asr.url 时明确返回 enabled=false，前端降级为大按钮，不伪造识别结果。
 * 转写接口未返回 confidence 时按未知处理，不再使用固定默认值。
 */
@Service
public class AsrService {

    @Value("${hospital.asr.url:}")
    private String asrUrl;

    @Value("${hospital.asr.token:}")
    private String asrToken;

    @Value("${hospital.asr.model:FunAudioLLM/SenseVoiceSmall}")
    private String asrModel;

    @Value("${hospital.asr.timeout-ms:10000}")
    private int timeoutMs;

    @Autowired
    private TriageRuleEngine ruleEngine;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> transcribe(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (asrUrl == null || asrUrl.trim().isEmpty()) {
            result.put("success", false);
            result.put("enabled", false);
            result.put("message", "语音识别未启用，请使用常见不适按钮");
            return result;
        }
        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("enabled", true);
            result.put("message", "没有收到录音，请重说一遍");
            return result;
        }

        try {
            String response = post(file);
            Map<String, Object> parsed = parse(response);
            String text = parsed == null ? null : stringValue(parsed.get("text"));
            if (text == null && parsed != null) {
                text = stringValue(parsed.get("result"));
            }
            if (text == null || text.trim().isEmpty()) {
                result.put("success", false);
                result.put("enabled", true);
                result.put("message", "没有听清，请重说或点下面的按钮");
                return result;
            }

            String normalized = ruleEngine.normalize(text);
            TriageRuleEngine.Result facts = ruleEngine.assess(normalized, null, null, null, null, null);
            boolean hasSymptom = facts.redFlag || facts.deptName != null;
            double confidence = confidence(parsed);
            // 供应商未返回置信度时按未知处理，只依据文本长度和症状命中判断
            boolean lowConfidence = !hasSymptom || normalized.length() < 3
                    || (confidence >= 0 && confidence < 0.65);

            result.put("success", true);
            result.put("enabled", true);
            result.put("text", text.trim());
            result.put("normalized", normalized);
            result.put("confidence", confidence >= 0 ? confidence : null);
            result.put("hasSymptom", hasSymptom);
            result.put("lowConfidence", lowConfidence);
            result.put("hint", lowConfidence ? "没太听清，可以点下面的按钮" : "请确认是不是您说的");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("enabled", true);
            result.put("message", "语音识别暂时不可用，请点下面的按钮");
            return result;
        }
    }

    private String post(MultipartFile file) throws Exception {
        String boundary = "----HospitalAsr" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String filename = file.getOriginalFilename() == null ? "audio.mp3" : file.getOriginalFilename();
        String contentType = file.getContentType() == null ? "audio/mpeg" : file.getContentType();

        write(body, "--" + boundary + "\r\n");
        write(body, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        write(body, "Content-Type: " + contentType + "\r\n\r\n");
        body.write(file.getBytes());
        write(body, "\r\n");
        if (asrModel != null && !asrModel.trim().isEmpty()) {
            write(body, "--" + boundary + "\r\n");
            write(body, "Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            write(body, asrModel.trim() + "\r\n");
        }
        write(body, "--" + boundary + "--\r\n");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(asrUrl.trim()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        if (asrToken != null && !asrToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + asrToken.trim());
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
                .build();
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("ASR HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void write(ByteArrayOutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                return map;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 返回供应商给出的置信度（0-1）；未返回或无法解析时返回 -1，表示未知，不伪造默认值。
     */
    private double confidence(Map<String, Object> parsed) {
        if (parsed == null || parsed.get("confidence") == null) {
            return -1;
        }
        try {
            double value = Double.parseDouble(parsed.get("confidence").toString());
            return value > 1 ? value / 100.0 : value;
        } catch (Exception e) {
            return -1;
        }
    }
}
