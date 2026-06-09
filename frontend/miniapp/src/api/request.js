/**
 * 模块: 小程序
 * 功能: 请求封装
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

const BASE_URL = 'http://localhost:8080/api';

export function request({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        Authorization: uni.getStorageSync('ycd_token') || ''
      },
      success: (res) => {
        const body = res.data;
        if (body?.code === 0) {
          resolve(body.data);
        } else {
          reject(new Error(body?.message || '请求失败'));
        }
      },
      fail: reject
    });
  });
}
