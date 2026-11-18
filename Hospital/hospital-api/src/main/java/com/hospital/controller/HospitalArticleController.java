package com.hospital.controller;

import com.hospital.entity.HospitalArticle;
import com.hospital.security.AuthContext;
import com.hospital.service.HospitalArticleService;
import com.hospital.util.HospitalIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospital-articles")
@CrossOrigin(origins = "*")
public class HospitalArticleController {

    @Autowired
    private HospitalArticleService service;

    @Autowired
    private AuthContext authContext;

    /** 患者端：按医院读取已发布内容（未登录也可访问） */
    @GetMapping
    public List<HospitalArticle> list(@RequestParam String hospitalId) {
        return service.listPublished(HospitalIdUtils.normalizeHospitalId(hospitalId));
    }

    /** 管理端：读本院全部内容（含下架） */
    @GetMapping("/manage")
    public List<HospitalArticle> listForManage(@RequestParam String hospitalId) {
        requireAdmin();
        String normalized = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalized);
        return service.listAll(normalized);
    }

    @GetMapping("/{articleId}")
    public HospitalArticle getById(@PathVariable String articleId) {
        return service.findById(articleId);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody HospitalArticle article) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireAdmin();
            if (article.getHospitalId() == null || article.getHospitalId().isEmpty()) {
                article.setHospitalId(authContext.hospitalId());
            } else {
                article.setHospitalId(authContext.resolveHospitalId(article.getHospitalId()));
            }
            if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
                throw new RuntimeException("标题不能为空");
            }
            result.put("success", true);
            result.put("data", service.create(article));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{articleId}")
    public Map<String, Object> update(@PathVariable String articleId, @RequestBody HospitalArticle article) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireAdmin();
            HospitalArticle existing = service.findById(articleId);
            authContext.requireHospitalScope(existing.getHospitalId());
            result.put("success", true);
            result.put("data", service.update(articleId, article));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{articleId}")
    public Map<String, Object> delete(@PathVariable String articleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireAdmin();
            HospitalArticle existing = service.findById(articleId);
            authContext.requireHospitalScope(existing.getHospitalId());
            service.delete(articleId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private void requireAdmin() {
        if (!authContext.isAdmin()) {
            throw new RuntimeException("仅管理员可操作");
        }
    }
}
