package com.yunchendun.common.upload;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 模块: 平台级 / common
 * 功能: 受保护文件预览（人脸照片/凭证/签字等敏感图片）
 *
 * 安全设计：
 *   - /uploads/** 不再作为公开静态资源对外暴露（WebMvcConfig 已移除映射）；
 *   - 所有图片须经本端点访问，校验登录 token 后才输出文件流；
 *   - <image>/<img> 标签无法携带 Header，故支持 ?token= 查询参数鉴权
 *     （token 即登录返回的 tokenValue，由前端 assetUrl()/resolveUrl() 自动拼接）；
 *   - 路径强制限定在上传根目录内，防目录穿越。
 */
@Slf4j
@Tag(name = "受保护文件访问")
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 预览受保护图片。
     * @param path  形如 /uploads/face/2026-06-11/xxx.png（upload 接口返回的相对路径）
     * @param token 登录 token（query 传递，供 image 标签使用；也兼容 Header Authorization）
     */
    @Operation(summary = "预览受保护图片（须登录token）")
    @GetMapping("/preview")
    public void preview(@RequestParam String path,
                        @RequestParam(required = false) String token,
                        HttpServletResponse resp) throws Exception {
        // ── 鉴权：query token 优先，其次走 sa-token 默认（Header/Cookie）──
        boolean authed = false;
        if (StringUtils.hasText(token)) {
            Object loginId = StpUtil.getLoginIdByToken(token);
            authed = loginId != null;
        }
        if (!authed) {
            try { authed = StpUtil.isLogin(); } catch (Exception ignored) {}
        }
        if (!authed) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录或token无效\"}");
            return;
        }

        // ── 路径校验：仅允许 /uploads/ 内的文件，防穿越 ──
        String p = path.trim();
        int idx = p.indexOf("/uploads/");
        if (idx >= 0) p = p.substring(idx + "/uploads/".length());
        else if (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) { resp.setStatus(400); return; }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(p).normalize();
        if (!target.startsWith(root)) { resp.setStatus(400); return; }

        File f = target.toFile();
        if (!f.exists() || !f.isFile()) { resp.setStatus(404); return; }

        // ── 输出文件 ──
        String contentType = Files.probeContentType(target);
        if (contentType == null) contentType = "image/jpeg";
        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "private, max-age=300"); // 仅私有缓存
        resp.setContentLengthLong(f.length());
        Files.copy(target, resp.getOutputStream());
        resp.getOutputStream().flush();
    }
}
