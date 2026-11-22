package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.Patient;
import com.hospital.entity.WechatUser;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.WechatUserRepository;
import com.hospital.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WechatAuthService {

    @Value("${wechat.miniapp.app-id}")
    private String appId;

    @Value("${wechat.miniapp.app-secret}")
    private String appSecret;

    /** true：微信接口不可用时回退模拟登录（仅本地演示）；默认 false，失败如实报错。 */
    @Value("${wechat.miniapp.mock-fallback:false}")
    private boolean mockFallback;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 微信登录（code2session）
     */
    public Map<String, Object> wxLogin(String code, String nickname, String avatarUrl) {
        String openid = null;

        try {
            // 调用微信接口获取 openid 和 session_key
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appId, appSecret, code
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = mapper.readTree(response.getBody());

            // 检查微信API是否返回错误
            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                String errmsg = json.has("errmsg") ? json.get("errmsg").asText() : "未知错误";
                throw new RuntimeException("微信API错误: " + errmsg);
            }

            openid = json.get("openid").asText();

            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("获取openid失败");
            }

        } catch (RestClientException e) {
            if (!mockFallback) {
                throw new RuntimeException("微信接口请求失败: " + e.getMessage(), e);
            }
            // 网络请求失败，使用模拟登录
            System.out.println("微信API请求失败，使用模拟登录: " + e.getMessage());
            openid = generateMockOpenid(code);
        } catch (Exception e) {
            if (!mockFallback) {
                throw new RuntimeException("微信登录失败: " + e.getMessage(), e);
            }
            // 其他错误，使用模拟登录
            System.out.println("微信登录异常，使用模拟登录: " + e.getMessage());
            openid = generateMockOpenid(code);
        }

        // 查找或创建微信用户；首次出现时同步创建患者档案并绑定
        final String finalOpenid = openid;
        WechatUser user = wechatUserRepository.findById(openid)
                .orElseGet(() -> createNewUser(finalOpenid, nickname, avatarUrl));

        // 更新用户信息（昵称/头像为空时不覆盖已有值）
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        if (user.getPatient() == null) {
            Patient patient = createPatientForUser(user.getNickname());
            user.setPatient(patient);
        }
        wechatUserRepository.save(user);

        Patient patient = user.getPatient();

        // 签发与手机号验证码登录一致的患者令牌
        String token = tokenService.issue("PATIENT", patient.getPatientId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("openid", openid);
        result.put("patientId", patient.getPatientId());
        result.put("patientName", patient.getName());
        result.put("phone", patient.getPhone());
        result.put("gender", patient.getGender());
        result.put("age", patient.getAge());
        result.put("medicalHistory", patient.getMedicalHistory());
        result.put("hasCompleteInfo", patient.getName() != null && patient.getGender() != null
                && patient.getAge() != null && patient.getAge() > 0);
        result.put("hasPatientInfo", true);

        return result;
    }

    /** 首次微信登录时创建患者档案：昵称作为姓名，性别/年龄待用户补充。 */
    private Patient createPatientForUser(String nickname) {
        Patient patient = new Patient();
        patient.setPatientId(newPatientId());
        patient.setName(nickname != null && !nickname.isEmpty() ? nickname : "微信用户");
        patient.setCreateTime(LocalDateTime.now());
        return patientRepository.save(patient);
    }

    /** 患者编号沿用 P + 6 位数字的规则，冲突时重试。 */
    private String newPatientId() {
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 50; i++) {
            String candidate = "P" + String.format("%06d", random.nextInt(1_000_000));
            if (!patientRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("患者编号生成失败，请重试");
    }

    private String generateMockOpenid(String code) {
        return "mock_openid_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 绑定患者信息
     */
    public Patient bindPatient(String openid, Patient patientInfo) {
        WechatUser user = wechatUserRepository.findById(openid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建或查找患者
        Patient patient = patientRepository.findByPhone(patientInfo.getPhone())
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setPatientId("P" + System.currentTimeMillis() % 100000);
                    p.setName(patientInfo.getName());
                    p.setPhone(patientInfo.getPhone());
                    p.setGender(patientInfo.getGender());
                    p.setAge(patientInfo.getAge());
                    p.setCreateTime(LocalDateTime.now());
                    return patientRepository.save(p);
                });

        user.setPatient(patient);
        wechatUserRepository.save(user);

        return patient;
    }

    /**
     * 更新用户位置
     */
    public void updateLocation(String openid, Double latitude, Double longitude) {
        WechatUser user = wechatUserRepository.findById(openid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setLastLatitude(latitude);
        user.setLastLongitude(longitude);
        user.setLastLocationTime(LocalDateTime.now());
        wechatUserRepository.save(user);
    }

    private WechatUser createNewUser(String openid, String nickname, String avatarUrl) {
        WechatUser user = new WechatUser();
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
}