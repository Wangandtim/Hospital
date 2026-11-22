package com.hospital.service;

import com.hospital.entity.Hospital;
import com.hospital.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    public List<Hospital> findAll() {
        return hospitalRepository.findAll();
    }

    public List<Hospital> findActive() {
        return hospitalRepository.findByStatus(1);
    }

    public Hospital findById(String hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("医院不存在"));
    }

    public Hospital create(Hospital hospital) {
        if (hospital.getHospitalId() == null) {
            hospital.setHospitalId(nextHospitalId());
        }
        return hospitalRepository.save(hospital);
    }

    public Hospital update(String hospitalId, Hospital hospitalUpdate) {
        Hospital hospital = findById(hospitalId);

        if (hospitalUpdate.getHospitalName() != null) {
            hospital.setHospitalName(hospitalUpdate.getHospitalName());
        }
        if (hospitalUpdate.getAddress() != null) {
            hospital.setAddress(hospitalUpdate.getAddress());
        }
        if (hospitalUpdate.getPhone() != null) {
            hospital.setPhone(hospitalUpdate.getPhone());
        }
        if (hospitalUpdate.getLevel() != null) {
            hospital.setLevel(hospitalUpdate.getLevel());
        }
        if (hospitalUpdate.getDescription() != null) {
            hospital.setDescription(hospitalUpdate.getDescription());
        }
        if (hospitalUpdate.getLatitude() != null) {
            hospital.setLatitude(hospitalUpdate.getLatitude());
        }
        if (hospitalUpdate.getLongitude() != null) {
            hospital.setLongitude(hospitalUpdate.getLongitude());
        }
        if (hospitalUpdate.getStatus() != null) {
            hospital.setStatus(hospitalUpdate.getStatus());
        }
        // 更新地区字段
        if (hospitalUpdate.getProvince() != null) {
            hospital.setProvince(hospitalUpdate.getProvince());
        }
        if (hospitalUpdate.getCity() != null) {
            hospital.setCity(hospitalUpdate.getCity());
        }
        if (hospitalUpdate.getDistrict() != null) {
            hospital.setDistrict(hospitalUpdate.getDistrict());
        }
        if (hospitalUpdate.getNoticeEnabled() != null) {
            hospital.setNoticeEnabled(hospitalUpdate.getNoticeEnabled());
        }

        return hospitalRepository.save(hospital);
    }

    public void delete(String hospitalId) {
        hospitalRepository.deleteById(hospitalId);
    }

    // 按地区筛选医院
    public List<Hospital> findByProvince(String province) {
        return hospitalRepository.findByProvince(province);
    }

    public List<Hospital> findByCity(String city) {
        return hospitalRepository.findByCity(city);
    }

    public List<Hospital> findByProvinceAndCity(String province, String city) {
        return hospitalRepository.findByProvinceAndCity(province, city);
    }

    public List<Hospital> findByLocation(String province, String city, String district) {
        if (district != null && !district.isEmpty()) {
            return hospitalRepository.findByProvinceAndCityAndDistrict(province, city, district);
        } else if (city != null && !city.isEmpty()) {
            return hospitalRepository.findByProvinceAndCity(province, city);
        } else if (province != null && !province.isEmpty()) {
            return hospitalRepository.findByProvince(province);
        }
        return findActive();
    }

    // 分页查询医院（支持模糊搜索）
    public Map<String, Object> findByLocationWithPage(String province, String city, String district, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String keywordPattern = hasKeyword ? "%" + keyword.trim() + "%" : null;
        String provinceValue = (province != null && !province.trim().isEmpty()) ? province.trim() : null;
        String cityValue = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
        String districtValue = (district != null && !district.trim().isEmpty()) ? district.trim() : null;

        Page<Hospital> hospitalPage = hospitalRepository.searchPage(
                keywordPattern, provinceValue, cityValue, districtValue, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("content", hospitalPage.getContent());
        result.put("totalElements", hospitalPage.getTotalElements());
        result.put("totalPages", hospitalPage.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", size);
        result.put("hasNext", hospitalPage.hasNext());
        result.put("hasPrevious", hospitalPage.hasPrevious());

        return result;
    }

    // 查询所有地区列表（用于级联选择）
    public List<String> getAllProvinces() {
        return hospitalRepository.findDistinctProvinces();
    }

    public List<String> getCitiesByProvince(String province) {
        return hospitalRepository.findCitiesByProvince(province);
    }

    public List<String> getDistrictsByProvinceAndCity(String province, String city) {
        return hospitalRepository.findDistrictsByProvinceAndCity(province, city);
    }

    /**
     * 医院准入申请：匿名提交，统一进入待审核，不直接启用。
     */
    public Hospital apply(Hospital hospital) {
        if (hospital.getHospitalName() == null || hospital.getHospitalName().trim().isEmpty()) {
            throw new RuntimeException("请填写医院名称");
        }
        if (hospital.getHospitalId() == null || hospital.getHospitalId().isEmpty()) {
            hospital.setHospitalId(nextHospitalId());
        }
        hospital.setStatus(0);
        hospital.setCreateTime(LocalDateTime.now());
        return hospitalRepository.save(hospital);
    }

    /** 医院编号统一为 H001、H002…的连续编号，避免同一医院出现多种 ID 写法。 */
    private synchronized String nextHospitalId() {
        int max = 0;
        for (Hospital item : hospitalRepository.findAll()) {
            String id = item.getHospitalId();
            if (id != null && id.matches("H\\d{3,}")) {
                max = Math.max(max, Integer.parseInt(id.substring(1)));
            }
        }
        return String.format("H%03d", max + 1);
    }

    /**
     * 卫健委审批医院准入：1 通过启用，2 驳回。
     */
    public Hospital audit(String hospitalId, Integer status) {
        Hospital hospital = findById(hospitalId);
        hospital.setStatus(status);
        return hospitalRepository.save(hospital);
    }

    /**
     * 卫健委医院列表：包含待审核/已启用/已驳回，支持省市区与关键字筛选。
     */
    public Map<String, Object> findForAdmin(Integer status, String province, String city, String district,
                                            String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.ASC, "hospitalId"));

        Specification<Hospital> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (province != null && !province.isEmpty()) {
                predicates.add(cb.equal(root.get("province"), province));
            }
            if (city != null && !city.isEmpty()) {
                predicates.add(cb.equal(root.get("city"), city));
            }
            if (district != null && !district.isEmpty()) {
                predicates.add(cb.equal(root.get("district"), district));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("hospitalName"), like),
                        cb.like(root.get("address"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Hospital> hospitalPage = hospitalRepository.findAll(spec, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("content", hospitalPage.getContent());
        result.put("total", hospitalPage.getTotalElements());
        result.put("totalPages", hospitalPage.getTotalPages());
        result.put("page", page);
        result.put("size", safeSize);
        return result;
    }
}
