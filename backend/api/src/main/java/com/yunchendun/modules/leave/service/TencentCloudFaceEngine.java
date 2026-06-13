package com.yunchendun.modules.leave.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯云人脸识别引擎（云端API，支持 Linux/Windows 部署）。
 *
 * 优势：
 *   1. 纯 HTTP API，无平台依赖，Linux 服务器可直接部署
 *   2. 腾讯云官方人脸识别算法，精度远高于图像哈希过渡引擎
 *   3. 支持 1:1 人脸比对 + 1:N 人脸搜索
 *   4. 按量计费，教育场景低成本
 *
 * 配置（application.yml）：
 *   face.api.provider=tencent
 *   face.api.tencent.secret-id=AKIDxxxxx
 *   face.api.tencent.secret-key=xxxxxxxx
 *   face.api.tencent.region=ap-guangzhou
 *   face.api.tencent.group-id=ycd-students  （人员库ID，用于1:N搜索）
 */
@Slf4j
@Component
public class TencentCloudFaceEngine implements FaceRecognitionEngine {

    private static final String ENDPOINT = "iai.tencentcloudapi.com";
    private static final String SERVICE = "iai";
    private static final String VERSION = "2020-03-03";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${face.api.tencent.secret-id:}")
    private String secretId;

    @Value("${face.api.tencent.secret-key:}")
    private String secretKey;

    @Value("${face.api.tencent.region:ap-guangzhou}")
    private String region;

    @Value("${face.api.tencent.group-id:ycd-students}")
    private String groupId;

    @Value("${face.api.tencent.quality-control:2}")
    private int qualityControl;

    @Override
    public String name() {
        return "tencent-cloud(腾讯云人脸识别)";
    }

    @Override
    public boolean isReady() {
        return secretId != null && !secretId.isBlank()
            && secretKey != null && !secretKey.isBlank();
    }

    @Override
    public byte[] extractFeature(File faceImage) {
        // 腾讯云为云端API，不需要本地提取特征
        // 直接返回图片的 Base64 字节作为"特征"标识
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

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("ImageA", imgA);
            params.put("ImageB", imgB);
            params.put("QualityControl", qualityControl);

            JsonNode resp = callApi("CompareFace", params);
            if (resp == null) return 0.0;

            double score = resp.path("Response").path("Score").asDouble(0.0);
            log.info("腾讯云1:1比对得分: {}", score);
            return score;
        } catch (Exception e) {
            log.error("腾讯云人脸比对失败: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 1:N 人脸搜索（在人员库中检索最匹配的人脸）
     * @param faceImage 抓拍照片
     * @param maxResults 最大返回数
     * @return 匹配结果列表 [{personId, score, faceId}]
     */
    public List<Map<String, Object>> searchFaces(File faceImage, int maxResults) {
        try {
            String imgBase64 = imageToBase64(faceImage);
            if (imgBase64 == null) return Collections.emptyList();

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("Image", imgBase64);
            params.put("GroupIds", Collections.singletonList(groupId));
            params.put("MaxPersonNum", maxResults);
            params.put("MaxFaceNum", 1);
            params.put("QualityControl", qualityControl);
            params.put("NeedPersonInfo", 1);

            JsonNode resp = callApi("SearchFaces", params);
            if (resp == null) return Collections.emptyList();

            List<Map<String, Object>> results = new ArrayList<>();
            JsonNode candidates = resp.path("Response").path("Results");
            if (candidates.isArray()) {
                for (JsonNode c : candidates) {
                    JsonNode top = c.path("Candidates").get(0);
                    if (top.isMissingNode()) continue;
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("personId", top.path("PersonId").asText());
                    r.put("score", top.path("Score").asDouble());
                    r.put("faceId", top.path("FaceId").asText());
                    r.put("personName", top.path("PersonName").asText());
                    results.add(r);
                }
            }
            results.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
            return results;
        } catch (Exception e) {
            log.error("腾讯云人脸搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ═══ TC3-HMAC-SHA256 签名 + API 调用 ═══

    private JsonNode callApi(String action, Map<String, Object> params) {
        try {
            String payload = MAPPER.writeValueAsString(params);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(Long.parseLong(timestamp) * 1000));

            // 1. CanonicalRequest
            String httpMethod = "POST";
            String canonicalUri = "/";
            String canonicalQuery = "";
            String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                    + "host:" + ENDPOINT + "\n"
                    + "x-tc-action:" + action.toLowerCase() + "\n";
            String signedHeaders = "content-type;host;x-tc-action";
            String hashedPayload = sha256Hex(payload);
            String canonicalRequest = httpMethod + "\n" + canonicalUri + "\n" + canonicalQuery + "\n"
                    + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedPayload;

            // 2. StringToSign
            String credentialScope = date + "/" + SERVICE + "/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n"
                    + sha256Hex(canonicalRequest);

            // 3. Signature
            byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmacSha256(secretDate, SERVICE);
            byte[] secretSigning = hmacSha256(secretService, "tc3_request");
            String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

            // 4. Authorization header
            String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                    + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

            // 5. HTTP Request
            URL url = URI.create("https://" + ENDPOINT).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Host", ENDPOINT);
            conn.setRequestProperty("X-TC-Action", action);
            conn.setRequestProperty("X-TC-Version", VERSION);
            conn.setRequestProperty("X-TC-Timestamp", timestamp);
            conn.setRequestProperty("X-TC-Region", region);
            conn.setRequestProperty("Authorization", authorization);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String body = new String(
                    code == 200 ? conn.getInputStream().readAllBytes() : conn.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(body);

            if (code != 200 || node.has("Error")) {
                String errMsg = node.has("Error")
                        ? node.path("Error").path("Message").asText("unknown")
                        : "HTTP " + code;
                log.warn("腾讯云API {} 失败: {}", action, errMsg);
                return null;
            }
            return node;
        } catch (Exception e) {
            log.error("腾讯云API调用异常 {}: {}", action, e.getMessage());
            return null;
        }
    }

    private String imageToBase64(File f) {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            // 腾讯云要求 Base64 不超过 7MB，这里简单校验
            if (b64.length() > 7 * 1024 * 1024 / 3 * 4) {
                log.warn("图片过大(base64>7MB)，跳过: {}", f.getName());
                return null;
            }
            return b64;
        } catch (Exception e) {
            log.warn("图片读取失败 {}: {}", f.getName(), e.getMessage());
            return null;
        }
    }

    // ═══ 加密工具 ═══

    private static String sha256Hex(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
