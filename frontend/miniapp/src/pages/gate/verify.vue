<template>
  <view class="page">
    <!-- 顶部 -->
    <view class="hero">
      <view class="hero-decor"></view>
      <view class="hero-content">
        <text class="hero-title">人脸核验离校</text>
        <text class="hero-sub">AI 人脸比对 · 假条双重校验</text>
      </view>
    </view>

    <view class="body">
      <!-- 扫描取景框 -->
      <view class="scanner" :class="scanState">
        <image v-if="capturePhotoUrl" :src="capturePhotoUrl" class="scan-img" mode="aspectFill" />
        <view v-else class="scan-empty">
          <text class="scan-face">👤</text>
        </view>
        <!-- 四角框 -->
        <view class="corner tl"></view>
        <view class="corner tr"></view>
        <view class="corner bl"></view>
        <view class="corner br"></view>
        <!-- 扫描线 -->
        <view v-if="verifying" class="scan-line"></view>
        <!-- 状态浮层 -->
        <view v-if="result" class="scan-result-mask" :class="resultClass">
          <text class="result-emoji">{{ resultIcon }}</text>
        </view>
      </view>

      <!-- 学籍号输入 -->
      <view class="input-card">
        <view class="input-row">
          <text class="input-label">学籍号</text>
          <input v-model="studentNo" placeholder="输入学籍号查询" class="input-field" />
          <view class="query-btn" @click="searchStudent">查询</view>
        </view>
        <view v-if="studentInfo" class="student-found">
          <image :src="studentInfo.facePhotoUrl || '/static/avatar.png'" class="sf-avatar" mode="aspectFill" />
          <view class="sf-info">
            <text class="sf-name">{{ studentInfo.realName }}</text>
            <text class="sf-class">{{ studentInfo.className || '—' }} · 档案正常</text>
          </view>
          <view class="sf-badge">✓</view>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-row">
        <button class="btn-capture" @click="capture">
          <text class="btn-icon">📷</text>{{ capturePhotoUrl ? '重拍' : '拍照采集' }}
        </button>
        <button class="btn-verify" :disabled="!canVerify || verifying" @click="doVerify">
          {{ verifying ? '核验中…' : '开始核验' }}
        </button>
      </view>

      <!-- 结果详情 -->
      <view v-if="result" class="result-card" :class="resultClass">
        <view class="rc-head">
          <text class="rc-icon">{{ resultIcon }}</text>
          <text class="rc-title">{{ result.message }}</text>
        </view>
        <view class="rc-detail">
          <view class="rc-item"><text class="rc-k">人脸匹配</text><text class="rc-v">{{ result.faceScore }} 分</text></view>
          <view v-if="result.leaveNo" class="rc-item"><text class="rc-k">假条编号</text><text class="rc-v">{{ result.leaveNo }}</text></view>
          <view v-if="result.studentName" class="rc-item"><text class="rc-k">学生</text><text class="rc-v">{{ result.studentName }}</text></view>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="quick-nav">
        <view class="qn-item" @click="nav('/pages/gate/today-leaves')">
          <text class="qn-icon">📋</text><text class="qn-label">今日假条</text>
        </view>
        <view class="qn-item" @click="nav('/pages/gate/temp-depart')">
          <text class="qn-icon">⚡</text><text class="qn-label">临时放行</text>
        </view>
        <view class="qn-item" @click="nav('/pages/gate/today-leaves')">
          <text class="qn-icon">⚠️</text><text class="qn-label">异常记录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { request } from '../../api/request';

const studentNo = ref('');
const capturePhotoUrl = ref('');
const studentInfo = ref(null);
const verifying = ref(false);
const result = ref(null);

const canVerify = computed(() => !!studentNo.value && !!capturePhotoUrl.value);

const scanState = computed(() => {
  if (verifying.value) return 'scanning';
  if (!result.value) return '';
  return result.value.result === 'PASS' ? 'ok' : 'fail';
});
const resultClass = computed(() => {
  if (!result.value) return '';
  if (result.value.result === 'PASS') return 'r-pass';
  if (result.value.result === 'NO_LEAVE') return 'r-warn';
  return 'r-fail';
});
const resultIcon = computed(() => {
  if (!result.value) return '';
  if (result.value.result === 'PASS') return '✓';
  if (result.value.result === 'NO_LEAVE') return '!';
  return '✕';
});

const searchStudent = async () => {
  if (!studentNo.value.trim()) return;
  result.value = null;
  try {
    const data = await request({ url: `/leave/face/by-student/${studentNo.value}` });
    studentInfo.value = data;
    if (!data) uni.showToast({ title: '未找到该学籍号档案', icon: 'none' });
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  }
};

const capture = () => {
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: (res) => { capturePhotoUrl.value = res.tempFilePaths[0]; result.value = null; }
  });
};

const doVerify = async () => {
  verifying.value = true;
  result.value = null;
  try {
    const compareRes = await request({
      url: '/leave/face/compare', method: 'POST',
      data: { studentNo: studentNo.value, capturePhotoUrl: capturePhotoUrl.value }
    });
    const verifyRes = await request({
      url: '/leave/gate/verify', method: 'POST',
      data: {
        studentNo: studentNo.value, capturePhotoUrl: capturePhotoUrl.value,
        faceMatchScore: compareRes.score, verifyType: 'DEPART'
      }
    });
    result.value = verifyRes;
    uni.vibrateShort && uni.vibrateShort();
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally {
    verifying.value = false;
  }
};

const nav = (url) => uni.navigateTo({ url });
</script>

<style scoped>
.page { min-height: 100vh; background: #eef2f9; }

.hero {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #0b2a6b 0%, #1746b5 60%, #2b6cff 100%);
  padding: 70rpx 40rpx 60rpx; border-radius: 0 0 36rpx 36rpx;
}
.hero-decor { position: absolute; width: 360rpx; height: 360rpx; border-radius: 50%;
  background: rgba(21,200,224,0.25); filter: blur(40rpx); top: -110rpx; right: -90rpx; }
.hero-content { position: relative; }
.hero-title { display: block; font-size: 40rpx; font-weight: 800; color: #fff; }
.hero-sub { display: block; margin-top: 10rpx; font-size: 22rpx; color: rgba(255,255,255,0.65); }

.body { padding: 32rpx; margin-top: -30rpx; }

/* 扫描取景框 */
.scanner {
  position: relative; width: 100%; height: 440rpx;
  background: radial-gradient(circle at center, #0e2a5e, #061634);
  border-radius: 28rpx; overflow: hidden;
  box-shadow: 0 16rpx 40rpx rgba(11, 42, 107, 0.25);
}
.scanner.scanning { box-shadow: 0 0 50rpx rgba(21, 200, 224, 0.5); }
.scanner.ok { box-shadow: 0 0 50rpx rgba(11, 191, 138, 0.5); }
.scanner.fail { box-shadow: 0 0 50rpx rgba(239, 68, 68, 0.5); }
.scan-img { width: 100%; height: 100%; }
.scan-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.scan-face { font-size: 160rpx; opacity: 0.25; }

.corner { position: absolute; width: 48rpx; height: 48rpx; border: 5rpx solid #15c8e0; }
.tl { top: 30rpx; left: 30rpx; border-right: none; border-bottom: none; border-radius: 10rpx 0 0 0; }
.tr { top: 30rpx; right: 30rpx; border-left: none; border-bottom: none; border-radius: 0 10rpx 0 0; }
.bl { bottom: 30rpx; left: 30rpx; border-right: none; border-top: none; border-radius: 0 0 0 10rpx; }
.br { bottom: 30rpx; right: 30rpx; border-left: none; border-top: none; border-radius: 0 0 10rpx 0; }

.scan-line {
  position: absolute; left: 30rpx; right: 30rpx; height: 4rpx;
  background: linear-gradient(90deg, transparent, #15c8e0, transparent);
  box-shadow: 0 0 16rpx #15c8e0;
  animation: scan 1.6s ease-in-out infinite;
}
@keyframes scan {
  0% { top: 40rpx; } 50% { top: 400rpx; } 100% { top: 40rpx; }
}

.scan-result-mask {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
}
.scan-result-mask.r-pass { background: rgba(11, 191, 138, 0.35); }
.scan-result-mask.r-warn { background: rgba(245, 158, 11, 0.35); }
.scan-result-mask.r-fail { background: rgba(239, 68, 68, 0.35); }
.result-emoji {
  width: 130rpx; height: 130rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.9); font-size: 70rpx; font-weight: 800; color: #0b2a6b;
}

/* 输入卡 */
.input-card { background: #fff; border-radius: 24rpx; padding: 28rpx; margin-top: 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(15,40,100,0.06); }
.input-row { display: flex; align-items: center; gap: 16rpx; }
.input-label { font-size: 26rpx; color: #64748b; flex-shrink: 0; }
.input-field { flex: 1; height: 76rpx; padding: 0 20rpx; background: #f1f5fb; border-radius: 14rpx; font-size: 28rpx; }
.query-btn { padding: 0 32rpx; height: 76rpx; line-height: 76rpx; background: linear-gradient(135deg,#2b6cff,#15c8e0);
  color: #fff; border-radius: 14rpx; font-size: 26rpx; }
.student-found { display: flex; align-items: center; gap: 18rpx; margin-top: 22rpx; padding-top: 22rpx; border-top: 1rpx solid #f1f5f9; }
.sf-avatar { width: 76rpx; height: 76rpx; border-radius: 18rpx; background: #e2e8f0; }
.sf-info { flex: 1; }
.sf-name { display: block; font-size: 30rpx; font-weight: 700; color: #111827; }
.sf-class { display: block; font-size: 22rpx; color: #0bbf8a; margin-top: 4rpx; }
.sf-badge { width: 48rpx; height: 48rpx; border-radius: 50%; background: #ecfdf5; color: #0bbf8a;
  display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 700; }

/* 操作按钮 */
.action-row { display: flex; gap: 20rpx; margin-top: 28rpx; }
.btn-capture { flex: 1; height: 92rpx; background: #fff; border: 1rpx solid #e2e8f0; border-radius: 18rpx;
  color: #2b6cff; font-size: 28rpx; font-weight: 600; display: flex; align-items: center; justify-content: center; }
.btn-icon { margin-right: 10rpx; }
.btn-verify { flex: 1.4; height: 92rpx; background: linear-gradient(135deg,#0bbf8a,#15c8e0); border: none;
  border-radius: 18rpx; color: #fff; font-size: 30rpx; font-weight: 700;
  box-shadow: 0 12rpx 30rpx rgba(11,191,138,0.35); }
.btn-verify[disabled] { opacity: 0.45; box-shadow: none; }
.btn-capture::after, .btn-verify::after { border: none; }

/* 结果卡 */
.result-card { margin-top: 28rpx; border-radius: 24rpx; padding: 28rpx; }
.result-card.r-pass { background: linear-gradient(135deg,#ecfdf5,#d1fae5); }
.result-card.r-warn { background: linear-gradient(135deg,#fffbeb,#fef3c7); }
.result-card.r-fail { background: linear-gradient(135deg,#fef2f2,#fee2e2); }
.rc-head { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.rc-icon { width: 56rpx; height: 56rpx; border-radius: 50%; background: rgba(255,255,255,0.8);
  display: flex; align-items: center; justify-content: center; font-size: 32rpx; font-weight: 800; }
.rc-title { font-size: 30rpx; font-weight: 700; color: #111827; flex: 1; }
.rc-detail { background: rgba(255,255,255,0.6); border-radius: 16rpx; padding: 20rpx; }
.rc-item { display: flex; justify-content: space-between; padding: 8rpx 0; }
.rc-k { font-size: 24rpx; color: #64748b; }
.rc-v { font-size: 24rpx; color: #111827; font-weight: 600; }

/* 快捷入口 */
.quick-nav { display: flex; gap: 18rpx; margin-top: 28rpx; }
.qn-item { flex: 1; background: #fff; border-radius: 20rpx; padding: 28rpx 16rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(15,40,100,0.05); }
.qn-icon { display: block; font-size: 44rpx; margin-bottom: 12rpx; }
.qn-label { font-size: 24rpx; color: #475569; }
</style>
