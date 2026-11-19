package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 家庭成员本地持久化（内存版）。
 * 暂无 family_member 表，编辑结果保存在进程内 Map，重启后回到演示种子数据。
 * 种子成员带 _mock 标记；用户新增/编辑的成员 _mock 为空。
 */
@Service
public class FamilyMemberService {

    @Autowired
    private DemoMockService mock;

    private final Map<String, List<Map<String, Object>>> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public List<Map<String, Object>> members(String ownerPatientId) {
        if (ownerPatientId == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = store.computeIfAbsent(ownerPatientId, this::seed);
        return list;
    }

    private List<Map<String, Object>> seed(String ownerPatientId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!mock.isEnabled()) {
            return list;
        }
        list.add(seedMember(ownerPatientId, "MOCK-FAM-0001", "李秀兰", "女", 32, "配偶"));
        list.add(seedMember(ownerPatientId, "MOCK-FAM-0002", "王小雨", "女", 6, "子女"));
        return list;
    }

    private Map<String, Object> seedMember(String owner, String id, String name, String gender,
                                           int age, String relation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patientId", id);
        map.put("ownerPatientId", owner);
        map.put("name", name);
        map.put("gender", gender);
        map.put("age", age);
        map.put("phone", "139****6621");
        map.put("idCard", "6101**********1234");
        map.put("relation", relation);
        map.put("isDefault", false);
        map.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        map.put("_mock", List.of("patientId", "name", "gender", "age", "phone", "idCard"));
        return map;
    }

    public Map<String, Object> add(String ownerPatientId, Map<String, Object> body) {
        Map<String, Object> member = new LinkedHashMap<>();
        String id = "FAM-" + String.format("%06d", sequence.getAndIncrement());
        member.put("patientId", id);
        member.put("ownerPatientId", ownerPatientId);
        member.put("name", requireName(body));
        member.put("gender", normalizeGender(body.get("gender")));
        member.put("age", normalizeAge(body.get("age")));
        member.put("phone", body.get("phone"));
        member.put("idCard", body.get("idCard"));
        member.put("relation", body.getOrDefault("relation", "家属"));
        member.put("isDefault", false);
        member.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        member.put("_mock", Collections.emptyList());
        members(ownerPatientId).add(member);
        return member;
    }

    /** 编辑：只能改本人名下的成员；找不到返回 null。 */
    public Map<String, Object> update(String ownerPatientId, String memberId, Map<String, Object> body) {
        Map<String, Object> member = find(ownerPatientId, memberId);
        if (member == null) {
            return null;
        }
        if (body.containsKey("name")) {
            member.put("name", requireName(body));
        }
        if (body.containsKey("gender")) {
            member.put("gender", normalizeGender(body.get("gender")));
        }
        if (body.containsKey("age")) {
            member.put("age", normalizeAge(body.get("age")));
        }
        if (body.containsKey("phone")) {
            member.put("phone", body.get("phone"));
        }
        if (body.containsKey("idCard")) {
            member.put("idCard", body.get("idCard"));
        }
        if (body.containsKey("relation")) {
            member.put("relation", body.get("relation"));
        }
        // 编辑后不再视为演示种子，保留标识一致：去掉 mock 字段声明
        member.put("_mock", Collections.emptyList());
        return member;
    }

    public boolean delete(String ownerPatientId, String memberId) {
        List<Map<String, Object>> list = members(ownerPatientId);
        return list.removeIf(item -> memberId.equals(item.get("patientId")));
    }

    public Map<String, Object> find(String ownerPatientId, String memberId) {
        for (Map<String, Object> item : members(ownerPatientId)) {
            if (memberId.equals(item.get("patientId"))) {
                return item;
            }
        }
        return null;
    }

    private String requireName(Map<String, Object> body) {
        Object name = body.get("name");
        if (name == null || String.valueOf(name).trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        return String.valueOf(name).trim();
    }

    private String normalizeGender(Object gender) {
        String value = gender == null ? "未知" : String.valueOf(gender).trim();
        if (value.isEmpty()) {
            return "未知";
        }
        if ("male".equalsIgnoreCase(value) || "男".equals(value) || "1".equals(value)) {
            return "男";
        }
        if ("female".equalsIgnoreCase(value) || "女".equals(value) || "2".equals(value)) {
            return "女";
        }
        return "未知";
    }

    private Integer normalizeAge(Object age) {
        if (age == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(String.valueOf(age).trim());
            return value >= 0 && value <= 130 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
