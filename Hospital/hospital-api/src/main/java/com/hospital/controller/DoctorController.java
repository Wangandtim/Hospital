package com.hospital.controller;

import com.hospital.entity.Doctor;
import com.hospital.entity.Department;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorService;
import com.hospital.util.HospitalIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    @Autowired
    private com.hospital.service.ScheduleService scheduleService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private com.hospital.service.DoctorDetailService doctorDetailService;

    @Autowired
    private com.hospital.repository.DoctorCertificateRepository certificateRepository;

    /** 患者端只展示已启用医生，待审核/已拒绝的账号不下发。 */
    private List<Doctor> activeOnly(List<Doctor> doctors) {
        return doctors.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 1)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String deptId) {
        List<Doctor> doctors;
        if (deptId != null && !deptId.isEmpty()) {
            doctors = authContext.isHospitalAdmin()
                    ? doctorService.findByDeptIdAndHospitalId(deptId, authContext.hospitalId())
                    : doctorService.findByDeptId(deptId);
        } else {
            doctors = authContext.isHospitalAdmin()
                    ? doctorService.findByHospitalId(authContext.hospitalId())
                    : doctorService.findAll();
        }
        return convertList(activeOnly(doctors));
    }

    // 通过医院查询
    @GetMapping("/hospital/{hospitalId}")
    public List<Map<String, Object>> listByHospital(@PathVariable String hospitalId) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        return convertList(activeOnly(doctorService.findByHospitalId(normalizedId)));
    }

    /** 医生分页（按医院）：职称/年限/擅长/标签/挂号费/今日余号。 */
    @GetMapping("/hospital/{hospitalId}/page")
    public Map<String, Object> pageByHospital(
            @PathVariable String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String keyword) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        List<Doctor> doctors = doctorService.findByHospitalId(normalizedId);
        return pageDoctors(doctors, deptId, keyword, page, size, normalizedId);
    }

    /** 医生分页（按科室）。 */
    @GetMapping("/dept/{deptId}/page")
    public Map<String, Object> pageByDept(
            @PathVariable String deptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        List<Doctor> doctors = doctorService.findByDeptId(deptId);
        String hospitalId = doctors.isEmpty() ? null : doctors.get(0).getHospitalId();
        if (hospitalId != null) {
            authContext.requireHospitalScope(hospitalId);
        }
        return pageDoctors(doctors, null, keyword, page, size, hospitalId);
    }

    private Map<String, Object> pageDoctors(List<Doctor> doctors, String deptId, String keyword,
                                            int page, int size, String hospitalId) {
        doctors = activeOnly(doctors);
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, Integer> remain = hospitalId == null
                ? new HashMap<>()
                : catalogService.doctorRemaining(hospitalId, today);
        Map<String, String> deptNames = new HashMap<>();
        for (Department d : departmentRepository.findAll()) {
            deptNames.put(d.getDeptId(), d.getName());
        }
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (Doctor doctor : doctors) {
            if (doctor.getStatus() != null && doctor.getStatus() != 1) {
                continue;
            }
            if (deptId != null && !deptId.isEmpty() && !deptId.equals(doctor.getDeptId())) {
                continue;
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                boolean match = (doctor.getName() != null && doctor.getName().contains(kw))
                        || (doctor.getExpertise() != null && doctor.getExpertise().contains(kw))
                        || (deptNames.get(doctor.getDeptId()) != null && deptNames.get(doctor.getDeptId()).contains(kw));
                if (!match) {
                    continue;
                }
            }
            items.add(catalogService.doctorCard(doctor, deptNames.get(doctor.getDeptId()),
                    remain.getOrDefault(doctor.getDoctorId(), 0), null));
        }
        Map<String, String> photos = doctorDetailService.photoUrls(
                doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList()));
        items.forEach(item -> item.put("photoUrl", photos.get(String.valueOf(item.get("doctorId")))));
        items.sort(java.util.Comparator
                .comparingInt((Map<String, Object> d) -> titleRank((String) d.get("title")))
                .thenComparing(d -> String.valueOf(d.get("name")))
                .thenComparing(d -> String.valueOf(d.get("doctorId"))));
        int safePage = catalogService.page(page);
        int safeSize = catalogService.size(size);
        long total = items.size();
        return catalogService.envelope(catalogService.slice(items, safePage, safeSize), safePage, safeSize, total);
    }

    private int titleRank(String title) {
        if (title == null) return 9;
        if (title.contains("主任医师") && !title.contains("副主任")) return 0;
        if (title.contains("副主任医师")) return 1;
        if (title.contains("主治")) return 2;
        return 3;
    }

    /** 批量转换：一次性取出科室名称，避免逐条查询导致的 N+1 慢查询。 */
    private List<Map<String, Object>> convertList(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        Map<String, String> deptNameMap = buildDeptNameMap();
        Map<String, String> photos = doctorDetailService.photoUrls(
                doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList()));
        return doctors.stream()
                .map(doctor -> {
                    Map<String, Object> item = convertToMapWithDeptName(doctor, deptNameMap);
                    item.put("photoUrl", photos.get(doctor.getDoctorId()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Map<String, String> buildDeptNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Department department : departmentRepository.findAll()) {
            map.put(department.getDeptId(), department.getName());
        }
        return map;
    }

    // 通过id查询
    @GetMapping("/{doctorId}")
    public Map<String, Object> getById(@PathVariable String doctorId) {
        Doctor doctor = doctorService.findById(doctorId);
        if (doctor == null) return null;
        authContext.requireHospitalScope(doctor.getHospitalId());
        String deptName = getDeptName(doctor.getDeptId());
        Integer remain = catalogService.doctorRemaining(doctor.getHospitalId(), java.time.LocalDate.now())
                .getOrDefault(doctorId, 0);
        List<com.hospital.dto.ScheduleDetailDTO> schedules = scheduleService.findByDoctorAndDateRangeDetail(
                doctorId, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(6));
        Map<String, Object> detail = catalogService.doctorDetail(doctor, deptName, remain, schedules);
        detail.put("status", doctor.getStatus());
        detail.put("username", doctor.getUsername());
        return detail;
    }
    
    private Map<String, Object> convertToMapWithDeptName(Doctor doctor) {
        return convertToMapWithDeptName(doctor, null);
    }

    private Map<String, Object> convertToMapWithDeptName(Doctor doctor, Map<String, String> deptNameMap) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", doctor.getDoctorId());
        result.put("doctorId", doctor.getDoctorId());
        result.put("name", doctor.getName());
        result.put("doctorName", doctor.getName());
        result.put("title", doctor.getTitle());
        result.put("hospitalId", doctor.getHospitalId());
        result.put("deptId", doctor.getDeptId());
        if (authContext.isAdmin()) {
            result.put("doctorNo", doctor.getDoctorNo());
        }
        
        // 获取科室名称（列表接口传入批量映射，避免逐条查询）
        String deptName;
        if (deptNameMap != null) {
            deptName = doctor.getDeptId() == null || doctor.getDeptId().isEmpty()
                    ? "未填写"
                    : deptNameMap.getOrDefault(doctor.getDeptId(), doctor.getDeptId());
        } else {
            deptName = getDeptName(doctor.getDeptId());
        }
        result.put("department", deptName);
        result.put("deptName", deptName);
        
        result.put("expertise", doctor.getExpertise());
        result.put("introduction", doctor.getExperience());
        result.put("gender", doctor.getGender());
        result.put("consultationFee", doctor.getConsultationFee());
        result.put("status", doctor.getStatus());
        result.put("username", doctor.getUsername());
        result.put("phone", doctor.getPhone());
        
        return result;
    }
    
    private String getDeptName(String deptId) {
        if (deptId == null || deptId.isEmpty()) {
            return "未填写";
        }
        Optional<Department> dept = departmentRepository.findById(deptId);
        return dept.map(Department::getName).orElse(deptId);
    }

    @GetMapping("/dept/{deptId}")
    public List<Map<String, Object>> getByDept(@PathVariable String deptId) {
        if (authContext.isHospitalAdmin()) {
            return convertList(activeOnly(doctorService.findByDeptIdAndHospitalId(deptId, authContext.hospitalId())));
        }
        return convertList(activeOnly(doctorService.findByDeptId(deptId)));
    }

    @GetMapping("/dept/{deptId}/hospital/{hospitalId}")
    public List<Map<String, Object>> getByDeptAndHospital(
            @PathVariable String deptId,
            @PathVariable String hospitalId) {
        String normalizedId = HospitalIdUtils.normalizeHospitalId(hospitalId);
        authContext.requireHospitalScope(normalizedId);
        return convertList(activeOnly(doctorService.findByDeptIdAndHospitalId(deptId, normalizedId)));
    }

    /** 管理端：历史医生工号批量补录（可空；不伪造）。 */
    @PostMapping("/doctor-no/batch")
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchDoctorNo(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            if (!authContext.isAdmin()) {
                throw new com.hospital.exception.ForbiddenException("仅管理员可补录工号");
            }
            String hospitalId = authContext.resolveHospitalId(str(body.get("hospitalId")));
            if (hospitalId == null || hospitalId.isEmpty()) {
                throw new RuntimeException("请指定医院");
            }
            List<Map<String, Object>> rows = new java.util.ArrayList<>();
            Object rawRows = body.get("rows");
            if (rawRows instanceof List) {
                for (Object item : (List<Object>) rawRows) {
                    if (item instanceof Map) {
                        rows.add((Map<String, Object>) item);
                    }
                }
            }
            if (rows.isEmpty()) {
                throw new RuntimeException("没有可补录的数据");
            }
            if (rows.size() > 200) {
                throw new RuntimeException("单次最多补录 200 条");
            }
            int successCount = 0;
            List<Map<String, Object>> errors = new java.util.ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                try {
                    String doctorId = str(row.get("doctorId"));
                    String username = str(row.get("username"));
                    Doctor doctor = doctorId != null
                            ? doctorService.findById(doctorId)
                            : doctorRepository.findByUsername(username)
                                    .orElseThrow(() -> new RuntimeException("医生不存在"));
                    authContext.requireHospitalScope(doctor.getHospitalId());
                    String no = str(row.get("doctorNo"));
                    if (no != null && !no.equals(doctor.getDoctorNo())
                            && doctorRepository.existsByHospitalIdAndDoctorNo(doctor.getHospitalId(), no)) {
                        throw new RuntimeException("工号已被使用：" + no);
                    }
                    doctor.setDoctorNo(no);
                    doctorRepository.save(doctor);
                    successCount++;
                } catch (com.hospital.exception.ForbiddenException e) {
                    throw e;
                } catch (RuntimeException e) {
                    Map<String, Object> err = new java.util.LinkedHashMap<>();
                    err.put("row", i + 1);
                    err.put("message", e.getMessage());
                    errors.add(err);
                }
            }
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("failCount", rows.size() - successCount);
            result.put("errors", errors);
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 管理端：读医生介绍（doctor_detail），无记录时字段为 null。 */
    @GetMapping("/{doctorId}/detail")
    public Map<String, Object> getDoctorDetail(@PathVariable String doctorId) {
        if (!authContext.isAdmin()) {
            throw new com.hospital.exception.ForbiddenException("仅管理员可查看医生介绍");
        }
        Doctor doctor = doctorService.findById(doctorId);
        authContext.requireHospitalScope(doctor.getHospitalId());
        com.hospital.entity.DoctorDetail detail = doctorDetailService.findByDoctorId(doctorId);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("success", true);
        map.put("doctorId", doctorId);
        map.put("exists", detail != null);
        map.put("introduction", detail == null ? null : detail.getIntroduction());
        map.put("expertiseDesc", detail == null ? null : detail.getExpertiseDesc());
        map.put("workExperience", detail == null ? null : detail.getWorkExperience());
        map.put("education", detail == null ? null : detail.getEducation());
        map.put("awards", detail == null ? null : detail.getAwards());
        map.put("qualification", detail == null ? null : detail.getQualification());
        map.put("academicAppointments", detail == null ? null : detail.getAcademicAppointments());
        map.put("photoUrl", detail == null ? null : detail.getPhotoUrl());
        map.put("consultationFee", detail == null ? doctor.getConsultationFee() : detail.getConsultationFee());
        return map;
    }

    /** 管理端：保存医生介绍；空值即清空，不生成任何默认文案。 */
    @PutMapping("/{doctorId}/detail")
    public Map<String, Object> saveDoctorDetail(@PathVariable String doctorId,
                                                @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            if (!authContext.isAdmin()) {
                throw new com.hospital.exception.ForbiddenException("仅管理员可维护医生介绍");
            }
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());
            com.hospital.entity.DoctorDetail detail = new com.hospital.entity.DoctorDetail();
            detail.setIntroduction(str(body.get("introduction")));
            detail.setExpertiseDesc(str(body.get("expertiseDesc")));
            detail.setWorkExperience(str(body.get("workExperience")));
            detail.setEducation(str(body.get("education")));
            detail.setAwards(str(body.get("awards")));
            detail.setQualification(str(body.get("qualification")));
            detail.setAcademicAppointments(str(body.get("academicAppointments")));
            detail.setPhotoUrl(str(body.get("photoUrl")));
            Object fee = body.get("consultationFee");
            detail.setConsultationFee(fee == null || String.valueOf(fee).trim().isEmpty()
                    ? null : Integer.parseInt(String.valueOf(fee).trim()));
            doctorDetailService.saveForDoctor(doctorId, detail);
            result.put("success", true);
            result.put("doctorId", doctorId);
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 医生荣誉证书列表（按 sort_order 升序）。 */
    @GetMapping("/{doctorId}/certificates")
    public List<Map<String, Object>> listCertificates(@PathVariable String doctorId) {
        doctorService.findById(doctorId);
        return certificateRepository.findByDoctorIdOrderBySortOrderAscCreateTimeAsc(doctorId)
                .stream().map(this::certificateCard).collect(Collectors.toList());
    }

    /** 新增证书（仅本院管理员）。 */
    @PostMapping("/{doctorId}/certificates")
    public Map<String, Object> createCertificate(@PathVariable String doctorId,
                                                 @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());
            String title = str(body.get("title"));
            if (title == null) {
                throw new RuntimeException("证书名称不能为空");
            }
            com.hospital.entity.DoctorCertificate certificate = new com.hospital.entity.DoctorCertificate();
            certificate.setCertificateId("CERT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            certificate.setDoctorId(doctorId);
            certificate.setTitle(title);
            certificate.setIssuer(str(body.get("issuer")));
            certificate.setIssuedDate(parseDate(body.get("issuedDate")));
            certificate.setImageUrl(str(body.get("imageUrl")));
            Integer sortOrder = body.get("sortOrder") == null ? null
                    : Integer.parseInt(String.valueOf(body.get("sortOrder")).trim());
            certificate.setSortOrder(sortOrder == null
                    ? certificateRepository.findByDoctorIdOrderBySortOrderAscCreateTimeAsc(doctorId).size() + 1
                    : sortOrder);
            certificate.setCreateTime(java.time.LocalDateTime.now());
            certificateRepository.save(certificate);
            result.put("success", true);
            result.put("data", certificateCard(certificate));
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 编辑证书。 */
    @PutMapping("/{doctorId}/certificates/{certificateId}")
    public Map<String, Object> updateCertificate(@PathVariable String doctorId,
                                                 @PathVariable String certificateId,
                                                 @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());
            com.hospital.entity.DoctorCertificate certificate = certificateRepository
                    .findByCertificateIdAndDoctorId(certificateId, doctorId)
                    .orElseThrow(() -> new RuntimeException("证书不存在"));
            if (body.containsKey("title")) {
                String title = str(body.get("title"));
                if (title == null) {
                    throw new RuntimeException("证书名称不能为空");
                }
                certificate.setTitle(title);
            }
            if (body.containsKey("issuer")) {
                certificate.setIssuer(str(body.get("issuer")));
            }
            if (body.containsKey("issuedDate")) {
                certificate.setIssuedDate(parseDate(body.get("issuedDate")));
            }
            if (body.containsKey("imageUrl")) {
                certificate.setImageUrl(str(body.get("imageUrl")));
            }
            if (body.containsKey("sortOrder") && body.get("sortOrder") != null) {
                certificate.setSortOrder(Integer.parseInt(String.valueOf(body.get("sortOrder")).trim()));
            }
            certificateRepository.save(certificate);
            result.put("success", true);
            result.put("data", certificateCard(certificate));
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 删除证书。 */
    @DeleteMapping("/{doctorId}/certificates/{certificateId}")
    public Map<String, Object> deleteCertificate(@PathVariable String doctorId,
                                                 @PathVariable String certificateId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());
            com.hospital.entity.DoctorCertificate certificate = certificateRepository
                    .findByCertificateIdAndDoctorId(certificateId, doctorId)
                    .orElseThrow(() -> new RuntimeException("证书不存在"));
            certificateRepository.delete(certificate);
            result.put("success", true);
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 证书排序：传入按展示顺序排列的 certificateId 数组。 */
    @PutMapping("/{doctorId}/certificates/sort")
    @SuppressWarnings("unchecked")
    public Map<String, Object> sortCertificates(@PathVariable String doctorId,
                                                @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            Doctor doctor = doctorService.findById(doctorId);
            authContext.requireHospitalScope(doctor.getHospitalId());
            Object rawIds = body.get("certificateIds");
            if (!(rawIds instanceof List)) {
                throw new RuntimeException("请提供 certificateIds 数组");
            }
            List<Object> ids = (List<Object>) rawIds;
            Map<String, com.hospital.entity.DoctorCertificate> existing = new java.util.HashMap<>();
            for (com.hospital.entity.DoctorCertificate certificate
                    : certificateRepository.findByDoctorIdOrderBySortOrderAscCreateTimeAsc(doctorId)) {
                existing.put(certificate.getCertificateId(), certificate);
            }
            int order = 1;
            for (Object idValue : ids) {
                String id = idValue == null ? null : String.valueOf(idValue);
                com.hospital.entity.DoctorCertificate certificate = existing.remove(id);
                if (certificate == null) {
                    throw new RuntimeException("证书不存在或不属于该医生：" + id);
                }
                certificate.setSortOrder(order++);
                certificateRepository.save(certificate);
            }
            result.put("success", true);
            result.put("data", certificateRepository.findByDoctorIdOrderBySortOrderAscCreateTimeAsc(doctorId)
                    .stream().map(this::certificateCard).collect(Collectors.toList()));
        } catch (com.hospital.exception.ForbiddenException e) {
            throw e;
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> certificateCard(com.hospital.entity.DoctorCertificate certificate) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("certificateId", certificate.getCertificateId());
        map.put("doctorId", certificate.getDoctorId());
        map.put("title", certificate.getTitle());
        map.put("issuer", certificate.getIssuer());
        map.put("issuedDate", certificate.getIssuedDate());
        map.put("imageUrl", certificate.getImageUrl());
        map.put("sortOrder", certificate.getSortOrder());
        map.put("createTime", certificate.getCreateTime());
        return map;
    }

    private java.time.LocalDate parseDate(Object value) {
        String text = str(value);
        return text == null ? null : java.time.LocalDate.parse(text);
    }

    private String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Doctor doctor) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (doctor.getPassword() == null || doctor.getPassword().isEmpty()) {
                throw new RuntimeException("请设置初始密码");
            }
            doctor.setHospitalId(authContext.resolveHospitalId(doctor.getHospitalId()));
            doctor.setMustChangePassword(0);
            Doctor created = doctorService.create(doctor);
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{doctorId}")
    public Map<String, Object> update(@PathVariable String doctorId, @RequestBody Doctor doctor) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor existing = doctorService.findById(doctorId);
            authContext.requireHospitalScope(existing.getHospitalId());
            Doctor updated = doctorService.update(doctorId, doctor);
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{doctorId}")
    public Map<String, Object> delete(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Doctor existing = doctorService.findById(doctorId);
            authContext.requireHospitalScope(existing.getHospitalId());
            doctorService.delete(doctorId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}