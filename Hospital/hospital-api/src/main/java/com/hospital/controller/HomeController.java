package com.hospital.controller;

import com.hospital.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 首页聚合（Direction A）。
 * 真实字段来自各业务表，缺失字段由 demo mock 补齐；响应 meta.mock / mockFields 可识别。
 */
@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "*")
public class HomeController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping
    public Map<String, Object> home(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String patientId) {
        return catalogService.home(hospitalId, lat, lng, patientId);
    }
}
