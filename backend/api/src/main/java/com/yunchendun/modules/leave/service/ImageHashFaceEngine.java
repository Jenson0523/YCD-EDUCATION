package com.yunchendun.modules.leave.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.nio.ByteBuffer;

/**
 * 过渡人脸引擎：基于感知哈希(dHash+aHash)的真实图像比对。
 *
 * 作用：在未接入虹软 ArcSoft 之前，用真实像素特征替换"假分数"——
 *   - 无关随机照片 → 低分，不再误判通过（解决"随便一张照片95%"问题）
 *   - 完全相同/高度相似的照片 → 高分
 * 局限：它比较的是整张图像结构，并非真正的人脸特征，
 *   同一个人不同角度/光照的实拍可能不稳，仅作过渡。生产请接入 ArcSoft。
 *
 * 当 ArcSoft 引擎(ArcSoftFaceEngine)就绪并被标记 @Primary 时，本引擎自动让位。
 */
@Slf4j
@Component
public class ImageHashFaceEngine implements FaceRecognitionEngine {

    private static final int W = 9, H = 8; // dHash 需要 9x8

    @Override
    public String name() { return "image-hash(过渡引擎)"; }

    @Override
    public boolean isReady() { return true; }

    /** 提取 128bit 特征：前64位 dHash，后64位 aHash，拼成 16 字节 */
    @Override
    public byte[] extractFeature(File faceImage) {
        try {
            BufferedImage img = ImageIO.read(faceImage);
            if (img == null) return null;
            long dh = dHash(img);
            long ah = aHash(img);
            return ByteBuffer.allocate(16).putLong(dh).putLong(ah).array();
        } catch (Exception e) {
            log.warn("图像特征提取失败 {}: {}", faceImage, e.getMessage());
            return null;
        }
    }

    @Override
    public double compareFeature(byte[] a, byte[] b) {
        if (a == null || b == null || a.length < 16 || b.length < 16) return 0.0;
        ByteBuffer ba = ByteBuffer.wrap(a), bb = ByteBuffer.wrap(b);
        long da = ba.getLong(), aa = ba.getLong();
        long db = bb.getLong(), ab = bb.getLong();
        int dDist = Long.bitCount(da ^ db); // 0~64
        int aDist = Long.bitCount(aa ^ ab); // 0~64
        // 两种哈希各占权重，转 0~100 相似度
        double dSim = (64 - dDist) / 64.0;
        double aSim = (64 - aDist) / 64.0;
        double sim = (dSim * 0.6 + aSim * 0.4) * 100.0;
        return Math.round(sim * 10.0) / 10.0;
    }

    /** 差异哈希：缩放 9x8 灰度，逐行比较相邻像素 */
    private long dHash(BufferedImage src) {
        BufferedImage g = toGray(src, W, H);
        Raster r = g.getRaster();
        long hash = 0; int bit = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W - 1; x++) {
                int left = r.getSample(x, y, 0);
                int right = r.getSample(x + 1, y, 0);
                if (left > right) hash |= (1L << bit);
                bit++;
            }
        }
        return hash;
    }

    /** 均值哈希：缩放 8x8 灰度，与均值比较 */
    private long aHash(BufferedImage src) {
        BufferedImage g = toGray(src, 8, 8);
        Raster r = g.getRaster();
        int sum = 0;
        int[] px = new int[64];
        int i = 0;
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) { px[i] = r.getSample(x, y, 0); sum += px[i]; i++; }
        int avg = sum / 64;
        long hash = 0;
        for (int k = 0; k < 64; k++) if (px[k] >= avg) hash |= (1L << k);
        return hash;
    }

    private BufferedImage toGray(BufferedImage src, int w, int h) {
        BufferedImage g = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gr = g.createGraphics();
        gr.drawImage(src, 0, 0, w, h, null);
        gr.dispose();
        return g;
    }
}
