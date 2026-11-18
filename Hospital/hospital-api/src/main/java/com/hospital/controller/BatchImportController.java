package com.hospital.controller;

import com.hospital.security.AuthContext;
import com.hospital.service.BatchImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量导入：医院管理员可导入本院医生；卫健委总管理员可导入各医院管理员。
 */
@RestController
@RequestMapping("/api/batch-import")
@CrossOrigin(origins = "*")
public class BatchImportController {

    @Autowired
    private BatchImportService batchImportService;

    @Autowired
    private AuthContext authContext;

    @PostMapping("/doctors")
    public Map<String, Object> importDoctors(@RequestBody Map<String, Object> body) {
        try {
            String requested = text(body.get("hospitalId"));
            String hospitalId = authContext.resolveHospitalId(requested);
            List<Map<String, Object>> rows = rows(body.get("rows"));
            return batchImportService.importDoctors(hospitalId, rows);
        } catch (RuntimeException e) {
            return fail(e.getMessage());
        }
    }

    @PostMapping("/admins")
    public Map<String, Object> importAdmins(@RequestBody Map<String, Object> body) {
        try {
            if (!authContext.isSuperAdmin()) {
                return fail("仅卫健委总管理员可批量导入医院管理员");
            }
            List<Map<String, Object>> rows = rows(body.get("rows"));
            return batchImportService.importAdmins(rows);
        } catch (RuntimeException e) {
            return fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Object value) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                }
            }
        }
        return list;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message == null ? "导入失败" : message);
        return result;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
