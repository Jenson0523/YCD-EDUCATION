package com.yunchendun.workflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.workflow.domain.FlowInstance;
import com.yunchendun.workflow.mapper.FlowInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块: 统一审批流
 * 功能: 审批实例查询接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow/instances")
public class FlowInstanceController {

    private final FlowInstanceMapper flowInstanceMapper;

    @GetMapping
    public ApiResponse<IPage<FlowInstance>> page(@RequestParam(defaultValue = "1") long pageNo,
                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(flowInstanceMapper.selectPage(Page.of(pageNo, pageSize), null));
    }
}
