package com.yunchendun.modules.leave.service;

import com.arcsoft.face.EngineConfiguration;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectOrient;
import com.arcsoft.face.enums.ErrorInfo;
import com.arcsoft.face.enums.ImageFormat;
import com.arcsoft.face.toolkit.ImageInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.arcsoft.face.toolkit.ImageFactory.getRGBData;

/**
 * 虹软 ArcSoft 离线人脸引擎（生产精准引擎）。
 *
 * 本类位于 src/arcsoft/java，仅当 backend/api/libs/arcsoft-sdk-face.jar 存在时，
 * 由 pom.xml 的 arcsoft profile 自动加入编译；否则不参与构建，系统回退到图像过渡引擎。
 *
 * 准备步骤见 docs/ARCSOFT_SETUP.md。
 */
@Slf4j
@Component
public class ArcSoftFaceEngine implements FaceRecognitionEngine {

    @Value("${face.api.arcsoft.app-id:}")
    private String appId;

    @Value("${face.api.arcsoft.sdk-key:}")
    private String sdkKey;

    /** 存放 libarcsoft_face*.dll / .so 的目录 */
    @Value("${face.api.arcsoft.lib-path:./libs/arcsoft}")
    private String libPath;

    private FaceEngine faceEngine;
    private volatile boolean ready = false;

    @PostConstruct
    public void init() {
        if (appId == null || appId.isBlank() || sdkKey == null || sdkKey.isBlank()) {
            log.warn("[ArcSoft] 未配置 app-id/sdk-key，离线引擎不启用，回退过渡引擎");
            return;
        }
        try {
            // ArcSoft 要求传入 DLL/SO 目录的【绝对路径】，相对路径会报 UnsatisfiedLinkError
            String absLibPath = new File(libPath).getAbsolutePath();
            log.info("[ArcSoft] 加载native库目录: {}", absLibPath);
            faceEngine = new FaceEngine(absLibPath);
            int active = faceEngine.activeOnline(appId, sdkKey);
            if (active != ErrorInfo.MOK.getValue() && active != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED.getValue()) {
                log.error("[ArcSoft] 激活失败 code={}，回退过渡引擎", active);
                return;
            }
            EngineConfiguration cfg = new EngineConfiguration();
            cfg.setDetectMode(DetectMode.ASF_DETECT_MODE_IMAGE);
            cfg.setDetectFaceOrientPriority(DetectOrient.ASF_OP_ALL_OUT);
            cfg.setDetectFaceMaxNum(5);
            cfg.setDetectFaceScaleVal(16);
            FunctionConfiguration fc = new FunctionConfiguration();
            fc.setSupportFaceDetect(true);
            fc.setSupportFaceRecognition(true);
            cfg.setFunctionConfiguration(fc);

            int init = faceEngine.init(cfg);
            if (init != ErrorInfo.MOK.getValue()) {
                log.error("[ArcSoft] 初始化失败 code={}，回退过渡引擎", init);
                return;
            }
            ready = true;
            log.info("[ArcSoft] 离线人脸引擎已就绪 ✓ (libPath={})", libPath);
        } catch (Throwable t) {
            log.error("[ArcSoft] 引擎加载异常（检查DLL/SO是否就位）: {}", t.getMessage(), t);
        }
    }

    @Override
    public String name() { return "arcsoft(虹软离线)"; }

    @Override
    public boolean isReady() { return ready; }

    @Override
    public synchronized byte[] extractFeature(File faceImage) {
        if (!ready) return null;
        try {
            ImageInfo img = getRGBData(faceImage);
            List<FaceInfo> faces = new ArrayList<>();
            int dr = faceEngine.detectFaces(img.getImageData(), img.getWidth(), img.getHeight(),
                    ImageFormat.CP_PAF_BGR24, faces);
            if (dr != ErrorInfo.MOK.getValue() || faces.isEmpty()) {
                log.warn("[ArcSoft] 未检测到人脸 {} (code={}, faces={})", faceImage.getName(), dr, faces.size());
                return null;
            }
            FaceFeature feature = new FaceFeature();
            int er = faceEngine.extractFaceFeature(img.getImageData(), img.getWidth(), img.getHeight(),
                    ImageFormat.CP_PAF_BGR24, faces.get(0), feature);
            if (er != ErrorInfo.MOK.getValue()) {
                log.warn("[ArcSoft] 特征提取失败 {} code={}", faceImage.getName(), er);
                return null;
            }
            return feature.getFeatureData();
        } catch (Throwable t) {
            log.warn("[ArcSoft] extractFeature 异常: {}", t.getMessage());
            return null;
        }
    }

    @Override
    public synchronized double compareFeature(byte[] a, byte[] b) {
        if (!ready || a == null || b == null) return 0.0;
        try {
            FaceFeature fa = new FaceFeature(); fa.setFeatureData(a);
            FaceFeature fb = new FaceFeature(); fb.setFeatureData(b);
            FaceSimilar sim = new FaceSimilar();
            int code = faceEngine.compareFaceFeature(fa, fb, sim);
            if (code != ErrorInfo.MOK.getValue()) return 0.0;
            return sim.getScore() * 100.0; // 0~1 → 0~100
        } catch (Throwable t) {
            log.warn("[ArcSoft] compareFeature 异常: {}", t.getMessage());
            return 0.0;
        }
    }
}
