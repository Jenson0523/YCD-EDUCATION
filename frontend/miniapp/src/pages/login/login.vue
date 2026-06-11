<template>
  <view class="login-page">
    <!-- 背景 -->
    <view class="bg-base"></view>
    <view class="bg-circle bg-c1"></view>
    <view class="bg-circle bg-c2"></view>

    <!-- 品牌区 -->
    <view class="brand-area">
      <view class="brand-badge">
        <view class="badge-inner">
          <text class="badge-char">盾</text>
        </view>
      </view>
      <text class="brand-zh">云辰盾</text>
      <text class="brand-en">YUNCHENDUN</text>
      <text class="brand-sub">智慧家校共育 · 安全离校管理</text>
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <text class="card-title">欢迎回来</text>
      <text class="card-sub">请使用学校分配的账号登录</text>

      <view class="input-group">
        <view class="input-box">
          <text class="input-icon">👤</text>
          <input
            class="input-field"
            v-model="form.username"
            placeholder="请输入用户名"
            placeholder-class="ph"
            :maxlength="30"
          />
        </view>
        <view class="input-box">
          <text class="input-icon">🔒</text>
          <input
            class="input-field"
            v-model="form.password"
            password
            placeholder="请输入密码"
            placeholder-class="ph"
            :maxlength="30"
          />
        </view>
      </view>

      <button class="submit-btn" :class="{ loading }" :disabled="loading" @click="handleLogin">
        <text v-if="!loading">登 录</text>
        <text v-else>验证中…</text>
      </button>

      <view class="tip-row">
        <text class="tip-text">家长 / 教师 / 门卫账号由学校管理员统一开通</text>
      </view>
    </view>

    <view class="footer-text">Powered by 云辰盾 · {{ year }}</view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { request } from '../../api/request';

const loading = ref(false);
const year = new Date().getFullYear();
const form = reactive({ username: '', password: '' });

const handleLogin = async () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    // 1. 登录
    const data = await request({ url: '/auth/login', method: 'POST', data: { ...form } });
    uni.setStorageSync('ycd_token', data.tokenValue);
    uni.setStorageSync('ycd_userId', data.userId);
    uni.setStorageSync('ycd_realName', data.realName);
    uni.setStorageSync('ycd_roleCode', data.roleCode);

    // 2. 加载角色专属信息（班级绑定/学生绑定）
    try {
      const info = await request({ url: '/auth/my-info' });
      if (info.classNames) {
        uni.setStorageSync('ycd_classNames', info.classNames);
        uni.setStorageSync('ycd_managedClasses', JSON.stringify(info.managedClasses || []));
      }
      if (info.boundStudents) {
        uni.setStorageSync('ycd_boundStudents', JSON.stringify(info.boundStudents));
      }
    } catch {
      // my-info 失败不影响登录
    }

    uni.reLaunch({ url: '/pages/index/index' });
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #F0F4FA 0%, #E8EDF5 50%, #F5F7FA 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 56rpx;
}
.bg-base { position: fixed; inset: 0; background: linear-gradient(180deg, #F0F4FA 0%, #E8EDF5 50%, #F5F7FA 100%); }
.bg-circle { position: fixed; border-radius: 50%; filter: blur(120rpx); opacity: 0.4; pointer-events: none; }
.bg-c1 { width: 500rpx; height: 500rpx; background: radial-gradient(circle, rgba(59,108,181,0.18), transparent); top: -200rpx; right: -150rpx; }
.bg-c2 { width: 400rpx; height: 400rpx; background: radial-gradient(circle, rgba(212,168,83,0.12), transparent); bottom: 20%; left: -180rpx; }

/* 品牌区 */
.brand-area {
  position: relative; z-index: 1; text-align: center;
  padding-top: 140rpx; padding-bottom: 52rpx;
}
.brand-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 130rpx; height: 130rpx; margin-bottom: 24rpx;
}
.badge-inner {
  width: 110rpx; height: 110rpx; border-radius: 50%;
  background: linear-gradient(135deg, #1E2F50, #2D4A7A);
  border: 3rpx solid rgba(212,168,83,0.6);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(30,47,80,0.2);
  animation: badge-breathe 3s ease-in-out infinite;
}
@keyframes badge-breathe {
  0%, 100% { transform: scale(1); box-shadow: 0 8rpx 32rpx rgba(30,47,80,0.2); }
  50% { transform: scale(1.04); box-shadow: 0 12rpx 40rpx rgba(30,47,80,0.3); }
}
.badge-char { font-size: 52rpx; font-weight: 800; color: #E8C068; }
.brand-zh { display: block; font-size: 52rpx; font-weight: 700; color: #1E2F50; letter-spacing: 10rpx; }
.brand-en { display: block; margin-top: 8rpx; font-size: 22rpx; color: #3B6CB5;
  letter-spacing: 6rpx; font-family: 'Courier New', monospace; }
.brand-sub { display: block; margin-top: 12rpx; font-size: 22rpx; color: #94A3B8; letter-spacing: 2rpx; }

/* 登录卡片 */
.login-card {
  position: relative; z-index: 1; width: 100%; box-sizing: border-box;
  background: #fff;
  border-radius: 28rpx;
  padding: 44rpx 36rpx;
  box-shadow: 0 8rpx 36rpx rgba(30,47,80,0.08);
  overflow: hidden;
}
.card-title { display: block; font-size: 36rpx; font-weight: 700; color: #1E2F50; text-align: center; }
.card-sub { display: block; margin-top: 10rpx; margin-bottom: 40rpx; font-size: 24rpx; color: #94A3B8; text-align: center; }

.input-group { display: flex; flex-direction: column; gap: 20rpx; }
.input-box {
  display: flex; align-items: center; gap: 16rpx;
  height: 96rpx; padding: 0 24rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
}
.input-icon { font-size: 30rpx; opacity: 0.6; flex-shrink: 0; }
.input-field { flex: 1; height: 100%; font-size: 28rpx; color: #1E293B; }
.ph { color: #CBD5E1; }

.submit-btn {
  width: 100%; height: 96rpx; margin-top: 32rpx;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1E2F50 0%, #3B6CB5 100%);
  color: #fff; font-size: 32rpx; font-weight: 700; letter-spacing: 8rpx;
  border-radius: 16rpx; border: none;
  box-shadow: 0 8rpx 28rpx rgba(30,47,80,0.25);
}
.submit-btn.loading { opacity: 0.65; }
.submit-btn::after { border: none; }

.tip-row { margin-top: 28rpx; text-align: center; }
.tip-text { font-size: 22rpx; color: #CBD5E1; letter-spacing: 1rpx; }

.footer-text {
  position: relative; z-index: 1; margin-top: auto; padding: 44rpx 0 36rpx;
  font-size: 20rpx; color: #CBD5E1; letter-spacing: 2rpx;
}
</style>
