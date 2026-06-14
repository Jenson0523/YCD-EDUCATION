package com.yunchendun.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块: 系统
 * 功能: 微信小程序订阅消息推送（"微信通知"）。
 *
 * ⚠️ 启用前提（本地无法测试，需发布版小程序）：
 *   1. 微信公众平台拿到小程序 AppID + AppSecret → 配 wx.appid / wx.secret
 *   2. 在「订阅消息」里申请模板，拿到模板ID → 配 wx.tmpl-*
 *   3. 小程序端在用户操作时 wx.requestSubscribeMessage 取得用户一次性订阅授权
 *   4. 用户的 openid 需绑定到账号（见 /api/auth/wx-bind，首次进入小程序 wx.login 获取）
 * 未配置时本服务静默跳过（不影响站内消息/首页弹窗等功能）。
 */
@Slf4j
@Service
public class WxNotifyService {

    @Value("${wx.appid:}")
    private String appid;
    @Value("${wx.secret:}")
    private String secret;
    /** 请假审批结果通知模板ID */
    @Value("${wx.tmpl-approve:}")
    private String tmplApprove;

    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile String cachedToken;
    private volatile long tokenExpire;

    public boolean isEnabled() {
        return appid != null && !appid.isBlank() && secret != null && !secret.isBlank();
    }

    /**
     * 发送订阅消息。openid 为空或未配置时静默跳过。
     * @param openid   接收用户 openid
     * @param templateId 模板ID
     * @param data     模板字段 { "thing1": {"value":"xxx"}, ... }
     * @param page     点击跳转的小程序页面路径（如 pages/leave/detail?id=1）
     */
    public void send(String openid, String templateId, Map<String, Object> data, String page) {
        if (!isEnabled() || openid == null || openid.isBlank() || templateId == null || templateId.isBlank()) {
            return; // 未配置/无openid，跳过（站内消息已生效）
        }
        try {
            String token = getAccessToken();
            if (token == null) return;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", openid);
            body.put("template_id", templateId);
            body.put("page", page);
            body.put("data", data);
            body.put("miniprogram_state", "trial"); // developer/trial/formal
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token;
            String resp = http.postForObject(url, body, String.class);
            log.info("[微信订阅消息] 发送结果: {}", resp);
        } catch (Exception e) {
            log.warn("[微信订阅消息] 发送异常: {}", e.getMessage());
        }
    }

    private String getAccessToken() {
        try {
            if (cachedToken != null && System.currentTimeMillis() < tokenExpire - 60_000) return cachedToken;
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + appid + "&secret=" + secret;
            String resp = http.getForObject(url, String.class);
            JsonNode node = mapper.readTree(resp);
            if (node.has("access_token")) {
                cachedToken = node.get("access_token").asText();
                tokenExpire = System.currentTimeMillis() + node.path("expires_in").asLong(7200) * 1000;
                return cachedToken;
            }
            log.warn("[微信] 获取access_token失败: {}", resp);
        } catch (Exception e) {
            log.warn("[微信] access_token异常: {}", e.getMessage());
        }
        return null;
    }
}
