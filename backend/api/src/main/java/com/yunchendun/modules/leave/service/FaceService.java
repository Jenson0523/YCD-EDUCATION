package com.yunchendun.modules.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.leave.domain.FaceRecord;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

/**
 * 人脸识别服务（门卫核验）。
 *
 * 引擎策略（按优先级自动择优）：
 *   1. 百度智能云人脸识别API（face.api.provider=baidu 且密钥有效）— 🟢免费额度充足，全平台通用
 *   2. 腾讯云人脸识别API（face.api.provider=tencent 且密钥有效）— 付费云端引擎，全平台通用
 *   3. 虹软 ArcSoft 离线SDK（face.api.provider=arcsoft 且激活成功）— Windows生产引擎（不推荐Linux）
 *   4. 图像相似度过渡引擎（ImageHashFaceEngine）— 无需凭证，本地比对，兜底
 *
 * 推荐：使用 baidu 模式（新用户10万次/月免费额度），Linux 服务器直接部署。
 */
@Slf4j
@Service
public class FaceService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /** 引擎提供商标识：baidu / tencent / arcsoft / image */
    @Value("${face.api.provider:baidu}")
    private String provider;

    /** 腾讯云等云端引擎的通过阈值 */
    @Value("${face.api.threshold:80.0}")
    private double threshold;

    /** 过渡图像引擎的通过阈值（更严格，降低误判） */
    @Value("${face.api.fallback-threshold:85.0}")
    private double fallbackThreshold;

    private final FaceRecordMapper faceRecordMapper;
    private final List<FaceRecognitionEngine> engines;
    private final TencentCloudFaceEngine tencentEngine;
    private final BaiduCloudFaceEngine baiduEngine;

    public FaceService(FaceRecordMapper faceRecordMapper, List<FaceRecognitionEngine> engines,
                       TencentCloudFaceEngine tencentEngine, BaiduCloudFaceEngine baiduEngine) {
        this.faceRecordMapper = faceRecordMapper;
        this.engines = engines;
        this.tencentEngine = tencentEngine;
        this.baiduEngine = baiduEngine;
    }

    /** 选出当前应使用的引擎：优先百度云 → 腾讯云 → ArcSoft → 图像哈希兜底 */
    private FaceRecognitionEngine engine() {
        // 1. 百度智能云引擎（推荐：免费额度10万次/月）
        if ("baidu".equalsIgnoreCase(provider) && baiduEngine.isReady()) {
            return baiduEngine;
        }
        // 2. 腾讯云云端引擎
        if ("tencent".equalsIgnoreCase(provider) && tencentEngine.isReady()) {
            return tencentEngine;
        }
        // 3. 虹软 ArcSoft 离线引擎（仅 Windows，需 SDK）
        if ("arcsoft".equalsIgnoreCase(provider)) {
            for (FaceRecognitionEngine e : engines) {
                if (e.name().startsWith("arcsoft") && e.isReady()) return e;
            }
        }
        // 4. 图像哈希过渡引擎（兜底）
        for (FaceRecognitionEngine e : engines) {
            if (e.name().startsWith("image-hash")) return e;
        }
        return engines.isEmpty() ? null : engines.get(0);
    }

    /** 当前引擎对应的通过阈值 */
    private double activeThreshold(FaceRecognitionEngine e) {
        return e != null && e.name().startsWith("image-hash") ? fallbackThreshold : threshold;
    }

    // ═══ 对外接口 ═══

    /** 1:1 人脸比对（核验指定学籍号 vs 抓拍照片） */
    public Map<String, Object> compare(String studentNo, String capturePhotoUrl) {
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>()
                        .eq(FaceRecord::getStudentNo, studentNo)
                        .eq(FaceRecord::getStatus, "ACTIVE"));
        if (rec == null || !StringUtils.hasText(rec.getFacePhotoUrl())) {
            return Map.of("score", 0.0, "passed", false, "studentId", 0L,
                    "realName", "", "facePhotoUrl", "", "message", "人脸档案不存在或未录入照片");
        }

        File enrolled = resolveImageFile(rec.getFacePhotoUrl());
        File captured = resolveImageFile(capturePhotoUrl);
        if (enrolled == null) {
            return Map.of("score", 0.0, "passed", false, "studentId",
                    rec.getStudentId() != null ? rec.getStudentId() : 0L,
                    "realName", rec.getRealName() != null ? rec.getRealName() : "",
                    "facePhotoUrl", rec.getFacePhotoUrl(), "message", "档案照片文件缺失");
        }
        if (captured == null) {
            return Map.of("score", 0.0, "passed", false, "studentId",
                    rec.getStudentId() != null ? rec.getStudentId() : 0L,
                    "realName", rec.getRealName() != null ? rec.getRealName() : "",
                    "facePhotoUrl", rec.getFacePhotoUrl(), "message", "抓拍照片缺失，请重新刷脸");
        }

        FaceRecognitionEngine eng = engine();
        double score = eng == null ? 0.0 : eng.compareImages(enrolled, captured);
        double th = activeThreshold(eng);
        boolean passed = score >= th;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("score", Math.round(score * 10.0) / 10.0);
        r.put("passed", passed);
        r.put("threshold", th);
        r.put("engine", eng == null ? "none" : eng.name());
        r.put("studentId", rec.getStudentId() != null ? rec.getStudentId() : 0L);
        r.put("realName", rec.getRealName() != null ? rec.getRealName() : "");
        r.put("facePhotoUrl", rec.getFacePhotoUrl());
        r.put("message", passed ? "人脸比对通过" : "人脸比对不匹配（" + Math.round(score * 10.0) / 10.0 + "分 < " + th + "）");
        return r;
    }

    /** 1:N 人脸检索（门卫刷脸：在人脸库中找出与抓拍照片匹配的学生） */
    public List<Map<String, Object>> recognize(List<FaceRecord> library, String capturePhotoUrl) {
        if (library == null || library.isEmpty()) return Collections.emptyList();

        File captured = resolveImageFile(capturePhotoUrl);
        FaceRecognitionEngine eng = engine();
        if (captured == null || eng == null) {
            log.warn("刷脸识别：抓拍照片缺失或无可用引擎，captured={}, engine={}", captured, eng);
            return Collections.emptyList();
        }
        double th = activeThreshold(eng);

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (FaceRecord r : library) {
            File enrolled = resolveImageFile(r.getFacePhotoUrl());
            if (enrolled == null) continue;
            // 统一用 compareImages：本地引擎走特征比对，云端引擎(百度/腾讯)走API直比，
            // 避免云端 compareFeature 恒为0导致1:N全部识别失败
            double score = eng.compareImages(captured, enrolled);
            if (score < th) continue; // 低于阈值不作为候选，杜绝误判

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", r.getStudentId());
            m.put("studentNo", r.getStudentNo());
            m.put("realName", r.getRealName());
            m.put("className", r.getClassName());
            m.put("gradeName", r.getGradeName());
            m.put("headTeacherName", r.getHeadTeacherName());
            m.put("facePhotoUrl", r.getFacePhotoUrl());
            m.put("score", Math.round(score * 10.0) / 10.0);
            candidates.add(m);
        }
        // 按分数降序，取 Top-5
        candidates.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return candidates.size() > 5 ? candidates.subList(0, 5) : candidates;
    }

    /** 当前引擎信息（供前端/调试展示） */
    public Map<String, Object> engineInfo() {
        FaceRecognitionEngine eng = engine();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", provider);
        m.put("engine", eng == null ? "none" : eng.name());
        m.put("ready", eng != null && eng.isReady());
        m.put("threshold", activeThreshold(eng));
        return m;
    }

    // ═══ 工具 ═══

    /** 将 /uploads/xxx 的访问URL解析为本地文件 */
    private File resolveImageFile(String url) {
        if (!StringUtils.hasText(url)) return null;
        String rel = url;
        int idx = rel.indexOf("/uploads/");
        if (idx >= 0) {
            rel = rel.substring(idx + "/uploads/".length());
        } else if (rel.startsWith("http")) {
            return null; // 外链不处理
        } else if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        try {
            File f = Paths.get(uploadDir, rel).toAbsolutePath().normalize().toFile();
            return f.exists() && f.isFile() ? f : null;
        } catch (Exception e) {
            return null;
        }
    }
}
