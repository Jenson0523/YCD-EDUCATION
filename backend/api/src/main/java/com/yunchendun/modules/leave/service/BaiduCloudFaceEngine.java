package com.yunchendun.modules.leave.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * 百度智能云人脸识别引擎（云端API，免费额度充足，全平台部署）。
 *
 * 优势：
 *   1. 纯 HTTP API，零本地依赖，Linux/Win/macOS 全平台通用
 *   2. 百度官方人脸识别算法，精度高
 *   3. 新用户免费额度 10万次/月（教育场景基本够用）
 *   4. 支持 1:1 人脸比对 + 1:N 人脸搜索
 *   5. OAuth2 简单认证（比腾讯云 TC3-HMAC-SHA256 签名更易接入）
 *
 * 配置（application.yml）：
 *   face.api.provider=baidu
 *   face.api.baidu.api-key=xxxxxxxx
 *   face.api.baidu.secret-key=xxxxxxxx
 *   face.api.baidu.group-id=ycd_students  （人脸库分组ID，用于1:N搜索）
 *
 * 开通流程：
 *   1. 百度智能云控制台 → 人脸识别 → 创建应用
 *      https://console.bce.baidu.com/ai/#/ai/face/overview/index
 *   2. 获取 API Key 和 Secret Key
 *   3. 免费额度：QPS=2，10万次/月（超出后按量计费约 0.0004元/次）
 */
@Slf4j
@Component
public class BaiduCloudFaceEngine implements FaceRecognitionEngine {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String MATCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/match";
    private static final String SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${face.api.baidu.api-key:}")
    private String apiKey;

    @Value("${face.api.baidu.secret-key:}")
    private String secretKey;

    @Value("${face.api.baidu.group-id:ycd_students}")
    private String groupId;

    /** access_token 缓存（有效期30天，运行时自动刷新） */
    private volatile String cachedToken;
    private volatile long tokenExpireTime;

    @Override
    public String name() {
        return "baidu-cloud(百度智能云人脸识别)";
    }

    @Override
    public boolean isReady() {
        return apiKey != null && !apiKey.isBlank()
            && secretKey != null && !secretKey.isBlank();
    }

    @Override
    public byte[] extractFeature(File faceImage) {
        // 百度云为云端API，不需要本地提取特征
        // 返回图片 Base64 作为"特征"标识（供 recognize 流程使用）
        try {
            byte[] imgBytes = Files.readAllBytes(faceImage.toPath());
            String base64 = Base64.getEncoder().encodeToString(imgBytes);
            return base64.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取图片失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public double compareFeature(byte[] featureA, byte[] featureB) {
        // 云端引擎不走本地特征比对，通过 compareImages 直接调用 API
        return 0.0;
    }

    @Override
    public double compareImages(File a, File b) {
        try {
            String imgA = imageToBase64(a);
            String imgB = imageToBase64(b);
            if (imgA == null || imgB == null) return 0.0;

            String token = getAccessToken();
            if (token == null) {
                log.warn("百度云 access_token 获取失败，无法进行人脸比对");
                return 0.0;
            }

            List<Map<String, Object>> faceList = new ArrayList<>();
            Map<String, Object> face1 = new LinkedHashMap<>();
            face1.put("image", imgA);
            face1.put("image_type", "BASE64");
            face1.put("face_type", "LIVE");
            face1.put("quality_control", "NORMAL");
            face1.put("liveness_control", "NONE");
            faceList.add(face1);

            Map<String, Object> face2 = new LinkedHashMap<>();
            face2.put("image", imgB);
            face2.put("image_type", "BASE64");
            face2.put("face_type", "LIVE");
            face2.put("quality_control", "NORMAL");
            face2.put("liveness_control", "NONE");
            faceList.add(face2);

            String body = MAPPER.writeValueAsString(faceList);
            JsonNode resp = postJson(MATCH_URL + "?access_token=" + token, body);
            if (resp == null) return 0.0;

            // 百度接口返回：{"error_code":0, "result":{"score":98.76, "face_list":[...]}}
            int errCode = resp.path("error_code").asInt(-1);
            if (errCode != 0) {
                String errMsg = resp.path("error_msg").asText("unknown");
                log.warn("百度云人脸比对失败: error_code={}, error_msg={}", errCode, errMsg);
                return 0.0;
            }

            double score = resp.path("result").path("score").asDouble(0.0);
            log.info("百度云1:1比对得分: {}", score);
            return score;
        } catch (Exception e) {
            log.error("百度云人脸比对异常: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 1:N 人脸搜索（在人脸库中检索最匹配的人脸）
     * @param faceImage 抓拍照片
     * @param maxResults 最大返回数
     * @return 匹配结果列表 [{userId, score, group_id, user_info}]
     */
    public List<Map<String, Object>> searchFaces(File faceImage, int maxResults) {
        try {
            String imgBase64 = imageToBase64(faceImage);
            if (imgBase64 == null) return Collections.emptyList();

            String token = getAccessToken();
            if (token == null) return Collections.emptyList();

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("image", imgBase64);
            params.put("image_type", "BASE64");
            params.put("group_id_list", groupId);
            params.put("max_face_num", 1);
            params.put("match_threshold", 80);
            params.put("quality_control", "NORMAL");
            params.put("liveness_control", "NONE");
            params.put("max_user_num", maxResults);

            String body = MAPPER.writeValueAsString(params);
            JsonNode resp = postJson(SEARCH_URL + "?access_token=" + token, body);
            if (resp == null) return Collections.emptyList();

            int errCode = resp.path("error_code").asInt(-1);
            if (errCode != 0) {
                String errMsg = resp.path("error_msg").asText("unknown");
                log.warn("百度云人脸搜索失败: error_code={}, error_msg={}", errCode, errMsg);
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            JsonNode userList = resp.path("result").path("user_list");
            if (userList.isArray()) {
                for (JsonNode u : userList) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("userId", u.path("user_id").asText());
                    r.put("score", u.path("score").asDouble());
                    r.put("groupId", u.path("group_id").asText());
                    r.put("userInfo", u.path("user_info").asText());
                    results.add(r);
                }
            }
            results.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
            return results;
        } catch (Exception e) {
            log.error("百度云人脸搜索异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ═══ OAuth2 获取 access_token ═══

    private synchronized String getAccessToken() {
        // 缓存有效则直接返回（提前5分钟刷新，避免边界问题）
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime - 300_000) {
            return cachedToken;
        }

        try {
            String urlStr = TOKEN_URL
                    + "?grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);

            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            String respBody = new String(
                    code == 200 ? conn.getInputStream().readAllBytes() : conn.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            conn.disconnect();

            JsonNode node = MAPPER.readTree(respBody);
            if (node.has("access_token")) {
                cachedToken = node.get("access_token").asText();
                int expiresIn = node.path("expires_in").asInt(2592000); // 默认30天
                tokenExpireTime = System.currentTimeMillis() + expiresIn * 1000L;
                log.info("百度云 access_token 获取成功，有效期至: {}", new Date(tokenExpireTime));
                return cachedToken;
            } else {
                String errDesc = node.path("error_description").asText("unknown");
                log.error("百度云 access_token 获取失败: {}", errDesc);
                return null;
            }
        } catch (Exception e) {
            log.error("百度云 access_token 请求异常: {}", e.getMessage());
            return null;
        }
    }

    // ═══ HTTP 工具 ═══

    private JsonNode postJson(String urlStr, String body) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String respBody = new String(
                    code == 200 ? conn.getInputStream().readAllBytes() : conn.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            conn.disconnect();

            return MAPPER.readTree(respBody);
        } catch (Exception e) {
            log.error("百度云 HTTP 请求异常: {}", e.getMessage());
            return null;
        }
    }

    private String imageToBase64(File f) {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            // 百度人脸接口 Base64 限制 10MB
            if (b64.length() > 13_000_000) {
                log.warn("图片过大(Base64>10MB)，跳过: {}", f.getName());
                return null;
            }
            return b64;
        } catch (Exception e) {
            log.warn("图片读取失败 {}: {}", f.getName(), e.getMessage());
            return null;
        }
    }
}
