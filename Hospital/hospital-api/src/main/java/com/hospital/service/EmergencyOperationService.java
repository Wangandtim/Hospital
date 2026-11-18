package com.hospital.service;

import com.hospital.entity.EmergencyOperation;
import com.hospital.entity.EmergencyRequest;
import com.hospital.repository.EmergencyOperationRepository;
import com.hospital.repository.EmergencyRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 记录应急通道使用期间的写操作，供结束报告自动生成改动范围。 */
@Service
public class EmergencyOperationService {

    private static final Pattern DEPT_PATTERN = Pattern.compile("^/api/departments/([^/]+)$");
    private static final Pattern SCHEDULE_PATTERN = Pattern.compile("^/api/schedules/([^/]+)$");

    @Autowired
    private EmergencyOperationRepository operationRepository;

    @Autowired
    private EmergencyRequestRepository requestRepository;

    public void record(String token, String hospitalId, String method, String path) {
        try {
            if (path == null) {
                return;
            }
            EmergencyOperation operation = new EmergencyOperation();
            operation.setId("OP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
            operation.setHospitalId(hospitalId);
            operation.setToken(token);
            operation.setMethod(method);
            operation.setPath(path);
            operation.setCreateTime(LocalDateTime.now());

            Optional<EmergencyRequest> request = requestRepository.findByToken(token).stream().findFirst();
            if (request.isPresent()) {
                operation.setRequestId(request.get().getId());
                operation.setDoctorId(request.get().getDoctorId());
            }

            fillTarget(operation, path);
            operationRepository.save(operation);
        } catch (Exception e) {
            // 操作记录失败不阻断应急业务
            System.out.println("应急操作记录失败: " + e.getMessage());
        }
    }

    public List<EmergencyOperation> listByRequestId(String requestId) {
        return operationRepository.findByRequestIdOrderByCreateTimeAsc(requestId);
    }

    private void fillTarget(EmergencyOperation operation, String path) {
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        Matcher deptMatcher = DEPT_PATTERN.matcher(cleanPath);
        if (deptMatcher.matches()) {
            operation.setTargetType("department");
            operation.setTargetId(deptMatcher.group(1));
            operation.setSummary(actionText(operation.getMethod()) + "科室（" + operation.getTargetId() + "）");
            return;
        }
        Matcher scheduleMatcher = SCHEDULE_PATTERN.matcher(cleanPath);
        if (scheduleMatcher.matches()) {
            operation.setTargetType("schedule");
            operation.setTargetId(scheduleMatcher.group(1));
            operation.setSummary(actionText(operation.getMethod()) + "排班（" + operation.getTargetId() + "）");
            return;
        }
        operation.setSummary(actionText(operation.getMethod()) + "应急数据");
    }

    private String actionText(String method) {
        if (method == null) {
            return "操作";
        }
        switch (method.toUpperCase()) {
            case "POST": return "新增";
            case "PUT":
            case "PATCH": return "修改";
            case "DELETE": return "删除";
            default: return "操作";
        }
    }
}
