package com.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.Department;
import com.hospital.entity.EmergencySnapshot;
import com.hospital.entity.Schedule;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.EmergencySnapshotRepository;
import com.hospital.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 应急写操作前记录科室/排班快照，管理员可一键回滚。 */
@Service
public class EmergencySnapshotService {

    private static final Pattern DEPT_PATTERN = Pattern.compile("^/api/departments/([^/]+)$");
    private static final Pattern SCHEDULE_PATTERN = Pattern.compile("^/api/schedules/([^/]+)$");

    @Autowired
    private EmergencySnapshotRepository snapshotRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** 仅对“修改已有记录”的 PUT 请求做快照；新增记录无需备份。 */
    public void capture(String token, String hospitalId, String method, String path) {
        if (!"PUT".equalsIgnoreCase(method) || hospitalId == null || path == null) {
            return;
        }

        try {
            String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

            Matcher deptMatcher = DEPT_PATTERN.matcher(cleanPath);
            if (deptMatcher.matches()) {
                String deptId = deptMatcher.group(1);
                Department dept = departmentRepository.findById(deptId).orElse(null);
                if (dept != null && hospitalId.equals(dept.getHospitalId())) {
                    saveSnapshot(token, hospitalId, "department", deptId, toDeptMap(dept));
                }
                return;
            }

            Matcher scheduleMatcher = SCHEDULE_PATTERN.matcher(cleanPath);
            if (scheduleMatcher.matches()) {
                String scheduleId = scheduleMatcher.group(1);
                Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
                if (schedule != null && hospitalId.equals(schedule.getHospitalId())) {
                    saveSnapshot(token, hospitalId, "schedule", scheduleId, toScheduleMap(schedule));
                }
            }
        } catch (Exception e) {
            // 快照失败不阻断业务，但记录日志便于排查
            System.out.println("应急快照失败: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> list(String hospitalId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmergencySnapshot snapshot : snapshotRepository.findByHospitalIdOrderByCreateTimeDesc(hospitalId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("snapshotId", snapshot.getId());
            item.put("tableName", snapshot.getTableName());
            item.put("recordId", snapshot.getRecordId());
            item.put("createTime", snapshot.getCreateTime());
            item.put("label", buildLabel(snapshot));
            result.add(item);
        }
        return result;
    }

    public String getHospitalId(String snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .map(EmergencySnapshot::getHospitalId)
                .orElse(null);
    }

    @Transactional
    public void restore(String snapshotId) throws Exception {
        EmergencySnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("快照不存在或已过期"));

        Map<?, ?> data = objectMapper.readValue(snapshot.getDataJson(), Map.class);

        if ("department".equals(snapshot.getTableName())) {
            Department dept = departmentRepository.findById(snapshot.getRecordId()).orElse(new Department());
            dept.setDeptId(snapshot.getRecordId());
            dept.setHospitalId((String) data.get("hospitalId"));
            dept.setName((String) data.get("name"));
            dept.setDescription((String) data.get("description"));
            dept.setDiseases((String) data.get("diseases"));
            dept.setServiceTime((String) data.get("serviceTime"));
            dept.setIsFlagship(toInt(data.get("isFlagship"), 0));
            dept.setFlagshipLevel(toInt(data.get("flagshipLevel"), 0));
            dept.setAwards((String) data.get("awards"));
            dept.setCases((String) data.get("cases"));
            if (dept.getCreateTime() == null) {
                dept.setCreateTime(LocalDateTime.now());
            }
            departmentRepository.save(dept);
            return;
        }

        if ("schedule".equals(snapshot.getTableName())) {
            Schedule schedule = scheduleRepository.findById(snapshot.getRecordId()).orElse(new Schedule());
            schedule.setScheduleId(snapshot.getRecordId());
            schedule.setHospitalId((String) data.get("hospitalId"));
            schedule.setDoctorId((String) data.get("doctorId"));
            Object workDate = data.get("workDate");
            if (workDate != null) {
                schedule.setWorkDate(java.time.LocalDate.parse(String.valueOf(workDate)));
            }
            schedule.setTimeSlot((String) data.get("timeSlot"));
            schedule.setMaxPatients(toInt(data.get("maxPatients"), 2));
            schedule.setBookedCount(toInt(data.get("bookedCount"), 0));
            schedule.setStatus(toInt(data.get("status"), 1));
            scheduleRepository.save(schedule);
            return;
        }

        throw new RuntimeException("未知的快照类型");
    }

    public void cleanup(int retainDays) {
        snapshotRepository.deleteByCreateTimeBefore(LocalDateTime.now().minusDays(retainDays));
    }

    private void saveSnapshot(String token, String hospitalId, String tableName, String recordId,
                              Map<String, Object> data) throws Exception {
        EmergencySnapshot snapshot = new EmergencySnapshot();
        snapshot.setId("SNP" + System.currentTimeMillis());
        snapshot.setHospitalId(hospitalId);
        snapshot.setTableName(tableName);
        snapshot.setRecordId(recordId);
        snapshot.setToken(token);
        snapshot.setCreateTime(LocalDateTime.now());
        snapshot.setDataJson(objectMapper.writeValueAsString(data));
        snapshotRepository.save(snapshot);
    }

    private Map<String, Object> toDeptMap(Department dept) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("deptId", dept.getDeptId());
        map.put("hospitalId", dept.getHospitalId());
        map.put("name", dept.getName());
        map.put("description", dept.getDescription());
        map.put("diseases", dept.getDiseases());
        map.put("serviceTime", dept.getServiceTime());
        map.put("isFlagship", dept.getIsFlagship());
        map.put("flagshipLevel", dept.getFlagshipLevel());
        map.put("awards", dept.getAwards());
        map.put("cases", dept.getCases());
        map.put("createTime", dept.getCreateTime());
        return map;
    }

    private Map<String, Object> toScheduleMap(Schedule schedule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scheduleId", schedule.getScheduleId());
        map.put("hospitalId", schedule.getHospitalId());
        map.put("doctorId", schedule.getDoctorId());
        map.put("workDate", schedule.getWorkDate());
        map.put("timeSlot", schedule.getTimeSlot());
        map.put("maxPatients", schedule.getMaxPatients());
        map.put("bookedCount", schedule.getBookedCount());
        map.put("status", schedule.getStatus());
        return map;
    }

    private String buildLabel(EmergencySnapshot snapshot) {
        try {
            Map<?, ?> data = objectMapper.readValue(snapshot.getDataJson(), Map.class);
            if ("department".equals(snapshot.getTableName())) {
                return "科室：" + data.get("name");
            }
            if ("schedule".equals(snapshot.getTableName())) {
                return "排班：" + data.get("workDate") + " " + data.get("timeSlot");
            }
        } catch (Exception ignored) {
        }
        return snapshot.getTableName() + " / " + snapshot.getRecordId();
    }

    private Integer toInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
