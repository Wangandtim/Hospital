package com.hospital.controller;

import com.hospital.entity.Doctor;
import com.hospital.entity.Schedule;
import com.hospital.security.AuthContext;
import com.hospital.service.DoctorService;
import com.hospital.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private com.hospital.config.ScheduleRules scheduleRules;

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    @Autowired
    private com.hospital.repository.DoctorRepository doctorRepository;

    /** 医生只能维护自己的排班；管理员按医院范围校验。 */
    private void requireDoctorScope(String doctorId) {
        if ("DOCTOR".equals(authContext.role())) {
            String ownerId = authContext.ownerId();
            if (ownerId == null || !ownerId.equals(doctorId)) {
                throw new RuntimeException("无权修改其他医生的排班");
            }
        }
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<?> getByHospital(
            @PathVariable String hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        authContext.requireHospitalScope(hospitalId);
        if (date == null) {
            // 不再返回全院全量排班：强制按日期查询，避免一次拉取上万条
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "缺少 date 参数：请使用 /api/schedules/hospital/{id}/date/{date} 查询某天排班");
        }
        return scheduleService.findByHospitalAndDateDetail(hospitalId, date);
    }

    /** 某医院近期有排班的日期（供日历标记使用，只返回日期，不返回排班明细）。 */
    @GetMapping("/hospital/{hospitalId}/dates")
    public Map<String, Object> getHospitalScheduleDates(
            @PathVariable String hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String, Object> result = new HashMap<>();
        try {
            authContext.requireHospitalScope(hospitalId);
            LocalDate start = from != null ? from : LocalDate.now();
            LocalDate end = to != null ? to : start.plusDays(30);
            result.put("success", true);
            result.put("data", scheduleService.findScheduleDates(hospitalId, start, end));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/hospital/{hospitalId}/date/{date}")
    public Map<String, Object> getSchedulesByHospitalAndDate(
            @PathVariable String hospitalId,
            @PathVariable String date) {
        Map<String, Object> result = new HashMap<>();
        try {
            authContext.requireHospitalScope(hospitalId);
            LocalDate workDate = LocalDate.parse(date);
            List<com.hospital.dto.ScheduleDetailDTO> schedules =
                    scheduleService.findByHospitalAndDateDetail(hospitalId, workDate);
            result.put("success", true);
            result.put("data", schedules);
            result.put("total", schedules.size());
            result.put("available", schedules.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAvailable()))
                    .count());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 单院单日排班分页：支持关键词/科室/职称筛选，可选按科室分组。 */
    @GetMapping("/hospital/{hospitalId}/date/{date}/page")
    public Map<String, Object> pageByHospitalAndDate(
            @PathVariable String hospitalId,
            @PathVariable String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "flat") String groupBy,
            @RequestParam(defaultValue = "0") int slotPage,
            @RequestParam(defaultValue = "20") int slotSize) {
        authContext.requireHospitalScope(hospitalId);
        LocalDate workDate = LocalDate.parse(date);
        List<com.hospital.dto.ScheduleDetailDTO> all = scheduleService.findByHospitalAndDateDetail(hospitalId, workDate);
        Map<String, Integer> fees = new HashMap<>();
        java.util.Set<String> doctorIds = all.stream()
                .map(com.hospital.dto.ScheduleDetailDTO::getDoctorId)
                .collect(java.util.stream.Collectors.toSet());
        for (Doctor doctor : doctorRepository.findAllById(doctorIds)) {
            fees.put(doctor.getDoctorId(), doctor.getConsultationFee());
        }

        List<com.hospital.dto.ScheduleDetailDTO> filtered = new java.util.ArrayList<>();
        for (com.hospital.dto.ScheduleDetailDTO dto : all) {
            // 只保留有号源时段；已满/停诊不进入列表
            if (!Boolean.TRUE.equals(dto.getAvailable())
                    || dto.getRemainingSlots() == null || dto.getRemainingSlots() <= 0) {
                continue;
            }
            if (deptId != null && !deptId.isEmpty() && !deptId.equals(dto.getDeptId())) {
                continue;
            }
            String p = catalogService.periodOf(dto.getTimeSlot());
            if (period != null && !period.isEmpty() && !period.equalsIgnoreCase(p)) {
                continue;
            }
            if (title != null && !title.trim().isEmpty()
                    && (dto.getDoctorTitle() == null || !dto.getDoctorTitle().contains(title.trim()))) {
                continue;
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                boolean match = (dto.getDoctorName() != null && dto.getDoctorName().contains(kw))
                        || (dto.getDeptName() != null && dto.getDeptName().contains(kw));
                if (!match) {
                    continue;
                }
            }
            filtered.add(dto);
        }

        int safePage = catalogService.page(page);
        int safeSize = catalogService.size(size);
        if ("dept".equalsIgnoreCase(groupBy)) {
            return groupSchedulesByDept(filtered, fees, safePage, safeSize, slotPage, slotSize);
        }

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (com.hospital.dto.ScheduleDetailDTO dto : filtered) {
            items.add(catalogService.scheduleCard(dto, fees.get(dto.getDoctorId())));
        }
        items.sort(java.util.Comparator
                .comparing((Map<String, Object> s) -> String.valueOf(s.get("timeSlot")))
                .thenComparing(s -> String.valueOf(s.get("doctorId"))));
        long total = items.size();
        return catalogService.envelope(catalogService.slice(items, safePage, safeSize), safePage, safeSize, total);
    }

    /** 按科室分组：外层 page/size 分页科室，slotPage/slotSize 分页组内时段。 */
    private Map<String, Object> groupSchedulesByDept(List<com.hospital.dto.ScheduleDetailDTO> filtered,
                                                     Map<String, Integer> fees, int page, int size,
                                                     int slotPage, int slotSize) {
        Map<String, List<com.hospital.dto.ScheduleDetailDTO>> byDept = new java.util.TreeMap<>();
        for (com.hospital.dto.ScheduleDetailDTO dto : filtered) {
            String key = dto.getDeptId() == null ? "" : dto.getDeptId();
            byDept.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(dto);
        }
        int safeSlotPage = Math.max(slotPage, 0);
        int safeSlotSize = Math.min(Math.max(slotSize, 1), 100);
        List<String> deptIds = new java.util.ArrayList<>(byDept.keySet());
        int from = Math.min(page * size, deptIds.size());
        int to = Math.min(from + size, deptIds.size());
        List<Map<String, Object>> groups = new java.util.ArrayList<>();
        for (String currentDeptId : deptIds.subList(from, to)) {
            List<com.hospital.dto.ScheduleDetailDTO> slots = byDept.get(currentDeptId);
            slots.sort(java.util.Comparator
                    .comparing(com.hospital.dto.ScheduleDetailDTO::getTimeSlot, java.util.Comparator.nullsLast(String::compareTo))
                    .thenComparing(com.hospital.dto.ScheduleDetailDTO::getDoctorId, java.util.Comparator.nullsLast(String::compareTo)));
            int total = slots.size();
            int slotFrom = Math.min(safeSlotPage * safeSlotSize, total);
            int slotTo = Math.min(slotFrom + safeSlotSize, total);
            List<Map<String, Object>> slotItems = new java.util.ArrayList<>();
            for (com.hospital.dto.ScheduleDetailDTO dto : slots.subList(slotFrom, slotTo)) {
                slotItems.add(catalogService.scheduleCard(dto, fees.get(dto.getDoctorId())));
            }
            Map<String, Object> group = new java.util.LinkedHashMap<>();
            group.put("deptId", currentDeptId.isEmpty() ? null : currentDeptId);
            group.put("deptName", currentDeptId.isEmpty() ? null : slots.get(0).getDeptName());
            group.put("scheduleCount", total);
            group.put("slotPage", safeSlotPage);
            group.put("slotSize", safeSlotSize);
            group.put("slotTotalPages", (int) Math.ceil((double) total / safeSlotSize));
            group.put("slotHasMore", (long) (safeSlotPage + 1) * safeSlotSize < total);
            group.put("slots", slotItems);
            groups.add(group);
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("groupBy", "dept");
        result.put("page", page);
        result.put("size", size);
        result.put("total", deptIds.size());
        result.put("totalPages", (int) Math.ceil((double) deptIds.size() / size));
        result.put("hasMore", (long) (page + 1) * size < deptIds.size());
        result.put("scheduleTotal", filtered.size());
        result.put("groups", groups);
        return result;
    }

    @GetMapping("/hospital/{hospitalId}/doctor/{doctorId}")
    public List<?> getByHospitalAndDoctor(
            @PathVariable String hospitalId,
            @PathVariable String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        authContext.requireHospitalScope(hospitalId);
        if (date == null) {
            // 不再返回该医生全部排班：强制按日期查询
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "缺少 date 参数：请使用 /api/schedules/hospital/{id}/doctor/{doctorId}?date=yyyy-MM-dd");
        }
        return scheduleService.findByHospitalAndDoctorAndDateDetail(hospitalId, doctorId, date);
    }

    @GetMapping("/available")
    public List<Schedule> getAvailable(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.findAvailableSchedules(doctorId, date);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Schedule> getByDoctorId(@PathVariable String doctorId) {
        Doctor doctor = doctorService.findById(doctorId);
        authContext.requireHospitalScope(doctor.getHospitalId());
        return scheduleService.findByDoctorId(doctorId);
    }

    @GetMapping("/doctor/{doctorId}/page")
    public Map<String, Object> getByDoctorIdWithPage(
            @PathVariable String doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return scheduleService.findByDoctorIdWithPage(doctorId, page, size);
    }

    @GetMapping("/doctor/{doctorId}/page/standard")
    public Map<String, Object> getByDoctorIdStandardWithPage(
            @PathVariable String doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return scheduleService.findByDoctorIdStandardWithPage(doctorId, page, size);
    }

    @GetMapping("/available/standard")
    public List<Schedule> getAvailableStandard(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.findAvailableSchedulesStandard(doctorId, date);
    }
    
    @GetMapping("/{scheduleId}")
    public Schedule getById(@PathVariable String scheduleId) {
        return scheduleService.findById(scheduleId);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Schedule schedule) {
        Map<String, Object> result = new HashMap<>();
        try {
            String hospitalId = authContext.resolveHospitalId(schedule.getHospitalId());
            requireDoctorScope(schedule.getDoctorId());
            Doctor doctor = doctorService.findById(schedule.getDoctorId());
            if (authContext.isAdmin()) {
                authContext.requireHospitalScope(doctor.getHospitalId());
            }
            Schedule created = scheduleService.createSchedule(
                    hospitalId,
                    schedule.getDoctorId(),
                    schedule.getWorkDate(),
                    schedule.getTimeSlot(),
                    schedule.getMaxPatients(),
                    schedule.getStatus()
            );
            result.put("success", true);
            result.put("data", created);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{scheduleId}")
    public Map<String, Object> update(
            @PathVariable String scheduleId,
            @RequestBody Schedule schedule) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule existing = scheduleService.findById(scheduleId);
            requireDoctorScope(existing.getDoctorId());
            authContext.requireHospitalScope(existing.getHospitalId());
            Schedule updated = scheduleService.updateSchedule(
                    scheduleId,
                    schedule.getWorkDate(),
                    schedule.getTimeSlot(),
                    schedule.getMaxPatients(),
                    schedule.getStatus()
            );
            result.put("success", true);
            result.put("data", updated);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{scheduleId}")
    public Map<String, Object> delete(@PathVariable String scheduleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule existing = scheduleService.findById(scheduleId);
            requireDoctorScope(existing.getDoctorId());
            authContext.requireHospitalScope(existing.getHospitalId());
            scheduleService.deleteSchedule(scheduleId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 回收站：已被软删除的排班（院管/超管）。 */
    @GetMapping("/deleted/{hospitalId}")
    public Map<String, Object> deletedList(@PathVariable String hospitalId) {
        Map<String, Object> result = new HashMap<>();
        try {
            authContext.requireHospitalScope(hospitalId);
            result.put("success", true);
            result.put("data", scheduleService.findDeletedByHospitalId(hospitalId));
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 从回收站恢复排班。 */
    @PostMapping("/{scheduleId}/restore")
    public Map<String, Object> restore(@PathVariable String scheduleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Schedule existing = scheduleService.findByIdIncludingDeleted(scheduleId);
            requireDoctorScope(existing.getDoctorId());
            authContext.requireHospitalScope(existing.getHospitalId());
            scheduleService.restoreSchedule(scheduleId);
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/generate/{doctorId}")
    public Map<String, Object> generateWeekly(
            @PathVariable String doctorId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            requireDoctorScope(doctorId);
            String hospitalId = (String) request.get("hospitalId");
            String startDateStr = (String) request.get("startDate");
            LocalDate startDate = LocalDate.parse(startDateStr);
            Integer maxPatients = request.get("maxPatients") != null ?
                    Integer.parseInt(request.get("maxPatients").toString()) : 20;

            List<String> timeSlots = scheduleRules.timeSlots();

            scheduleService.generateWeeklySchedule(hospitalId, doctorId, startDate, timeSlots, maxPatients);
            result.put("success", true);
            result.put("message", "排班生成成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/check-conflict")
    public Map<String, Object> checkConflict(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String timeSlot,
            @RequestParam(required = false) String excludeScheduleId) {
        Map<String, Object> result = new HashMap<>();
        boolean hasConflict = scheduleService.checkAppointmentConflict(doctorId, date, timeSlot, excludeScheduleId);
        result.put("success", true);
        result.put("hasConflict", hasConflict);
        return result;
    }

    @GetMapping("/doctor/{doctorId}/dates")
    public Map<String, Object> getScheduleDates(@PathVariable String doctorId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> dates = scheduleService.findScheduleDatesByDoctor(doctorId);
            result.put("success", true);
            result.put("data", dates);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/doctor/{doctorId}/date/{date}")
    public Map<String, Object> getSchedulesByDoctorAndDate(
            @PathVariable String doctorId,
            @PathVariable String date) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate workDate = LocalDate.parse(date);
            List<com.hospital.dto.ScheduleDetailDTO> schedules =
                    scheduleService.findByDoctorAndDateDetail(doctorId, workDate);
            result.put("success", true);
            result.put("data", schedules);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 手动触发复制今天排班到明天
     */
    @PostMapping("/copy-to-next-day")
    public Map<String, Object> copyToNextDay() {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = scheduleService.copyScheduleToNextDay();
            result.put("success", true);
            result.put("message", "排班复制成功");
            result.put("copiedCount", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 复制指定日期的排班到目标日期
     */
    @PostMapping("/copy")
    public Map<String, Object> copySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sourceDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = scheduleService.copyScheduleToDate(sourceDate, targetDate);
            result.put("success", true);
            result.put("message", "排班复制成功");
            result.put("copiedCount", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 为所有医生生成保底排班
     * 每个医生每天设置两个1小时的排班时段：上午9:00-10:00，下午14:00-15:00
     * 这些排班作为保底时间，后续可更换，但保底时间始终可预约
     */
    @PostMapping("/generate-base")
    public Map<String, Object> generateBaseSchedule(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = scheduleService.generateBaseScheduleForAllDoctors(days);
            result.put("success", true);
            result.put("message", "保底排班生成成功");
            result.put("generatedCount", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 为指定医生生成保底排班
     */
    @PostMapping("/generate-base/doctor/{doctorId}")
    public Map<String, Object> generateBaseScheduleForDoctor(
            @PathVariable String doctorId,
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = scheduleService.generateBaseScheduleForDoctor(doctorId, days);
            result.put("success", true);
            result.put("message", "保底排班生成成功");
            result.put("generatedCount", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}