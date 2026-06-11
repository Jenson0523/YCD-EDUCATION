package com.yunchendun.modules.leave.service;

import java.io.File;

/**
 * 人脸识别引擎抽象。
 * 默认实现：图像相似度过渡引擎（无需任何凭证，已能拒绝无关照片）。
 * 生产实现：虹软 ArcSoft 离线SDK（src/arcsoft/java，需安装SDK后由 Maven profile 自动编译启用）。
 *
 * 约定：所有 score 均为 0~100 的"匹配置信度"，越高越像同一个人。
 */
public interface FaceRecognitionEngine {

    /** 引擎名称（用于日志/返回，便于前端区分"虹软"还是"过渡引擎"） */
    String name();

    /** 引擎是否就绪（ArcSoft 需激活成功；过渡引擎恒为 true） */
    boolean isReady();

    /**
     * 从一张人脸照片中提取特征。
     * @return 特征字节（ArcSoft 为人脸特征码；过渡引擎为感知哈希），失败返回 null
     */
    byte[] extractFeature(File faceImage);

    /**
     * 比对两个特征，返回 0~100 匹配置信度。
     */
    double compareFeature(byte[] featureA, byte[] featureB);

    /**
     * 便捷方法：直接比对两张图片（内部 extract + compare）。
     * @return 0~100 置信度；任一图片无法读取/检测不到人脸返回 0
     */
    default double compareImages(File a, File b) {
        byte[] fa = extractFeature(a);
        byte[] fb = extractFeature(b);
        if (fa == null || fb == null) return 0.0;
        return compareFeature(fa, fb);
    }
}
