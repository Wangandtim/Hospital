package com.hospital.controller;

import com.hospital.entity.PatientFeedback;
import com.hospital.repository.PatientFeedbackRepository;
import com.hospital.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 患者反馈接口。
 * 目前仅提供提交（POST）；后续医院管理端可按 hospitalId 查询处理。
 */
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private PatientFeedbackRepository feedbackRepository;

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        Object ratingObj = body.get("rating");
        int rating = ratingObj instanceof Number ? ((Number) ratingObj).intValue() : 0;
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content")).trim();

        if (rating < 1 || rating > 5) {
            result.put("success", false);
            result.put("message", "请选择 1-5 星评分");
            return result;
        }
        if (content.isEmpty()) {
            result.put("success", false);
            result.put("message", "请填写反馈内容");
            return result;
        }
        if (content.length() > 500) {
            result.put("success", false);
            result.put("message", "反馈内容过长，请控制在 500 字以内");
            return result;
        }

        // 令牌归属优先，避免患者伪造他人身份
        String tokenPatientId = (String) request.getAttribute(AuthInterceptor.ATTR_OWNER_ID);
        String tokenRole = (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
        String patientId = "PATIENT".equals(tokenRole) && tokenPatientId != null
                ? tokenPatientId
                : (body.get("patientId") == null ? "" : String.valueOf(body.get("patientId")));

        PatientFeedback feedback = new PatientFeedback();
        feedback.setFeedbackId(UUID.randomUUID().toString().replace("-", ""));
        feedback.setPatientId(patientId);
        feedback.setHospitalId(toStr(body.get("hospitalId")));
        feedback.setHospitalName(toStr(body.get("hospitalName")));
        feedback.setAppointmentId(toStr(body.get("appointmentId")));
        feedback.setRating(rating);
        feedback.setTags(toStr(body.get("tags")));
        feedback.setContent(content);
        feedback.setContact(toStr(body.get("contact")));
        feedback.setStatus(0);
        feedback.setCreateTime(LocalDateTime.now());
        feedbackRepository.save(feedback);

        result.put("success", true);
        result.put("feedbackId", feedback.getFeedbackId());
        result.put("message", "反馈已提交，医院服务台会尽快查看");
        return result;
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
