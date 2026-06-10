package com.yunchendun.modules.leave.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.leave.domain.FaceRecord;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块: 人脸识别请假离校
 * 功能: 学生人脸档案管理
 */
@Tag(name = "人脸档案管理")
@RestController
@RequestMapping("/api/leave/face")
@RequiredArgsConstructor
public class FaceRecordController {

    private final FaceRecordMapper faceRecordMapper;
    private final DataPermissionHelper dataPermissionHelper;

    /** 分页查询人脸档案（后台管理） */
    @Operation(summary = "人脸档案列表")
    @GetMapping
    public ApiResponse<Page<FaceRecord>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<FaceRecord>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(FaceRecord::getRealName, keyword)
                        .or().like(FaceRecord::getStudentNo, keyword))
                .eq(StringUtils.hasText(status), FaceRecord::getStatus, status)
                .orderByDesc(FaceRecord::getCreatedAt);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            if (dp.hasNoClass()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            wrapper.in(FaceRecord::getClassId, dp.getClassIds());
        } else if (dp.isSelf()) {
            if (dp.hasNoStudent()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            wrapper.in(FaceRecord::getStudentId, dp.getStudentIds());
        }
        // ALL / GATE_VALID：全部可见
        return ApiResponse.ok(faceRecordMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    /** 根据学生ID查询人脸档案 */
    @Operation(summary = "查询某学生人脸档案（by studentId）")
    @GetMapping("/by-student/{studentId}")
    public ApiResponse<FaceRecord> getByStudent(@PathVariable Long studentId) {
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>().eq(FaceRecord::getStudentId, studentId));
        return ApiResponse.ok(rec);
    }

    /** 根据学籍号查询人脸档案（门卫核验专用） */
    @Operation(summary = "查询某学生人脸档案（by studentNo）")
    @GetMapping("/by-no/{studentNo}")
    public ApiResponse<FaceRecord> getByStudentNo(@PathVariable String studentNo) {
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>().eq(FaceRecord::getStudentNo, studentNo));
        return ApiResponse.ok(rec);
    }

    /** 新建或更新人脸档案（家长/班主任上传照片） */
    @Operation(summary = "新建/更新人脸档案")
    @PostMapping
    public ApiResponse<FaceRecord> save(@RequestBody FaceRecord record) {
        // 检查是否已有档案
        FaceRecord exist = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>()
                        .eq(FaceRecord::getStudentNo, record.getStudentNo()));
        if (exist != null) {
            // 更新照片
            exist.setFacePhotoUrl(record.getFacePhotoUrl());
            exist.setStatus("ACTIVE");
            if (StringUtils.hasText(record.getRealName())) exist.setRealName(record.getRealName());
            faceRecordMapper.updateById(exist);
            return ApiResponse.ok(exist);
        }
        record.setStatus("ACTIVE");
        record.setTenantId(1L);
        faceRecordMapper.insert(record);
        return ApiResponse.ok(record);
    }

    /** 修改档案状态（启用/禁用） */
    @Operation(summary = "修改人脸档案状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        FaceRecord rec = faceRecordMapper.selectById(id);
        if (rec == null) return ApiResponse.fail(404, "档案不存在");
        rec.setStatus(body.get("status"));
        faceRecordMapper.updateById(rec);
        return ApiResponse.ok(null);
    }

    /** 人脸照片比对（本期：返回模拟分数，预留真实API） */
    @Operation(summary = "人脸比对（门卫核验用）")
    @PostMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(@RequestBody Map<String, String> body) {
        String studentNo = body.get("studentNo");
        String capturePhotoUrl = body.get("capturePhotoUrl");

        // 查询人脸档案
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>()
                        .eq(FaceRecord::getStudentNo, studentNo)
                        .eq(FaceRecord::getStatus, "ACTIVE"));
        if (rec == null || !StringUtils.hasText(rec.getFacePhotoUrl())) {
            return ApiResponse.ok(Map.of("score", 0.0, "passed", false, "message", "人脸档案不存在"));
        }

        /*
         * TODO: 接入真实人脸对比 API（腾讯云 / 百度云）
         * 腾讯云示例：
         *   FaceVerifyRequest req = new FaceVerifyRequest();
         *   req.setImageA(base64A); req.setImageB(base64B);
         *   double score = iai.FaceVerify(req).getScore();
         *
         * 本期 Mock：固定返回高分（档案存在即视为通过），后期替换为真实比对
         */
        double mockScore = 92.5 + (Math.random() * 5); // 92.5 ~ 97.5 模拟
        double threshold = 80.0;
        boolean passed = mockScore >= threshold;

        return ApiResponse.ok(Map.of(
                "score", Math.round(mockScore * 10.0) / 10.0,
                "passed", passed,
                "studentId", rec.getStudentId(),
                "realName", rec.getRealName(),
                "facePhotoUrl", rec.getFacePhotoUrl(),
                "message", passed ? "人脸比对通过" : "人脸比对不匹配"
        ));
    }
}
