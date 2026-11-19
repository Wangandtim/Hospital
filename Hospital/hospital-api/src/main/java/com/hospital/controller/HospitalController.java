package com.hospital.controller;

import com.hospital.entity.Hospital;
import com.hospital.service.CatalogService;
import com.hospital.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "*")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private com.hospital.repository.ScheduleRepository scheduleRepository;

    @GetMapping
    public List<Hospital> list() {
        return hospitalService.findActive();
    }

    /** 医院准入申请（公开）：提交后进入待审核，由卫健委审批。 */
    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Hospital hospital) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            Hospital created = hospitalService.apply(hospital);
            result.put("success", true);
            result.put("message", "医院准入申请已提交，待卫健委审核");
            result.put("hospitalId", created.getHospitalId());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{hospitalId}/navigation")
    public Map<String, Object> navigation(@PathVariable String hospitalId) {
        return catalogService.navigation(hospitalId);
    }

    @GetMapping("/{hospitalId}")
    public Map<String, Object> getById(@PathVariable String hospitalId) {
        Hospital hospital = hospitalService.findById(hospitalId);
        Map<String, Object> detail = catalogService.hospitalDetail(hospital);
        detail.put("status", hospital.getStatus());
        detail.put("createTime", hospital.getCreateTime());
        return detail;
    }

    @PostMapping
    public Hospital create(@RequestBody Hospital hospital) {
        return hospitalService.create(hospital);
    }

    @PutMapping("/{hospitalId}")
    public Hospital update(@PathVariable String hospitalId, @RequestBody Hospital hospital) {
        return hospitalService.update(hospitalId, hospital);
    }

    @DeleteMapping("/{hospitalId}")
    public void delete(@PathVariable String hospitalId) {
        hospitalService.delete(hospitalId);
    }

    // 按地区筛选接口
    @GetMapping("/filter")
    public List<Hospital> filterByLocation(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district) {
        return hospitalService.findByLocation(province, city, district);
    }

    @GetMapping("/by-province/{province}")
    public List<Hospital> listByProvince(@PathVariable String province) {
        return hospitalService.findByProvince(province);
    }

    @GetMapping("/by-city/{city}")
    public List<Hospital> listByCity(@PathVariable String city) {
        return hospitalService.findByCity(city);
    }

    // 分页查询接口（支持模糊搜索）
    @GetMapping("/page")
    public Map<String, Object> listWithPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        int safePage = catalogService.page(page);
        int safeSize = catalogService.size(size);
        Map<String, Object> result = hospitalService.findByLocationWithPage(
                province, city, district, keyword, safePage, safeSize);
        @SuppressWarnings("unchecked")
        List<Hospital> hospitals = (List<Hospital>) result.get("content");
        List<Map<String, Object>> items = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate tomorrow = today.plusDays(1);
        for (Hospital hospital : hospitals) {
            items.add(catalogService.hospitalCard(hospital,
                    scheduleRepository.countAvailable(hospital.getHospitalId(), today) > 0,
                    scheduleRepository.countAvailable(hospital.getHospitalId(), tomorrow) > 0,
                    lat, lng));
        }
        long total = ((Number) result.getOrDefault("totalElements", 0)).longValue();
        Map<String, Object> envelope = catalogService.envelope(items, safePage, safeSize, total);
        result.put("content", items);
        result.put("data", items);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        result.put("hasMore", envelope.get("hasMore"));
        result.put("totalPages", envelope.get("totalPages"));
        result.put("meta", envelope.get("meta"));
        return result;
    }

    private Map<String, Boolean> availableMap(java.time.LocalDate date) {
        Map<String, Boolean> map = new java.util.HashMap<>();
        for (Object[] row : scheduleRepository.countAvailableByHospital(date)) {
            map.put((String) row[0], ((Number) row[1]).longValue() > 0);
        }
        return map;
    }
    // 获取省份列表（用于级联选择）
    @GetMapping("/provinces")
    public List<String> getProvinces() {
        return hospitalService.getAllProvinces();
    }

    // 根据省份获取城市列表
    @GetMapping("/cities")
    public List<String> getCities(@RequestParam String province) {
        return hospitalService.getCitiesByProvince(province);
    }

    // 根据省份和城市获取区县列表
    @GetMapping("/districts")
    public List<String> getDistricts(
            @RequestParam String province,
            @RequestParam String city) {
        return hospitalService.getDistrictsByProvinceAndCity(province, city);
    }
}
