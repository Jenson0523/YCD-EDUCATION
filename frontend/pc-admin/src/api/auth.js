/**
 * 模块: PC 后台
 * 功能: 登录相关 API
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { http } from './http';

export const login = (username, password) =>
  http.post('/auth/login', { username, password });

export const logout = () => http.post('/auth/logout');

export const fetchMe = () => http.get('/auth/me');

export const fetchUnreadCount = () => http.get('/sys/messages/unread-count');

export const fetchMessages = (params) => http.get('/sys/messages', { params });

export const markMessageRead = (id) => http.put(`/sys/messages/${id}/read`);

export const markAllRead = () => http.put('/sys/messages/read-all');
