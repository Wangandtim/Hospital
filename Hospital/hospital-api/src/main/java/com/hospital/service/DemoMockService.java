package com.hospital.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 演示用 mock 数据源。
 * 开关：hospital.demo.mock.enabled / 环境变量 DEMO_MOCK_ENABLED，默认开启。
 * 关闭后所有 mock 字段返回 null，真实字段不受影响；接口用 _mock / meta.mock 标识。
 */
@Service
public class DemoMockService {

    @Value("${hospital.demo.mock.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    /** mock 长文本统一前缀，便于一眼识别；医院名/科室名等业务名称不加前缀。 */
    public String text(String value) {
        if (!enabled || value == null) {
            return null;
        }
        return "MOCK-" + value;
    }

    public List<String> fields(String... names) {
        return enabled ? new ArrayList<>(Arrays.asList(names)) : new ArrayList<>();
    }

    private int idx(String key, int n) {
        if (key == null) {
            return 0;
        }
        return Math.abs(key.hashCode() % n);
    }

    private String pick(String key, String... values) {
        return values[idx(key, values.length)];
    }

    /** 医院扩展字段（院区、性质、院徽、门诊时间、电话等）。 */
    public void putHospitalExtras(Map<String, Object> target, String hospitalId) {
        if (!enabled) {
            return;
        }
        target.put("campus", pick(hospitalId, "南院区", "北院区", "东院区"));
        target.put("nature", pick(hospitalId, "公立综合", "公立专科", "民营综合"));
        target.put("logoUrl", null);
        target.put("outpatientHours", "08:00–17:30");
        target.put("guideDeskPhone", phone("029-8763", hospitalId, 100));
        target.put("emergencyPhone", phone("029-8763", hospitalId, 900));
        target.put("emergency24h", true);
        target.put("insuranceSupported", true);
        target.put("onlineBooking", true);
        target.put("rating", 4.5 + idx(hospitalId, 5) / 10.0);
    }

    private String phone(String prefix, String key, int offset) {
        int n = 1000 + idx(key, 800) + offset;
        return prefix + " " + n;
    }

    public void putDeptExtras(Map<String, Object> target, String deptId) {
        if (!enabled) {
            return;
        }
        target.put("location", "门诊楼 " + (2 + idx(deptId, 4)) + " 层 A 区");
        target.put("guideInfo", Map.of(
                "location", "门诊 1 层大厅",
                "phone", phone("029-8763", deptId, 0),
                "services", List.of("盖章", "病历复印", "轮椅借用")));
    }

    public void putDoctorExtras(Map<String, Object> target, String doctorId, String title) {
        if (!enabled) {
            return;
        }
        List<String> tags = new ArrayList<>();
        if (title != null && title.contains("主任医师") && !title.contains("副主任")) {
            tags.add("科主任");
            tags.add("专家门诊");
        } else if (title != null && title.contains("副主任医师")) {
            tags.add("专家门诊");
        } else if (title != null && title.contains("主治")) {
            tags.add("普通门诊");
        }
        target.put("tags", tags);
        target.put("avatarUrl", null);
        target.put("rating", 4.5 + idx(doctorId, 5) / 10.0);
        target.put("reviewCount", 40 + idx(doctorId, 180));
    }

    public void putSlotExtras(Map<String, Object> target, String scheduleId) {
        if (!enabled) {
            return;
        }
        target.put("insuranceNote", "医保可报 50%");
        target.put("location", "门诊楼 " + (2 + idx(scheduleId, 4)) + " 层 A 区");
    }

    public void putAppointmentExtras(Map<String, Object> target, String appointmentId) {
        if (!enabled) {
            return;
        }
        target.put("queueNo", 1 + idx(appointmentId, 40));
        target.put("location", "门诊楼 " + (2 + idx(appointmentId, 4)) + " 层 A 区");
        target.put("smsSentTo", "138****6621");
    }

    /** 首页常用症状（mock，按关键词映射到科室名）。 */
    public List<Map<String, Object>> commonSymptoms() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!enabled) {
            return list;
        }
        list.add(symptom("咳嗽", "呼吸内科"));
        list.add(symptom("发热", "发热门诊"));
        list.add(symptom("头痛", "神经内科"));
        list.add(symptom("腹痛", "消化内科"));
        list.add(symptom("失眠", "神经内科"));
        return list;
    }

    private Map<String, Object> symptom(String text, String deptName) {
        return Map.of("text", text, "deptName", deptName);
    }
}
