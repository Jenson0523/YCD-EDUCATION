<template>
  <view class="page">
    <view class="bg-layer"><view class="bg-grid"></view><view class="bg-glow"></view></view>
    <view :style="{ height: statusBarH + 'px' }"></view>

    <view class="header">
      <view class="hd-back" hover-class="hd-hover" @tap="goBack">‹</view>
      <text class="hd-title">修改密码</text>
      <view style="width:64rpx"></view>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">原密码</text>
        <input v-model="form.oldPassword" password placeholder="请输入原密码" class="field-input" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="field-label">新密码</text>
        <input v-model="form.newPassword" password placeholder="至少6位" class="field-input" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="field-label">确认新密码</text>
        <input v-model="form.confirm" password placeholder="再次输入新密码" class="field-input" placeholder-class="ph" />
      </view>

      <view class="tip">⚠️ 修改成功后需用新密码重新登录</view>

      <view class="submit-btn" :class="{ disabled: loading }" hover-class="btn-press" @tap="submit">
        <text class="submit-text">{{ loading ? '提交中…' : '确认修改' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { request } from '../../api/request';

const statusBarH = ref(20);
const loading = ref(false);
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' });

onMounted(() => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
});

const goBack = () => uni.navigateBack();

const submit = async () => {
  if (!form.oldPassword) { uni.showToast({ title: '请输入原密码', icon: 'none' }); return; }
  if (!form.newPassword || form.newPassword.length < 6) { uni.showToast({ title: '新密码至少6位', icon: 'none' }); return; }
  if (form.newPassword !== form.confirm) { uni.showToast({ title: '两次新密码不一致', icon: 'none' }); return; }
  if (form.newPassword === form.oldPassword) { uni.showToast({ title: '新密码不能与原密码相同', icon: 'none' }); return; }
  loading.value = true;
  try {
    await request({ url: '/auth/change-password', method: 'POST',
      data: { oldPassword: form.oldPassword, newPassword: form.newPassword } });
    uni.showToast({ title: '✓ 修改成功，请重新登录', icon: 'none', duration: 2000 });
    // 清登录态，跳登录
    ['ycd_token','ycd_userId','ycd_realName','ycd_roleCode','ycd_classNames','ycd_managedClasses','ycd_boundStudents']
      .forEach(k => uni.removeStorageSync(k));
    setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 1500);
  } catch (e) {
    uni.showToast({ title: e.message || '修改失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx); background-size: 48rpx 48rpx; mask-image: linear-gradient(180deg,#000,transparent 70%); }
.bg-glow { position: absolute; width: 500rpx; height: 500rpx; border-radius: 50%; filter: blur(90rpx); background: rgba(43,127,255,0.15); top: -100rpx; right: -140rpx; }
.header { position: relative; z-index: 2; display: flex; align-items: center; padding: 16rpx 32rpx 20rpx; }
.hd-back { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center; font-size: 52rpx; color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 12rpx; }
.hd-hover { background: rgba(0,229,255,0.12); }
.hd-title { flex: 1; text-align: center; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }

.form-card { position: relative; z-index: 1; margin: 32rpx 40rpx; background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 24rpx; padding: 40rpx 32rpx; }
.field { margin-bottom: 32rpx; }
.field-label { display: block; font-size: 26rpx; color: rgba(255,255,255,0.7); margin-bottom: 14rpx; }
.field-input { height: 88rpx; padding: 0 24rpx; background: rgba(255,255,255,0.07); border: 1rpx solid rgba(255,255,255,0.12); border-radius: 16rpx; font-size: 30rpx; color: #fff; }
.ph { color: rgba(255,255,255,0.3); }
.tip { font-size: 22rpx; color: #E8C068; margin: 8rpx 0 32rpx; }
.submit-btn { height: 96rpx; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1947C8, #2B7FFF); border-radius: 18rpx; box-shadow: 0 12rpx 30rpx rgba(25,71,200,0.4); }
.btn-press { opacity: 0.85; }
.submit-btn.disabled { opacity: 0.5; }
.submit-text { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 4rpx; }
</style>
