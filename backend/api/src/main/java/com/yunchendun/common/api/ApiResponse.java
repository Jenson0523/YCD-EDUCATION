package com.yunchendun.common.api;

/**
 * 模块: 平台级 / common
 * 功能: 统一接口返回体
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
