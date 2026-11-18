package com.hospital.controller;

import com.hospital.service.AsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 语音转写：小程序录音（≤30 秒）上传后统一走可插拔 ASR 网关。
 * 未配置 ASR 时返回 enabled=false，由前端降级为「常见不适」大按钮。
 */
@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    @Autowired
    private AsrService asrService;

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> transcribe(@RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            return asrService.transcribe(file);
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("enabled", true);
            error.put("message", "语音识别暂时不可用，请点下面的按钮");
            return error;
        }
    }
}
