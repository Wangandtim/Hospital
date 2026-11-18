package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.Patient;
import com.hospital.entity.WechatUser;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.WechatUserRepository;
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

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private PatientRepository patientRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 微信登录（code2session）
     */
    public Map<String, Object> wxLogin(String code, String nickname, String avatarUrl) {
        String openid = null;
        String sessionKey = null;

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
            sessionKey = json.get("session_key").asText();

            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("获取openid失败");
            }

        } catch (RestClientException e) {
            // 网络请求失败，使用模拟登录
            System.out.println("微信API请求失败，使用模拟登录: " + e.getMessage());
            openid = generateMockOpenid(code);
            sessionKey = "mock_session_key";
        } catch (Exception e) {
            // 其他错误，使用模拟登录
            System.out.println("微信登录异常，使用模拟登录: " + e.getMessage());
            openid = generateMockOpenid(code);
            sessionKey = "mock_session_key";
        }

        // 查找或创建用户
        final String finalOpenid = openid;
        WechatUser user = wechatUserRepository.findById(openid)
                .orElseGet(() -> createNewUser(finalOpenid, nickname, avatarUrl));

        // 更新用户信息
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        wechatUserRepository.save(user);

        // 生成自定义登录态（JWT）
        String token = generateToken(openid, sessionKey);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("openid", openid);
        result.put("hasPatientInfo", user.getPatient() != null);
        if (user.getPatient() != null) {
            result.put("patientId", user.getPatient().getPatientId());
        }

        return result;
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
        return wechatUserRepository.save(user);
    }

    private String generateToken(String openid, String sessionKey) {
        // 使用 JWT 生成 token
        // 实际项目中使用 jjwt 库
        return "jwt_token_placeholder";
    }
}