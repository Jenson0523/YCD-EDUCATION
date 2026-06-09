package com.yunchendun.datacenter.api;

/**
 * 模块: 数据中台
 * 功能: 统一接口返回体
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }
}
