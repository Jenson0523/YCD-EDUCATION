package com.yunchendun.common.upload;

import com.yunchendun.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 模块: 平台级 / common
 * 功能: 通用图片/文件上传（人脸照片、凭证等），保存到本地并返回可访问URL
 *   - 本期：本地磁盘存储
 *   - TODO: 后续可替换为 阿里云OSS / 腾讯云COS
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 上传根目录，默认项目运行目录下 ./uploads */
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png", "webp", "bmp"};

    /**
     * 上传图片（人脸照片 / 凭证照片）
     * @param category 业务分类目录，如 face / proof
     * @return { url: "/uploads/face/2026-06-10/xxx.jpg", absoluteUrl: "http://host/uploads/..." }
     */
    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "common") String category,
            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(400, "上传文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return ApiResponse.fail(400, "图片大小不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        boolean allowed = false;
        for (String e : ALLOWED_EXT) if (e.equals(ext)) { allowed = true; break; }
        if (!allowed) {
            return ApiResponse.fail(400, "仅支持 jpg/png/webp/bmp 格式图片");
        }

        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // 安全分类目录名
            String safeCategory = category.replaceAll("[^a-zA-Z0-9_-]", "");
            if (safeCategory.isBlank()) safeCategory = "common";

            // 解析为绝对路径，避免 transferTo 相对路径落到 Tomcat 临时目录
            Path dir = Paths.get(uploadDir, safeCategory, datePath).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File dest = dir.resolve(filename).toFile();
            file.transferTo(dest);

            // 对外访问相对路径
            String relUrl = "/uploads/" + safeCategory + "/" + datePath + "/" + filename;
            // 绝对URL（小程序/前端可直接用）
            String scheme = request.getScheme();
            String host = request.getServerName();
            int port = request.getServerPort();
            String absoluteUrl = scheme + "://" + host + ":" + port + relUrl;

            Map<String, String> data = new HashMap<>();
            data.put("url", relUrl);
            data.put("absoluteUrl", absoluteUrl);
            return ApiResponse.ok(data);
        } catch (IOException e) {
            return ApiResponse.fail(500, "上传失败: " + e.getMessage());
        }
    }
}
