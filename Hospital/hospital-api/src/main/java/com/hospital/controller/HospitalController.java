package com.hospital.controller;

import com.hospital.entity.Hospital;
import com.hospital.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "*")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

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

    @GetMapping("/{hospitalId}")
    public Hospital getById(@PathVariable String hospitalId) {
        return hospitalService.findById(hospitalId);
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword) {
        return hospitalService.findByLocationWithPage(province, city, district, keyword, page, size);
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
