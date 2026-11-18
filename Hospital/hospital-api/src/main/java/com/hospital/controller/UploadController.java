package com.hospital.controller;

import com.hospital.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传：医院管理员上传文章封面等图片，返回可直接访问的相对地址。
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "webp");

    @Autowired
    private AuthContext authContext;

    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!authContext.isAdmin()) {
                throw new RuntimeException("仅管理员可上传图片");
            }
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("请选择图片");
            }
            String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            int dot = original.lastIndexOf('.');
            String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "";
            if (!ALLOWED_EXT.contains(ext)) {
                throw new RuntimeException("仅支持 jpg / png / webp 图片");
            }

            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dir = Paths.get("uploads", dateDir).toAbsolutePath();
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File target = dir.resolve(fileName).toFile();
            file.transferTo(target);

            result.put("success", true);
            result.put("url", "/uploads/" + dateDir + "/" + fileName);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
