<template>
  <view class="login-page">
    <view class="logo-area">
      <text class="logo-text">云辰盾</text>
      <text class="logo-sub">家校共育共同体</text>
    </view>

    <view class="form-area">
      <view class="field">
        <view class="field-label">用户名</view>
        <input class="field-input" v-model="form.username" placeholder="请输入用户名" />
      </view>
      <view class="field">
        <view class="field-label">密码</view>
        <input class="field-input" v-model="form.password" password placeholder="请输入密码" />
      </view>

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>
    </view>

    <view class="tip">家长/教师账号由学校管理员创建</view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { request } from '../../api/request';

const loading = ref(false);
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
    // 家长/学生跳首页，教师跳报备审核
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
  background: linear-gradient(160deg, #1b5ea6 0%, #0d3d73 60%, #f6f8fb 60%);
  padding: 0 48rpx;
}

.logo-area {
  padding: 120rpx 0 80rpx;
  text-align: center;
}

.logo-text {
  display: block;
  font-size: 64rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 8rpx;
}

.logo-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255,255,255,0.75);
}

.form-area {
  background: #fff;
  border-radius: 20rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.12);
}

.field { margin-bottom: 32rpx; }

.field-label {
  font-size: 26rpx;
  color: #6b7280;
  margin-bottom: 12rpx;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  background: #f9fafb;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.login-btn {
  width: 100%;
  height: 90rpx;
  background: #1b5ea6;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
  margin-top: 8rpx;
}

.login-btn[disabled] { opacity: 0.6; }

.tip {
  text-align: center;
  margin-top: 32rpx;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
