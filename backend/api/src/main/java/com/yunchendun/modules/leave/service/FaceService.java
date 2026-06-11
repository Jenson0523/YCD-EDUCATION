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
 *   1. 虹软 ArcSoft 离线SDK（face.api.provider=arcsoft 且激活成功）— 生产精准引擎
 *   2. 图像相似度过渡引擎（ImageHashFaceEngine）— 无需凭证，真实图像比对
 *
 * 关键点：分数来自对"实际上传照片"的真实比对，不再是与照片无关的假分数，
 *        从根本上杜绝"随便一张照片都能高分匹配"。
 */
@Slf4j
@Service
public class FaceService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${face.api.provider:image}")
    private String provider;

    /** ArcSoft 等真实人脸引擎的通过阈值（同一个人置信度） */
    @Value("${face.api.threshold:80.0}")
    private double threshold;

    /** 过渡图像引擎的通过阈值（更严格，降低误判） */
    @Value("${face.api.fallback-threshold:85.0}")
    private double fallbackThreshold;

    private final FaceRecordMapper faceRecordMapper;
    private final List<FaceRecognitionEngine> engines;

    public FaceService(FaceRecordMapper faceRecordMapper, List<FaceRecognitionEngine> engines) {
        this.faceRecordMapper = faceRecordMapper;
        this.engines = engines;
    }

    /** 选出当前应使用的引擎：优先 ArcSoft（就绪），否则过渡引擎 */
    private FaceRecognitionEngine engine() {
        FaceRecognitionEngine fallback = null;
        for (FaceRecognitionEngine e : engines) {
            if ("arcsoft".equalsIgnoreCase(provider) && e.name().startsWith("arcsoft") && e.isReady()) {
                return e;
            }
            if (e.name().startsWith("image-hash")) fallback = e;
        }
        if (fallback != null) return fallback;
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

        // 提取一次抓拍特征
        byte[] capFeature = eng.extractFeature(captured);
        if (capFeature == null) {
            log.warn("刷脸识别：抓拍照片未能提取特征（可能未检测到人脸）");
            return Collections.emptyList();
        }

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (FaceRecord r : library) {
            File enrolled = resolveImageFile(r.getFacePhotoUrl());
            if (enrolled == null) continue;
            byte[] enrFeature = eng.extractFeature(enrolled);
            if (enrFeature == null) continue;
            double score = eng.compareFeature(capFeature, enrFeature);
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
