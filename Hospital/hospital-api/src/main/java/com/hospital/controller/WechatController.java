package com.hospital.controller;

import com.hospital.service.WechatAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wechat")
@CrossOrigin(origins = "*")
public class WechatController {

    @Autowired
    private WechatAuthService wechatAuthService;

    @PostMapping("/login")
    public Map<String, Object> wechatLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String nickname = request.get("nickname");
        String avatarUrl = request.get("avatarUrl");
        
        System.out.println("收到微信登录请求, code: " + code + ", nickname: " + nickname);
        
        Map<String, Object> result = new HashMap<>();
        
        if (code == null || code.isEmpty()) {
            result.put("success", false);
            result.put("message", "code不能为空");
            return result;
        }
        
        try {
            Map<String, Object> loginResult = wechatAuthService.wxLogin(code, nickname, avatarUrl);
            result.putAll(loginResult);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            System.err.println("微信登录失败: " + e.getMessage());
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/location")
    public Map<String, Object> updateLocation(@RequestHeader(value = "X-Openid", required = false) String openid,
                                             @RequestBody Map<String, Double> location) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("收到位置更新请求, openid: " + openid + ", 位置: " + location);
        result.put("success", true);
        result.put("message", "位置更新成功");
        return result;
    }
}