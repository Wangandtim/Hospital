package com.hospital.controller;

import com.hospital.entity.HospitalArticle;
import com.hospital.security.AuthContext;
import com.hospital.service.HospitalArticleService;
import com.hospital.util.HospitalIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    /** 患者端：按医院读取已发布内容（未登录也可访问） */
    @GetMapping
    public List<HospitalArticle> list(@RequestParam String hospitalId) {
        return service.listPublished(HospitalIdUtils.normalizeHospitalId(hospitalId));
    }

    /** 患者端公告分页：带发布单位/阅读数（mock 标记）。 */
    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam String hospitalId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String keyword) {
        String normalized = HospitalIdUtils.normalizeHospitalId(hospitalId);
        List<HospitalArticle> all = service.searchPublished(normalized, keyword);
        if (category != null && !category.isEmpty()) {
            all = all.stream().filter(a -> category.equals(a.getCategory())).collect(java.util.stream.Collectors.toList());
        }
        all.sort(java.util.Comparator
                .comparing(HospitalArticle::getCreateTime, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(HospitalArticle::getArticleId));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (HospitalArticle article : all.subList(from, to)) {
            items.add(catalogService.articleCard(article));
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("data", items);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", all.size());
        result.put("totalPages", (int) Math.ceil((double) all.size() / safeSize));
        result.put("hasMore", (long) (safePage + 1) * safeSize < all.size());
        result.put("meta", Map.of("mock", catalogService.mockEnabled()));
        return result;
    }

    /** 管理端：读本院全部内容（含下架） */
    @GetMapping("/manage")
    public List<HospitalArticle> listForManage(@RequestParam String hospitalId) {
        requireAdmin();
        String normalized = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalized);
        return service.listAll(normalized);
    }

    /** 管理端：读本院公告开关（院管只能读本院）。 */
    @GetMapping("/notice-switch")
    public Map<String, Object> getNoticeSwitch(@RequestParam(required = false) String hospitalId) {
        Map<String, Object> result = new LinkedHashMap<>();
        requireAdmin();
        String target = hospitalId == null || hospitalId.isEmpty()
                ? authContext.hospitalId()
                : HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(target);
        result.put("success", true);
        result.put("hospitalId", target);
        result.put("noticeEnabled", service.isNoticeEnabled(target));
        return result;
    }

    /** 管理端：开/关本院公告。院管只能改本院，超管任意医院。 */
    @PutMapping("/notice-switch")
    public Map<String, Object> updateNoticeSwitch(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            requireAdmin();
            String rawHospitalId = body.get("hospitalId") == null ? null : String.valueOf(body.get("hospitalId"));
            String target = rawHospitalId == null || rawHospitalId.isEmpty()
                    ? authContext.hospitalId()
                    : HospitalIdUtils.normalizeHospitalId(rawHospitalId);
            authContext.requireHospitalScope(target);
            Object enabledValue = body.get("enabled");
            if (enabledValue == null) {
                throw new RuntimeException("缺少 enabled 参数");
            }
            boolean enabled = Boolean.parseBoolean(String.valueOf(enabledValue));
            service.setNoticeEnabled(target, enabled);
            result.put("success", true);
            result.put("hospitalId", target);
            result.put("noticeEnabled", enabled);
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (com.hospital.exception.NotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{articleId}")
    public HospitalArticle getById(@PathVariable String articleId) {
        return service.findPublishedById(articleId);
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
