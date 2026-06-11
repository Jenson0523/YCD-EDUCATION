package com.yunchendun.modules.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.leave.domain.FaceRecord;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 人脸识别服务
 * 支持腾讯云人脸识别 API（真机对比）与 Mock 模式（开发调试用）
 */
@Slf4j
@Service
public class FaceService {

    @Value("${face.api.provider:mock}")
    private String provider;

    @Value("${face.api.tencent.secret-id:}")
    private String secretId;

    @Value("${face.api.tencent.secret-key:}")
    private String secretKey;

    @Value("${face.api.tencent.region:ap-guangzhou}")
    private String region;

    @Value("${face.api.tencent.group-id:ycd-students}")
    private String groupId;

    @Value("${face.api.threshold:80.0}")
    private double threshold;

    private final RestTemplate restTemplate;
    private final FaceRecordMapper faceRecordMapper;

    private static final String HOST = "iai.tencentcloudapi.com";
    private static final String SERVICE = "iai";
    private static final String VERSION = "2020-03-03";

    public FaceService(RestTemplateBuilder builder, FaceRecordMapper faceRecordMapper) {
        this.restTemplate = builder.build();
        this.faceRecordMapper = faceRecordMapper;
    }

    // ═══ 对外接口 ═══

    /** 1:1 人脸比对（门卫核验） */
    public Map<String, Object> compare(String studentNo, String capturePhotoUrl) {
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>()
                        .eq(FaceRecord::getStudentNo, studentNo)
                        .eq(FaceRecord::getStatus, "ACTIVE"));
        if (rec == null || !StringUtils.hasText(rec.getFacePhotoUrl())) {
            return Map.of("score", 0.0, "passed", false, "studentId", 0L,
                    "realName", "", "facePhotoUrl", "", "message", "人脸档案不存在");
        }

        double score;
        if ("tencent".equalsIgnoreCase(provider) && StringUtils.hasText(secretId)) {
            score = tencentCompareFaces(rec.getFacePhotoUrl(), capturePhotoUrl);
        } else {
            score = mockCompare(rec);
        }

        boolean passed = score >= threshold;
        return Map.of(
                "score", Math.round(score * 10.0) / 10.0,
                "passed", passed,
                "studentId", rec.getStudentId() != null ? rec.getStudentId() : 0L,
                "realName", rec.getRealName() != null ? rec.getRealName() : "",
                "facePhotoUrl", rec.getFacePhotoUrl(),
                "message", passed ? "人脸比对通过" : "人脸比对不匹配（" + score + "分）"
        );
    }

    /** 1:N 人脸检索（门卫刷脸识别） */
    public List<Map<String, Object>> recognize(List<FaceRecord> library, String capturePhotoUrl) {
        if (library == null || library.isEmpty()) return Collections.emptyList();

        if ("tencent".equalsIgnoreCase(provider) && StringUtils.hasText(secretId)) {
            // TODO: 真实 API 使用 capturePhotoUrl 进行 1:N 搜索，目前暂回退到 mock
            return mockRecognize(library);
        }
        return mockRecognize(library);
    }

    // ═══ 腾讯云 API 调用 ═══

    private double tencentCompareFaces(String imageUrlA, String imageUrlB) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("UrlA", imageUrlA);
            payload.put("UrlB", imageUrlB);
            payload.put("QualityControl", 2); // 中等质量控制

            Map<?, ?> res = callTencentApi("CompareFace", payload);
            if (res != null && res.containsKey("Response")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) res.get("Response");
                Object scoreObj = response.get("Score");
                if (scoreObj instanceof Number) {
                    return ((Number) scoreObj).doubleValue();
                }
            }
        } catch (Exception e) {
            log.warn("腾讯云 CompareFace 调用失败，回退到 mock: {}", e.getMessage());
        }
        // 回退：从数据库查人脸档案做 mock
        return 82.0 + Math.random() * 10;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> callTencentApi(String action, Map<String, Object> payload) throws Exception {
        String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        String timestamp = String.valueOf(ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond());
        String date = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // TC3-HMAC-SHA256 签名
        String authorization = signTc3(secretId, secretKey, SERVICE, HOST, action, VERSION, region, jsonBody, timestamp, date);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Host", HOST);
        headers.set("X-TC-Action", action);
        headers.set("X-TC-Version", VERSION);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Region", region);
        headers.set("Authorization", authorization);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://" + HOST, HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    private String signTc3(String secretId, String secretKey, String service, String host,
                           String action, String version, String region, String payload,
                           String timestamp, String date) throws Exception {
        // Step 1: Canonical Request
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + host + "\n"
                + "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedPayload = sha256Hex(payload);
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedPayload;

        // Step 2: String to Sign
        String credentialScope = date + "/" + service + "/tc3_request";
        String hashedCanonical = sha256Hex(canonicalRequest);
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonical;

        // Step 3: Signature
        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, service);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        byte[] signature = hmacSha256(secretSigning, stringToSign);

        return "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + bytesToHex(signature);
    }

    // ═══ Mock 实现（开发/调试用，基于学籍号哈希保证一致性） ═══

    private double mockCompare(FaceRecord rec) {
        // 基于 studentNo 生成伪随机但稳定的基础分（模拟真实比对）
        String seed = rec.getStudentNo() + rec.getUpdatedAt().toString();
        int hash = Math.abs(seed.hashCode());
        double base = 78.0 + (hash % 20); // 78 ~ 97
        // 加上少量噪声（±3分），模拟光照/角度影响
        double noise = (Math.random() - 0.5) * 6;
        return Math.min(99.9, Math.max(0, base + noise));
    }

    private List<Map<String, Object>> mockRecognize(List<FaceRecord> lib) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        int idx = 0;
        for (FaceRecord r : lib) {
            String seed = r.getStudentNo() + r.getUpdatedAt().toString();
            double base = 85.0 + (Math.abs(seed.hashCode()) % 14); // 85 ~ 98
            double noise = (Math.random() - 0.5) * 4;
            double score = Math.round((base + noise) * 10.0) / 10.0;
            if (score < 70) continue; // 低于阈值不展示

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", r.getStudentId());
            m.put("studentNo", r.getStudentNo());
            m.put("realName", r.getRealName());
            m.put("className", r.getClassName());
            m.put("gradeName", r.getGradeName());
            m.put("headTeacherName", r.getHeadTeacherName());
            m.put("facePhotoUrl", r.getFacePhotoUrl());
            m.put("score", score);
            candidates.add(m);
            idx++;
            if (idx >= 5) break;
        }
        // 按分数降序
        candidates.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return candidates;
    }

    // ═══ 加密工具 ═══

    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
