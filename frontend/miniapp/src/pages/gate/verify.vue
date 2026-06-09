<template>
  <view class="page">
    <!-- 顶部状态卡 -->
    <view class="hero">
      <text class="hero-title">门卫人脸核验</text>
      <text class="hero-sub">手机端 · 双重校验（人脸 + 假条）</text>
    </view>

    <!-- 核验步骤 -->
    <view class="section">
      <!-- Step1: 输入学籍号 -->
      <view class="step-card">
        <view class="step-header"><text class="step-no">01</text><text class="step-title">输入学籍号</text></view>
        <view class="field-row">
          <input v-model="studentNo" placeholder="请输入学籍号" class="field-input" />
          <button class="search-btn" @click="searchStudent">查询</button>
        </view>
        <view v-if="studentInfo" class="student-info">
          <image :src="studentInfo.facePhotoUrl || '/static/avatar.png'" class="student-avatar" mode="aspectFill" />
          <view class="student-detail">
            <text class="sname">{{ studentInfo.realName }}</text>
            <text class="sno">{{ studentInfo.className }}</text>
          </view>
        </view>
      </view>

      <!-- Step2: 拍照采集 -->
      <view class="step-card">
        <view class="step-header"><text class="step-no">02</text><text class="step-title">拍照采集人脸</text></view>
        <view class="capture-area" @click="capture">
          <image v-if="capturePhotoUrl" :src="capturePhotoUrl" class="capture-img" mode="aspectFill" />
          <view v-else class="capture-placeholder">
            <text class="camera-icon">📷</text>
            <text class="capture-tip">点击拍照</text>
          </view>
        </view>
      </view>

      <!-- Step3: 执行核验 -->
      <button class="verify-btn" :disabled="!studentNo || !capturePhotoUrl || verifying" @click="doVerify">
        {{ verifying ? '核验中…' : '🔍 执行核验' }}
      </button>
    </view>

    <!-- 核验结果 -->
    <view v-if="result" :class="['result-card', resultClass]">
      <text class="result-icon">{{ resultIcon }}</text>
      <text class="result-msg">{{ result.message }}</text>
      <view class="result-detail">
        <text class="detail-row">人脸匹配分：{{ result.faceScore }}分</text>
        <text v-if="result.leaveNo" class="detail-row">假条号：{{ result.leaveNo }}</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-nav section">
      <view class="nav-item" @click="nav('/pages/gate/today-leaves')">
        <text class="nav-icon">📋</text>
        <text class="nav-label">今日假条</text>
      </view>
      <view class="nav-item" @click="nav('/pages/gate/temp-depart')">
        <text class="nav-icon">⚡</text>
        <text class="nav-label">临时放行</text>
      </view>
      <view class="nav-item" @click="nav('/pages/gate/abnormal')">
        <text class="nav-icon">⚠️</text>
        <text class="nav-label">异常记录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { request } from '../../api/request';

const studentNo = ref('');
const capturePhotoUrl = ref('');
const studentInfo = ref(null);
const verifying = ref(false);
const result = ref(null);

const searchStudent = async () => {
  if (!studentNo.value.trim()) return;
  try {
    const data = await request({ url: `/leave/face/by-student/${studentNo.value}` });
    studentInfo.value = data;
    if (!data) uni.showToast({ title: '未找到该学籍号的人脸档案', icon: 'none' });
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  }
};

const capture = () => {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera'],
    success: (res) => { capturePhotoUrl.value = res.tempFilePaths[0]; }
  });
};

const doVerify = async () => {
  verifying.value = true;
  result.value = null;
  try {
    // Step1: 调用人脸比对接口
    const compareRes = await request({
      url: '/leave/face/compare',
      method: 'POST',
      data: { studentNo: studentNo.value, capturePhotoUrl: capturePhotoUrl.value }
    });

    // Step2: 调用门卫核验接口（含假条校验）
    const verifyRes = await request({
      url: '/leave/gate/verify',
      method: 'POST',
      data: {
        studentNo: studentNo.value,
        capturePhotoUrl: capturePhotoUrl.value,
        faceMatchScore: compareRes.score,
        verifyType: 'DEPART'
      }
    });
    result.value = verifyRes;
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally {
    verifying.value = false;
  }
};

const resultClass = result.value?.result === 'PASS' ? 'result-pass'
    : result.value?.result === 'NO_LEAVE' ? 'result-warn' : 'result-fail';

const resultIcon = result.value?.result === 'PASS' ? '✅'
    : result.value?.result === 'NO_LEAVE' ? '⚠️' : '❌';

const nav = (url) => uni.navigateTo({ url });
</script>

<style scoped>
.hero { background: #1b5ea6; padding: 40rpx 32rpx 28rpx; }
.hero-title { display: block; font-size: 40rpx; font-weight: 700; color: #fff; }
.hero-sub { display: block; font-size: 22rpx; color: rgba(255,255,255,0.7); margin-top: 8rpx; }
.section { padding: 24rpx 28rpx; }
.step-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06); }
.step-header { display: flex; align-items: center; margin-bottom: 16rpx; }
.step-no { font-size: 32rpx; font-weight: 900; color: #1b5ea6; margin-right: 12rpx; }
.step-title { font-size: 28rpx; font-weight: 600; color: #374151; }
.field-row { display: flex; gap: 12rpx; }
.field-input { flex: 1; height: 80rpx; padding: 0 20rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 10rpx; font-size: 28rpx; }
.search-btn { height: 80rpx; padding: 0 28rpx; background: #1b5ea6; color: #fff; border: none; border-radius: 10rpx; font-size: 26rpx; }
.student-info { display: flex; align-items: center; gap: 20rpx; margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx solid #f3f4f6; }
.student-avatar { width: 80rpx; height: 80rpx; border-radius: 50%; background: #e5e7eb; }
.sname { display: block; font-size: 30rpx; font-weight: 700; color: #111827; }
.sno { display: block; font-size: 22rpx; color: #9ca3af; }
.capture-area { height: 240rpx; background: #f9fafb; border: 2rpx dashed #d1d5db; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.capture-img { width: 100%; height: 100%; }
.capture-placeholder { display: flex; flex-direction: column; align-items: center; }
.camera-icon { font-size: 56rpx; }
.capture-tip { font-size: 24rpx; color: #9ca3af; margin-top: 12rpx; }
.verify-btn { width: calc(100% - 56rpx); margin: 0 28rpx; height: 92rpx; background: #059669; color: #fff; font-size: 32rpx; font-weight: 600; border-radius: 12rpx; border: none; }
.verify-btn[disabled] { opacity: 0.5; }
.result-card { margin: 20rpx 28rpx; padding: 28rpx; border-radius: 16rpx; }
.result-pass { background: #d1fae5; }
.result-warn { background: #fef3c7; }
.result-fail { background: #fee2e2; }
.result-icon { display: block; font-size: 48rpx; text-align: center; margin-bottom: 12rpx; }
.result-msg { display: block; font-size: 30rpx; font-weight: 700; text-align: center; color: #111827; margin-bottom: 16rpx; }
.result-detail { text-align: center; }
.detail-row { display: block; font-size: 24rpx; color: #6b7280; margin-bottom: 6rpx; }
.quick-nav { display: flex; gap: 16rpx; margin-top: 8rpx; }
.nav-item { flex: 1; background: #fff; border-radius: 14rpx; padding: 24rpx 16rpx; text-align: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05); }
.nav-icon { display: block; font-size: 40rpx; margin-bottom: 10rpx; }
.nav-label { font-size: 24rpx; color: #374151; }
</style>
