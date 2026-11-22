package com.hospital.service;

import com.hospital.dto.ScheduleDetailDTO;
import com.hospital.entity.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 目录类接口（医院/科室/医生/排班/首页/搜索）的展示字段组装。
 * 真实字段来自现有表；计算字段（余号、出诊数、距离、流程节点）在服务端算；
 * 缺失字段由 {@link DemoMockService} 补齐并打 _mock 标记。
 */
@Service
public class CatalogService {

    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private HospitalArticleRepository articleRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private DemoMockService mock;
    @Autowired
    private ScheduleService scheduleService;

    public static final int MAX_SIZE = 100;

    public int page(int page) {
        return Math.max(page, 0);
    }

    public int size(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_SIZE);
    }

    public Map<String, Object> envelope(List<?> items, int page, int size, long total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) total / size));
        result.put("hasMore", (long) (page + 1) * size < total);
        if (mock.isEnabled()) {
            result.put("meta", Map.of("mock", true, "mockEnabled", true));
        } else {
            result.put("meta", Map.of("mock", false, "mockEnabled", false));
        }
        return result;
    }

    public <T> List<T> slice(List<T> list, int page, int size) {
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    // ---------------- 医院 ----------------

    public Map<String, Object> hospitalCard(Hospital h, boolean todayAvailable, boolean tomorrowAvailable,
                                            Double userLat, Double userLng) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hospitalId", h.getHospitalId());
        map.put("name", h.getHospitalName());
        map.put("hospitalName", h.getHospitalName());
        map.put("shortName", shortName(h.getHospitalName()));
        map.put("level", h.getLevel());
        map.put("address", h.getAddress());
        map.put("district", h.getDistrict());
        map.put("city", h.getCity());
        map.put("province", h.getProvince());
        map.put("phone", h.getPhone());
        map.put("latitude", h.getLatitude());
        map.put("longitude", h.getLongitude());
        map.put("description", h.getDescription());
        map.put("flagshipDept", h.getFlagshipDept());
        map.put("websiteUrl", h.getWebsiteUrl());
        map.put("status", h.getStatus());
        map.put("distanceKm", distanceKm(userLat, userLng, h.getLatitude(), h.getLongitude()));
        map.put("todayAvailable", todayAvailable);
        map.put("tomorrowAvailable", tomorrowAvailable);
        map.put("notice", Boolean.TRUE.equals(h.getNoticeEnabled()) ? latestNotice(h.getHospitalId()) : null);
        List<String> mockFields = new ArrayList<>();
        map.put("_mock", mockFields);
        mock.putHospitalExtras(map, h.getHospitalId());
        if (mock.isEnabled()) {
            mockFields.addAll(Arrays.asList("nature", "campus", "logoUrl", "outpatientHours",
                    "guideDeskPhone", "emergencyPhone", "insuranceSupported", "rating"));
        }
        return map;
    }

    private String shortName(String name) {
        if (name == null) {
            return null;
        }
        return name.replace("医院", "").replace("人民医院", "人医");
    }

    public Map<String, Object> hospitalDetail(Hospital h) {
        Map<String, Object> map = hospitalCard(h, hasAvailable(h.getHospitalId(), LocalDate.now()),
                hasAvailable(h.getHospitalId(), LocalDate.now().plusDays(1)), null, null);
        map.put("departmentsSummary", departmentsSummary(h.getHospitalId()));
        List<Map<String, Object>> announcements = new ArrayList<>();
        if (Boolean.TRUE.equals(h.getNoticeEnabled())) {
            for (HospitalArticle article : articleRepository.findByHospitalIdAndStatusOrderByCreateTimeDesc(h.getHospitalId(), 1)) {
                announcements.add(articleCard(article));
                if (announcements.size() >= 3) {
                    break;
                }
            }
        }
        map.put("announcements", announcements);
        List<String> mockFields = (List<String>) map.get("_mock");
        if (mock.isEnabled()) {
            map.put("guideInfo", Map.of(
                    "location", "门诊 1 层大厅",
                    "phone", h.getPhone(),
                    "services", List.of("盖章", "病历复印", "轮椅借用")));
            mockFields.addAll(Arrays.asList("guideInfo"));
        }
        return map;
    }

    public Map<String, Object> latestNotice(String hospitalId) {
        List<HospitalArticle> list = articleRepository.findByHospitalIdAndStatusOrderByCreateTimeDesc(hospitalId, 1);
        if (list.isEmpty()) {
            return null;
        }
        HospitalArticle a = list.get(0);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("articleId", a.getArticleId());
        map.put("title", a.getTitle());
        map.put("summary", a.getSummary());
        map.put("publishedAt", a.getCreateTime());
        return map;
    }

    public boolean mockEnabled() {
        return mock.isEnabled();
    }

    public Map<String, Object> articleCard(HospitalArticle a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("articleId", a.getArticleId());
        map.put("hospitalId", a.getHospitalId());
        map.put("title", a.getTitle());
        map.put("summary", a.getSummary());
        map.put("content", a.getContent());
        map.put("coverUrl", a.getCoverUrl());
        map.put("category", a.getCategory());
        map.put("publishedAt", a.getCreateTime());
        map.put("_mock", Collections.emptyList());
        return map;
    }

    private Map<String, Object> departmentsSummary(String hospitalId) {
        List<Department> depts = departmentRepository.findByHospitalId(hospitalId);
        int todayOpen = 0;
        for (Department dept : depts) {
            if (dept.getServiceTime() != null || hasDeptSchedule(hospitalId, dept.getDeptId())) {
                todayOpen++;
            }
        }
        return Map.of("total", depts.size(), "todayOpen", todayOpen);
    }

    private boolean hasAvailable(String hospitalId, LocalDate date) {
        return !scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, date).isEmpty();
    }

    private boolean hasDeptSchedule(String hospitalId, String deptId) {
        Map<String, String> doctorDept = doctorDeptMap(hospitalId);
        for (Schedule s : scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, LocalDate.now())) {
            if (deptId.equals(doctorDept.get(s.getDoctorId()))) {
                return true;
            }
        }
        return false;
    }

    /** 医院当天各科室统计：出诊医生数、剩余号、停诊数。 */
    @Autowired
    private DoctorDetailService doctorDetailService;

    @Autowired
    private DoctorCertificateRepository certificateRepository;

    public Map<String, int[]> deptStats(String hospitalId, LocalDate date) {
        Map<String, String> doctorDept = doctorDeptMap(hospitalId);
        Map<String, int[]> stats = new HashMap<>();
        Set<String> doctors = new HashSet<>();
        for (Schedule s : scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, date)) {
            String deptId = doctorDept.get(s.getDoctorId());
            if (deptId == null) {
                continue;
            }
            int[] row = stats.computeIfAbsent(deptId, k -> new int[3]);
            row[0] = row[0] + 1;
            row[1] = row[1] + Math.max(0, (s.getMaxPatients() == null ? 0 : s.getMaxPatients())
                    - (s.getBookedCount() == null ? 0 : s.getBookedCount()));
            if (doctors.add(s.getDoctorId())) {
                row[2] = row[2] + 1;
            }
        }
        return stats;
    }

    public Map<String, String> doctorDeptMap(String hospitalId) {
        Map<String, String> map = new HashMap<>();
        for (Doctor d : doctorRepository.findByHospitalId(hospitalId)) {
            map.put(d.getDoctorId(), d.getDeptId());
        }
        return map;
    }

    // ---------------- 科室 ----------------

    public Map<String, Object> deptCard(Department d, int[] stats) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("deptId", d.getDeptId());
        map.put("hospitalId", d.getHospitalId());
        map.put("name", d.getName());
        map.put("deptName", d.getName());
        map.put("description", d.getDescription());
        map.put("diseases", d.getDiseases());
        map.put("serviceTime", d.getServiceTime());
        map.put("group", groupOf(d.getName()));
        map.put("isFlagship", d.getIsFlagship());
        map.put("flagshipLevel", d.getFlagshipLevel());
        int doctors = stats == null ? 0 : stats[2];
        int remaining = stats == null ? 0 : stats[1];
        map.put("todayDoctorCount", doctors);
        map.put("todayRemaining", remaining);
        map.put("status", doctors > 0 || remaining > 0 ? "OPEN" : "CLOSED");
        map.put("todayAvailable", remaining > 0);
        List<String> mockFields = new ArrayList<>();
        map.put("_mock", mockFields);
        mock.putDeptExtras(map, d.getDeptId());
        if (mock.isEnabled()) {
            mockFields.addAll(Arrays.asList("location", "guideInfo"));
        }
        return map;
    }

    public Map<String, Object> deptDetail(Department d, Map<String, int[]> stats, Map<String, Integer> doctorRemain,
                                          List<Doctor> doctors, Map<String, String> deptNames) {
        Map<String, Object> map = deptCard(d, stats == null ? null : stats.get(d.getDeptId()));
        Integer fee = feeOf(doctors);
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("type", "普通号");
        general.put("fee", fee);
        Map<String, Object> expert = new LinkedHashMap<>();
        expert.put("type", "专家号");
        expert.put("fee", fee);
        map.put("groups", List.of(general, expert));
        List<Map<String, Object>> doctorItems = new ArrayList<>();
        for (Doctor doc : doctors) {
            doctorItems.add(doctorCard(doc, d.getName(), doctorRemain.getOrDefault(doc.getDoctorId(), 0), null));
            if (doctorItems.size() >= 10) {
                break;
            }
        }
        map.put("doctors", doctorItems);
        Map<String, String> photos = doctorDetailService.photoUrls(
                doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList()));
        for (Map<String, Object> item : doctorItems) {
            item.put("photoUrl", photos.get(String.valueOf(item.get("doctorId"))));
        }
        List<String> mockFields = (List<String>) map.get("_mock");
        if (mock.isEnabled()) {
            map.put("floorMap", Map.of("report", "3F 报到", "wait", "A 区候诊", "pharmacy", "1F 取药"));
            mockFields.addAll(Arrays.asList("floorMap"));
        }
        return map;
    }

    /** 无真实报价返回 null，不伪造价格。 */
    private Integer feeOf(List<Doctor> doctors) {
        for (Doctor doctor : doctors) {
            if (doctor.getConsultationFee() != null) {
                return doctor.getConsultationFee();
            }
        }
        return null;
    }

    /** 医生证书：按 sort_order 升序，无证书返回空数组。 */
    private List<Map<String, Object>> certificateCards(String doctorId) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (com.hospital.entity.DoctorCertificate certificate
                : certificateRepository.findByDoctorIdOrderBySortOrderAscCreateTimeAsc(doctorId)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("certificateId", certificate.getCertificateId());
            map.put("title", certificate.getTitle());
            map.put("issuer", certificate.getIssuer());
            map.put("issuedDate", certificate.getIssuedDate());
            map.put("imageUrl", certificate.getImageUrl());
            map.put("sortOrder", certificate.getSortOrder());
            items.add(map);
        }
        return items;
    }

    public String groupOf(String deptName) {
        if (deptName == null) {
            return "专科";
        }
        String n = deptName;
        if (n.contains("检验") || n.contains("放射") || n.contains("超声") || n.contains("病理")
                || n.contains("影像") || n.contains("药房") || n.contains("核医学")) {
            return "医技";
        }
        if (n.contains("妇") || n.contains("产") || n.contains("儿") || n.contains("眼")
                || n.contains("耳鼻") || n.contains("口腔") || n.contains("皮肤") || n.contains("中医")
                || n.contains("精神") || n.contains("康复") || n.contains("肿瘤") || n.contains("疼痛")) {
            return "专科";
        }
        if (n.contains("外科") || n.contains("骨科") || n.contains("泌尿") || n.contains("整形")
                || n.contains("肛肠") || n.contains("麻醉")) {
            return "外科";
        }
        return "内科";
    }

    // ---------------- 医生 ----------------

    public Map<String, Object> doctorCard(Doctor d, String deptName, int todayRemaining, String scheduleSummary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getDoctorId());
        map.put("doctorId", d.getDoctorId());
        map.put("name", d.getName());
        map.put("doctorName", d.getName());
        map.put("title", d.getTitle());
        map.put("hospitalId", d.getHospitalId());
        map.put("deptId", d.getDeptId());
        map.put("deptName", deptName);
        map.put("department", deptName);
        map.put("expertise", d.getExpertise());
        map.put("introduction", d.getExperience());
        map.put("education", d.getEducation());
        map.put("gender", d.getGender());
        map.put("phone", d.getPhone());
        map.put("consultationFee", d.getConsultationFee());
        map.put("photoUrl", null);
        map.put("status", d.getStatus());
        map.put("todayRemaining", todayRemaining);
        map.put("todayScheduleSummary", scheduleSummary);
        List<String> mockFields = new ArrayList<>();
        map.put("_mock", mockFields);
        map.put("years", parseYears(d.getExperience()));
        return map;
    }

    public Map<String, Object> doctorDetail(Doctor d, String deptName, int todayRemaining,
                                            List<ScheduleDetailDTO> schedules) {
        Map<String, Object> map = doctorCard(d, deptName, todayRemaining, null);
        List<String> mockFields = (List<String>) map.get("_mock");
        // 介绍内容只读 doctor_detail，未填写即 null，不生成模板文案
        com.hospital.entity.DoctorDetail detail = doctorDetailService.findByDoctorId(d.getDoctorId());
        map.put("introduction", detail == null ? null : detail.getIntroduction());
        map.put("expertiseDesc", detail == null ? null : detail.getExpertiseDesc());
        map.put("workExperience", detail == null ? null : detail.getWorkExperience());
        map.put("education", detail == null ? null : detail.getEducation());
        map.put("awards", detail == null ? null : detail.getAwards());
        map.put("qualification", detail == null ? null : detail.getQualification());
        map.put("academicAppointments", detail == null ? null : detail.getAcademicAppointments());
        map.put("photoUrl", detail == null ? null : detail.getPhotoUrl());
        map.put("certificates", certificateCards(d.getDoctorId()));
        // 详情页挂号费以 doctor_detail 维护的值为准，未维护时沿用医生档案
        if (detail != null && detail.getConsultationFee() != null) {
            map.put("consultationFee", detail.getConsultationFee());
        }
        map.put("career", detail == null ? null : detail.getWorkExperience());
        map.put("moral", d.getMoral());
        List<Map<String, Object>> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            List<ScheduleDetailDTO> daySlots = schedules.stream()
                    .filter(s -> date.equals(s.getWorkDate()))
                    .collect(Collectors.toList());
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date);
            day.put("week", dayOfWeek(date));
            day.put("slots", daySlots.size());
            day.put("remaining", daySlots.stream().mapToInt(s -> s.getRemainingSlots() == null ? 0 : s.getRemainingSlots()).sum());
            days.add(day);
        }
        map.put("scheduleDays", days);
        map.put("reviews", new ArrayList<>());
        Map<String, Object> feeDetail = new LinkedHashMap<>();
        feeDetail.put("expertFee", d.getConsultationFee());
        feeDetail.put("generalFee", null);
        map.put("feeDetail", feeDetail);
        return map;
    }

    private String dayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            default -> "周日";
        };
    }

    public Integer parseYears(String experience) {
        if (experience == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})\\s*年").matcher(experience);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** 医生当天剩余号（按医院批量算一次，避免逐个医生查库）。 */
    public Map<String, Integer> doctorRemaining(String hospitalId, LocalDate date) {
        Map<String, Integer> map = new HashMap<>();
        for (Schedule s : scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, date)) {
            int remain = Math.max(0, (s.getMaxPatients() == null ? 0 : s.getMaxPatients())
                    - (s.getBookedCount() == null ? 0 : s.getBookedCount()));
            map.merge(s.getDoctorId(), remain, Integer::sum);
        }
        return map;
    }

    // ---------------- 排班 ----------------

    public Map<String, Object> scheduleCard(ScheduleDetailDTO dto, Integer fee) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scheduleId", dto.getScheduleId());
        map.put("hospitalId", dto.getHospitalId());
        map.put("hospitalName", dto.getHospitalName());
        map.put("doctorId", dto.getDoctorId());
        map.put("doctorName", dto.getDoctorName());
        map.put("doctorTitle", dto.getDoctorTitle());
        map.put("deptId", dto.getDeptId());
        map.put("deptName", dto.getDeptName());
        map.put("workDate", dto.getWorkDate());
        map.put("timeSlot", dto.getTimeSlot());
        map.put("period", periodOf(dto.getTimeSlot()));
        map.put("maxPatients", dto.getMaxPatients());
        map.put("bookedCount", dto.getBookedCount());
        map.put("remainingSlots", dto.getRemainingSlots());
        map.put("status", statusOf(dto));
        map.put("statusText", statusText(dto));
        map.put("fee", fee);
        List<String> mockFields = new ArrayList<>();
        map.put("_mock", mockFields);
        mock.putSlotExtras(map, dto.getScheduleId());
        if (mock.isEnabled()) {
            mockFields.addAll(Arrays.asList("insuranceNote", "location"));
        }
        return map;
    }

    public String periodOf(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains(":")) {
            return null;
        }
        int hour = Integer.parseInt(timeSlot.substring(0, 2));
        return hour < 12 ? "am" : "pm";
    }

    private String statusOf(ScheduleDetailDTO dto) {
        if (dto.getStatus() != null && dto.getStatus() == 2) {
            return "FULL";
        }
        if (dto.getRemainingSlots() != null && dto.getRemainingSlots() <= 0) {
            return "FULL";
        }
        if (dto.getStatus() != null && dto.getStatus() != 1) {
            return "SUSPENDED";
        }
        LocalDate date = dto.getWorkDate();
        if (date != null && date.isBefore(LocalDate.now())) {
            return "EXPIRED";
        }
        return "AVAILABLE";
    }

    private String statusText(ScheduleDetailDTO dto) {
        return switch (statusOf(dto)) {
            case "FULL" -> "约满";
            case "SUSPENDED" -> "停诊";
            case "EXPIRED" -> "已过号";
            default -> "余 " + (dto.getRemainingSlots() == null ? 0 : dto.getRemainingSlots());
        };
    }

    // ---------------- 首页 ----------------

    public Map<String, Object> home(String hospitalId, Double lat, Double lng, String patientId) {
        Hospital hospital = hospitalId == null ? firstHospital() : hospitalRepository.findById(hospitalId).orElse(firstHospital());
        if (hospital == null) {
            return envelope(Collections.emptyList(), 0, 20, 0);
        }
        LocalDate today = LocalDate.now();
        Map<String, int[]> stats = deptStats(hospital.getHospitalId(), today);
        List<Department> depts = departmentRepository.findByHospitalId(hospital.getHospitalId());

        List<Map<String, Object>> recommended = depts.stream()
                .map(d -> deptCard(d, stats.get(d.getDeptId())))
                .filter(d -> ((Integer) d.get("todayRemaining")) > 0)
                .sorted(Comparator.comparingInt((Map<String, Object> d) -> (Integer) d.get("todayRemaining")).reversed())
                .limit(6)
                .collect(Collectors.toList());

        List<ScheduleDetailDTO> slotDtos = scheduleService.findByHospitalAndDateDetail(hospital.getHospitalId(), today);
        Map<String, Integer> fees = doctorFeeMap(slotDtos);
        List<Map<String, Object>> todaySlots = slotDtos.stream()
                .filter(s -> s.getRemainingSlots() != null && s.getRemainingSlots() > 0)
                .limit(3)
                .map(s -> scheduleCard(s, fees.get(s.getDoctorId())))
                .collect(Collectors.toList());

        Map<String, Integer> remain = doctorRemaining(hospital.getHospitalId(), today);
        Map<String, String> deptNames = deptNameMap(hospital.getHospitalId());
        List<Doctor> doctors = doctorRepository.findByHospitalId(hospital.getHospitalId());
        List<Map<String, Object>> recommendedDoctors = doctors.stream()
                .filter(d -> remain.getOrDefault(d.getDoctorId(), 0) > 0)
                .map(d -> doctorCard(d, deptNames.get(d.getDeptId()), remain.getOrDefault(d.getDoctorId(), 0), null))
                .limit(2)
                .collect(Collectors.toList());

        List<Map<String, Object>> pending = new ArrayList<>();
        if (patientId != null && !patientId.isEmpty()) {
            for (Appointment a : appointmentRepository.findByPatientIdOrderByCreateTimeDesc(patientId)) {
                if (a.getStatus() != null && (a.getStatus() == 1 || a.getStatus() == 3)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("appointmentId", a.getAppointmentId());
                    item.put("doctorId", a.getDoctorId());
                    item.put("appointmentDate", a.getAppointmentDate());
                    item.put("timeSlot", a.getTimeSlot());
                    item.put("status", a.getStatus());
                    item.put("statusText", a.getStatus() == 3 ? "进行中" : "待就诊");
                    pending.add(item);
                    if (pending.size() >= 2) {
                        break;
                    }
                }
            }
        }

        List<Map<String, Object>> nearby = hospitalRepository.findByCity(hospital.getCity() == null ? "" : hospital.getCity())
                .stream()
                .filter(h -> !h.getHospitalId().equals(hospital.getHospitalId()))
                .map(h -> hospitalCard(h, hasAvailable(h.getHospitalId(), today),
                        hasAvailable(h.getHospitalId(), today.plusDays(1)), lat, lng))
                .limit(3)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hospital", hospitalCard(hospital, hasAvailable(hospital.getHospitalId(), today),
                hasAvailable(hospital.getHospitalId(), today.plusDays(1)), lat, lng));
        data.put("notice", Boolean.TRUE.equals(hospital.getNoticeEnabled()) ? latestNotice(hospital.getHospitalId()) : null);
        data.put("recommendedDepartments", recommended);
        data.put("commonSymptoms", mock.commonSymptoms());
        data.put("todaySlots", todaySlots);
        data.put("recommendedDoctors", recommendedDoctors);
        data.put("nearbyHospitals", nearby);
        data.put("pendingAppointments", pending);
        data.put("quickStats", Map.of(
                "todayDeptCount", recommended.size(),
                "todaySlotCount", slotDtos.size(),
                "familyCount", 0));

        Map<String, Object> result = envelope(Collections.singletonList(data), 0, 20, 1);
        result.put("data", data);
        List<String> mockFields = new ArrayList<>();
        if (mock.isEnabled()) {
            mockFields.addAll(Arrays.asList("hospital.nature", "hospital.campus", "commonSymptoms",
                    "todaySlots.insuranceNote", "todaySlots.location", "recommendedDoctors.tags",
                    "quickStats.familyCount"));
        }
        result.put("mockFields", mockFields);
        result.put("meta", Map.of("mock", mock.isEnabled(), "mockFields", mockFields));
        return result;
    }

    private Hospital firstHospital() {
        List<Hospital> list = hospitalRepository.findByStatus(1);
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Integer> doctorFeeMap(List<ScheduleDetailDTO> slots) {
        Set<String> ids = slots.stream().map(ScheduleDetailDTO::getDoctorId).collect(Collectors.toSet());
        Map<String, Integer> fees = new HashMap<>();
        for (Doctor d : doctorRepository.findAllById(ids)) {
            fees.put(d.getDoctorId(), d.getConsultationFee());
        }
        return fees;
    }

    private Map<String, String> deptNameMap(String hospitalId) {
        Map<String, String> map = new HashMap<>();
        for (Department d : departmentRepository.findByHospitalId(hospitalId)) {
            map.put(d.getDeptId(), d.getName());
        }
        return map;
    }

    // ---------------- 搜索 ----------------

    public Map<String, Object> search(String keyword, String type, String hospitalId, int page, int size) {
        page = page(page);
        size = size(size);
        String kw = keyword == null ? "" : keyword.trim();
        List<Map<String, Object>> items = new ArrayList<>();
        List<Department> deptLegacy = new ArrayList<>();
        List<Doctor> doctorLegacy = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("hospital", 0);
        counts.put("department", 0);
        counts.put("doctor", 0);
        counts.put("article", 0);

        boolean all = type == null || type.isEmpty() || "all".equals(type);

        if (!kw.isEmpty() && (all || "hospital".equals(type))) {
            List<Hospital> hospitals = hospitalRepository
                    .searchPage("%" + kw + "%", null, null, null,
                            org.springframework.data.domain.PageRequest.of(0, 200)).getContent();
            counts.put("hospital", hospitals.size());
            for (Hospital h : hospitals) {
                items.add(searchItem("hospital", h.getHospitalId(), h.getHospitalName(),
                        (h.getLevel() == null ? "" : h.getLevel()) + " · " + (h.getDistrict() == null ? "" : h.getDistrict()),
                        mock.isEnabled() ? Arrays.asList("公立综合", "支持医保") : Collections.emptyList(),
                        Map.of("hospitalId", h.getHospitalId())));
            }
        }

        if (!kw.isEmpty() && (all || "department".equals(type))) {
            List<Department> depts = departmentRepository.searchByKeyword(kw);
            if (hospitalId != null && !hospitalId.isEmpty()) {
                depts = depts.stream().filter(d -> hospitalId.equals(d.getHospitalId())).collect(Collectors.toList());
            }
            counts.put("department", depts.size());
            deptLegacy.addAll(depts);
            for (Department d : depts) {
                items.add(searchItem("department", d.getDeptId(), d.getName(),
                        d.getDiseases(), Collections.emptyList(),
                        Map.of("hospitalId", d.getHospitalId(), "deptId", d.getDeptId())));
            }
        }

        if (!kw.isEmpty() && (all || "doctor".equals(type) || "expertise".equals(type))) {
            Set<String> seen = new LinkedHashSet<>();
            List<Doctor> doctors = new ArrayList<>();
            if (type == null || all || "doctor".equals(type)) {
                doctorRepository.findByNameContaining(kw).forEach(d -> {
                    if (seen.add(d.getDoctorId())) doctors.add(d);
                });
            }
            doctorRepository.findByExpertiseContaining(kw).forEach(d -> {
                if (seen.add(d.getDoctorId())) doctors.add(d);
            });
            if (hospitalId != null && !hospitalId.isEmpty()) {
                doctors.removeIf(d -> !hospitalId.equals(d.getHospitalId()));
            }
            counts.put("doctor", doctors.size());
            doctorLegacy.addAll(doctors);
            Map<String, String> deptNames = new HashMap<>();
            for (Department d : departmentRepository.findAll()) {
                deptNames.put(d.getDeptId(), d.getName());
            }
            for (Doctor d : doctors) {
                String deptName = deptNames.get(d.getDeptId());
                items.add(searchItem("doctor", d.getDoctorId(), d.getName(),
                        (d.getTitle() == null ? "" : d.getTitle()) + " · " + (deptName == null ? "" : deptName),
                        mock.isEnabled() ? Arrays.asList("今日余号", "擅长：" + safe(d.getExpertise())) : Collections.emptyList(),
                        Map.of("hospitalId", d.getHospitalId(), "deptId", d.getDeptId() == null ? "" : d.getDeptId(),
                                "consultationFee", d.getConsultationFee() == null ? 0 : d.getConsultationFee())));
            }
        }

        if (!kw.isEmpty() && (all || "article".equals(type))) {
            List<HospitalArticle> articles = articleRepository.findByTitleContainingOrderByCreateTimeDesc(kw);
            if (hospitalId != null && !hospitalId.isEmpty()) {
                articles = articles.stream().filter(a -> hospitalId.equals(a.getHospitalId())).collect(Collectors.toList());
            }
            counts.put("article", articles.size());
            for (HospitalArticle a : articles) {
                items.add(searchItem("article", a.getArticleId(), a.getTitle(),
                        a.getSummary(), Collections.emptyList(),
                        Map.of("hospitalId", a.getHospitalId())));
            }
        }

        Map<String, Integer> typeOrder = Map.of("hospital", 0, "department", 1, "doctor", 2, "article", 3);
        items.sort(Comparator.<Map<String, Object>>comparingInt(i -> typeOrder.getOrDefault((String) i.get("type"), 9))
                .thenComparing(i -> String.valueOf(i.get("title")))
                .thenComparing(i -> String.valueOf(i.get("id"))));
        long total = items.size();
        List<Map<String, Object>> pageItems = slice(items, page, size);

        Map<String, Object> result = envelope(pageItems, page, size, total);
        result.put("keyword", keyword);
        result.put("counts", counts);
        result.put("departments", deptLegacy);
        result.put("doctors", doctorLegacy);
        result.put("totalCount", total);
        return result;
    }

    private Map<String, Object> searchItem(String type, String id, String title, String subtitle,
                                           List<String> tags, Map<String, Object> extra) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("id", id);
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("tags", tags);
        map.put("extra", extra);
        map.put("_mock", tags.isEmpty() ? Collections.emptyList() : Collections.singletonList("tags"));
        return map;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    // ---------------- 院内导航 ----------------

    /** 院内导航：楼层/诊区/路线/导医台，数据整体为 demo mock（_mock 标记）。 */
    public Map<String, Object> navigation(String hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        if (hospital == null) {
            result.put("success", false);
            result.put("message", "医院不存在");
            return result;
        }
        List<Map<String, Object>> floors = new ArrayList<>();
        floors.add(floor("1F", "门诊大厅", List.of(
                zone("挂号收费", "1 层 A 区", "1-6 号窗口"),
                zone("门诊药房", "1 层 B 区", "取药窗口 1-8"),
                zone("导医台", "1 层 大厅中央", "咨询/轮椅/盖章"))));
        floors.add(floor("2F", "内科诊区", deptZones(hospitalId, "内科", 2)));
        floors.add(floor("3F", "外科诊区", deptZones(hospitalId, "外科", 3)));
        floors.add(floor("4F", "专科与医技", concatZones(
                deptZones(hospitalId, "专科", 4), deptZones(hospitalId, "医技", 4))));

        String guidePhone = "029-8763 " + (1000 + Math.abs(hospitalId.hashCode() % 800) + 100);
        List<String> steps = mock.isEnabled() ? List.of(
                mock.text("进入门诊大厅，先在 1 层 A 区自助机取号/报到"),
                mock.text("看指示牌乘电梯至目标楼层"),
                mock.text("到对应诊区护士站扫码签到，等候叫号"),
                mock.text("就诊结束后按处方到 1 层 B 区药房取药")) : Collections.emptyList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hospitalId", hospital.getHospitalId());
        data.put("hospitalName", hospital.getHospitalName());
        data.put("address", hospital.getAddress());
        data.put("guideDeskPhone", guidePhone);
        data.put("floors", floors);
        data.put("route", Map.of(
                "from", "门诊 1 层大厅",
                "to", "对应诊区",
                "minutes", 6,
                "steps", steps));
        data.put("parking", mock.isEnabled() ? Map.of(
                "entrance", mock.text("地下停车场入口：医院东门"),
                "fee", "首 30 分钟免费，之后 5 元/小时") : Collections.emptyMap());
        List<String> mockFields = new ArrayList<>();
        if (mock.isEnabled()) {
            mockFields.addAll(Arrays.asList("guideDeskPhone", "floors", "route", "parking"));
        }
        data.put("_mock", mockFields);
        result.put("success", true);
        result.put("data", data);
        result.put("meta", Map.of("mock", mock.isEnabled(), "mockFields", mockFields));
        return result;
    }

    private Map<String, Object> floor(String floor, String name, List<Map<String, Object>> zones) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("floor", floor);
        map.put("name", name);
        map.put("zones", zones);
        return map;
    }

    private Map<String, Object> zone(String deptName, String area, String room) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("deptName", deptName);
        map.put("area", area);
        map.put("room", room);
        map.put("waitArea", area + " 候诊区");
        return map;
    }

    private List<Map<String, Object>> deptZones(String hospitalId, String group, int floor) {
        List<Map<String, Object>> zones = new ArrayList<>();
        for (Department dept : departmentRepository.findByHospitalId(hospitalId)) {
            if (!group.equals(groupOf(dept.getName()))) {
                continue;
            }
            zones.add(zone(dept.getName(), floor + " 层 A 区", floor + " 层 " + (zones.size() + 1) + " 诊室"));
        }
        return zones;
    }

    private List<Map<String, Object>> concatZones(List<Map<String, Object>> left, List<Map<String, Object>> right) {
        List<Map<String, Object>> all = new ArrayList<>(left);
        all.addAll(right);
        return all;
    }

    // ---------------- 距离 ----------------

    public Double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double earth = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double km = earth * c;
        return Math.round(km * 10) / 10.0;
    }
}
