/**
 * 模块: PC 后台
 * 功能: Axios 请求封装
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ycd_token');
  if (token) {
    config.headers.Authorization = token;
  }
  return config;
});

http.interceptors.response.use((response) => {
  const body = response.data;
  if (body?.code !== 0) {
    return Promise.reject(new Error(body?.message || '请求失败'));
  }
  return body.data;
});
