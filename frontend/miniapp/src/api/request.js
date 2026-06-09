/**
 * 模块: 小程序
 * 功能: 请求封装，自动注入 token，401 跳登录页
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

// 真机调试/扫码预览时必须用电脑局域网IP（手机访问不到 localhost）
// 纯模拟器调试可改回 http://localhost:8080/api
const BASE_URL = 'http://192.168.0.40:8080/api';

export function request({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('ycd_token') || '';
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: { Authorization: token, 'Content-Type': 'application/json' },
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('ycd_token');
          uni.reLaunch({ url: '/pages/login/login' });
          reject(new Error('登录已过期'));
          return;
        }
        const body = res.data;
        if (body?.code === 0) {
          resolve(body.data);
        } else {
          reject(new Error(body?.message || '请求失败'));
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '网络异常'))
    });
  });
}
