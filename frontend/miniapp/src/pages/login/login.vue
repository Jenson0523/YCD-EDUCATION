<template>
  <view class="login-page">
    <!-- 科技感背景装饰 -->
    <view class="bg-decor">
      <view class="glow glow-1"></view>
      <view class="glow glow-2"></view>
      <view class="grid-lines"></view>
    </view>

    <!-- Logo 区 -->
    <view class="logo-area">
      <view class="logo-badge">
        <view class="logo-ring"></view>
        <text class="logo-icon">盾</text>
      </view>
      <text class="logo-text">云辰盾</text>
      <text class="logo-sub">YUNCHENDUN · 智慧家校共育平台</text>
    </view>

    <!-- 玻璃登录卡 -->
    <view class="form-card">
      <view class="card-title">欢迎登录</view>
      <view class="card-subtitle">信息化教育 · 安全离校管理</view>

      <view class="field">
        <view class="field-icon">👤</view>
        <input class="field-input" v-model="form.username" placeholder="请输入用户名" placeholder-class="ph" />
      </view>
      <view class="field">
        <view class="field-icon">🔒</view>
        <input class="field-input" v-model="form.password" password placeholder="请输入密码" placeholder-class="ph" />
      </view>

      <button class="login-btn" :class="{ loading }" :disabled="loading" @click="handleLogin">
        <text v-if="!loading">登 录</text>
        <text v-else>登录中…</text>
      </button>

      <view class="tip">家长 / 教师 / 门卫账号由学校管理员统一创建</view>
    </view>

    <view class="footer-brand">Powered by 云辰盾 · {{ year }}</view>
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
    const data = await request({ url: '/auth/login', method: 'POST', data: { ...form } });
    uni.setStorageSync('ycd_token', data.tokenValue);
    uni.setStorageSync('ycd_userId', data.userId);
    uni.setStorageSync('ycd_realName', data.realName);
    uni.setStorageSync('ycd_roleCode', data.roleCode);
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
  position: relative;
  min-height: 100vh;
  background: linear-gradient(165deg, #061a44 0%, #0b2a6b 45%, #103a8f 100%);
  overflow: hidden;
  padding: 0 56rpx;
}

/* 背景光晕与网格 */
.bg-decor { position: absolute; inset: 0; overflow: hidden; }
.glow { position: absolute; border-radius: 50%; filter: blur(60rpx); opacity: 0.55; }
.glow-1 { width: 420rpx; height: 420rpx; background: #15c8e0; top: -120rpx; right: -100rpx; }
.glow-2 { width: 500rpx; height: 500rpx; background: #2b6cff; bottom: -160rpx; left: -140rpx; opacity: 0.4; }
.grid-lines {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.04) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(255,255,255,0.04) 1rpx, transparent 1rpx);
  background-size: 60rpx 60rpx;
}

/* Logo */
.logo-area { position: relative; padding: 150rpx 0 70rpx; text-align: center; }
.logo-badge {
  position: relative; display: inline-flex; align-items: center; justify-content: center;
  width: 140rpx; height: 140rpx; margin-bottom: 28rpx;
}
.logo-ring {
  position: absolute; inset: 0; border-radius: 50%;
  background: linear-gradient(135deg, #2b6cff, #15c8e0);
  box-shadow: 0 0 50rpx rgba(21, 200, 224, 0.6);
}
.logo-icon { position: relative; font-size: 64rpx; font-weight: 800; color: #fff; }
.logo-text { display: block; font-size: 60rpx; font-weight: 800; color: #fff; letter-spacing: 12rpx; }
.logo-sub { display: block; margin-top: 16rpx; font-size: 22rpx; color: rgba(255,255,255,0.6); letter-spacing: 2rpx; }

/* 玻璃登录卡 */
.form-card {
  position: relative;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(30rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  border-radius: 32rpx;
  padding: 56rpx 44rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.3);
}
.card-title { font-size: 40rpx; font-weight: 700; color: #fff; }
.card-subtitle { margin-top: 8rpx; font-size: 22rpx; color: rgba(255,255,255,0.55); margin-bottom: 44rpx; }

.field {
  display: flex; align-items: center;
  height: 96rpx; margin-bottom: 28rpx; padding: 0 28rpx;
  background: rgba(255, 255, 255, 0.12);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: 18rpx;
}
.field-icon { font-size: 32rpx; margin-right: 18rpx; opacity: 0.85; }
.field-input { flex: 1; height: 100%; font-size: 30rpx; color: #fff; }
.ph { color: rgba(255,255,255,0.4); }

.login-btn {
  width: 100%; height: 96rpx; margin-top: 16rpx;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2b6cff 0%, #15c8e0 100%);
  color: #fff; font-size: 34rpx; font-weight: 700; letter-spacing: 8rpx;
  border-radius: 18rpx; border: none;
  box-shadow: 0 14rpx 36rpx rgba(21, 200, 224, 0.42);
}
.login-btn.loading { opacity: 0.7; }
.login-btn::after { border: none; }

.tip { text-align: center; margin-top: 36rpx; font-size: 22rpx; color: rgba(255,255,255,0.45); }

.footer-brand {
  position: relative; text-align: center; margin-top: 60rpx;
  font-size: 20rpx; color: rgba(255,255,255,0.35); letter-spacing: 1rpx;
}
</style>
