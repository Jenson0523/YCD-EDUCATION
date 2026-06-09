/**
 * 模块: PC 后台
 * 功能: 登录状态 Pinia Store
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { login as loginApi, logout as logoutApi, fetchMe } from '../api/auth';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('ycd_token') || '');
  const userId = ref(Number(localStorage.getItem('ycd_userId') || 0));
  const realName = ref(localStorage.getItem('ycd_realName') || '');
  const roleCode = ref(localStorage.getItem('ycd_roleCode') || '');
  const unreadCount = ref(0);

  const isLoggedIn = () => !!token.value;

  async function login(username, password) {
    const data = await loginApi(username, password);
    token.value = data.tokenValue;
    userId.value = data.userId;
    realName.value = data.realName;
    roleCode.value = data.roleCode;
    localStorage.setItem('ycd_token', data.tokenValue);
    localStorage.setItem('ycd_userId', data.userId);
    localStorage.setItem('ycd_realName', data.realName);
    localStorage.setItem('ycd_roleCode', data.roleCode);
  }

  async function logout() {
    try { await logoutApi(); } catch {}
    token.value = '';
    userId.value = 0;
    realName.value = '';
    roleCode.value = '';
    localStorage.removeItem('ycd_token');
    localStorage.removeItem('ycd_userId');
    localStorage.removeItem('ycd_realName');
    localStorage.removeItem('ycd_roleCode');
  }

  async function loadMe() {
    if (!token.value) return;
    try {
      const data = await fetchMe();
      realName.value = data.realName;
      roleCode.value = data.roleCode;
    } catch {
      token.value = '';
    }
  }

  return { token, userId, realName, roleCode, unreadCount, isLoggedIn, login, logout, loadMe };
});
