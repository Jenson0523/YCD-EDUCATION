package com.yunchendun.common.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块: 平台级 / common
 * 功能: 登录拦截器，所有 /api/** 接口必须携带有效 token（除白名单外）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            StpUtil.checkLogin();
            return true;
        } catch (NotLoginException e) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            // 注意：Map.of 不允许 null 值，需用 HashMap 承载 data=null
            Map<String, Object> body = new HashMap<>();
            body.put("code", 401);
            body.put("message", "未登录或登录已过期，请重新登录");
            body.put("data", null);
            response.getWriter().write(MAPPER.writeValueAsString(body));
            return false;
        }
    }
}
