/**
 * 模块: PC 后台
 * 功能: Axios 请求封装，自动注入 token，401 跳登录页
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import axios from 'axios';
import { ElMessage } from 'element-plus';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ycd_token');
  if (token) config.headers.Authorization = token;
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body?.code !== 0) {
      ElMessage.error(body?.message || '请求失败');
      return Promise.reject(new Error(body?.message || '请求失败'));
    }
    return body.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ycd_token');
      window.location.href = '/login';
      return Promise.reject(error);
    }
    const msg = error.response?.data?.message || error.message || '网络异常';
    ElMessage.error(msg);
    return Promise.reject(error);
  }
);
