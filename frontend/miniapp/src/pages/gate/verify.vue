<template>
  <view class="page">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarH + 'px' }"></view>

    <!-- 顶部 Header -->
    <view class="header">
      <view class="header-left" @click="uni.navigateBack()">
        <text class="back-icon">‹</text>
      </view>
      <view class="header-center">
        <text class="header-title">人脸核验离校</text>
        <text class="header-sub">AI · 双重安全校验</text>
      </view>
      <view class="header-right" @click="navToday">
        <text class="today-btn">今日假条</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-body">

      <!-- 扫描仪区域 -->
      <view class="scanner-wrap">
        <view class="scanner-outer">
          <!-- 背景光晕 -->
          <view class="glow-ring" :class="{ 'glow-active': verifying, 'glow-ok': scanState==='ok', 'glow-fail': scanState==='fail' }"></view>

          <!-- 照片/占位 -->
          <view class="scanner-frame" :class="scanState">
            <image v-if="capturePhotoUrl" :src="capturePhotoUrl" class="scan-photo" mode="aspectFill" />
            <view v-else class="scan-placeholder">
              <view class="face-circle">
                <text class="face-icon">👤</text>
              </view>
              <text class="scan-hint">点击「拍照采集」进行人脸采集</text>
            </view>

            <!-- 四角金色括号 -->
            <view class="corner c-tl"></view>
            <view class="corner c-tr"></view>
            <view class="corner c-bl"></view>
            <view class="corner c-br"></view>

            <!-- 扫描线（核验中） -->
            <view v-if="verifying" class="scan-laser"></view>

            <!-- 结果遮罩 -->
            <view v-if="resultData && !verifying" class="result-overlay" :class="resultData.result">
              <view class="result-icon-wrap">
                <text class="result-icon">{{ resultIcon }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 状态文字 -->
        <text class="scan-status-text" :class="{ 'text-verifying': verifying }">
          {{ verifying ? '正在比对中，请稍候…' : (resultData ? resultData.message : '请先输入学籍号并拍照') }}
        </text>
      </view>

      <!-- 学生信息输入 -->
      <view class="section-card">
        <view class="card-label">
          <view class="label-dot"></view>
          <text class="label-text">学生信息</text>
        </view>
        <view class="input-row">
          <view class="input-wrap">
            <text class="input-icon">🔍</text>
            <input
              v-model="studentNo"
              placeholder="输入学籍号"
              class="stu-input"
              confirm-type="search"
              @confirm="searchStudent"
            />
          </view>
          <view class="search-btn" :class="{ loading: searching }" @click="searchStudent">
            {{ searching ? '…' : '查询' }}
          </view>
        </view>

        <!-- 学生信息展示 -->
        <view v-if="studentInfo" class="stu-info-card">
          <view class="stu-avatar-wrap">
            <image :src="studentInfo.facePhotoUrl || '/static/avatar-default.png'" class="stu-avatar" mode="aspectFill" />
            <view class="stu-avatar-badge">
              <text class="badge-dot"></text>
            </view>
          </view>
          <view class="stu-meta">
            <text class="stu-name">{{ studentInfo.realName || studentInfo.name }}</text>
            <text class="stu-class">{{ studentInfo.className || '—' }}</text>
            <view class="face-status" :class="studentInfo.hasFace ? 'face-ok' : 'face-none'">
              {{ studentInfo.hasFace ? '✓ 人脸档案已录入' : '⚠ 未录入人脸档案' }}
            </view>
          </view>
          <view class="stu-check">✓</view>
        </view>
        <view v-else-if="searched && !studentInfo" class="not-found">
          <text>未找到该学籍号对应学生档案</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-area">
        <view class="btn-capture" @click="capture">
          <text class="btn-cap-icon">{{ capturePhotoUrl ? '🔄' : '📷' }}</text>
          <text class="btn-cap-text">{{ capturePhotoUrl ? '重新拍照' : '拍照采集' }}</text>
        </view>
        <view
          class="btn-verify"
          :class="{ 'btn-disabled': !canVerify || verifying, 'btn-loading': verifying }"
          @click="doVerify"
        >
          <text v-if="!verifying" class="btn-verify-text">开始核验</text>
          <view v-else class="loading-dots">
            <text class="dot d1">●</text>
            <text class="dot d2">●</text>
            <text class="dot d3">●</text>
          </view>
        </view>
      </view>

      <!-- 结果详情卡 -->
      <view v-if="resultData" class="result-card" :class="'rc-' + resultData.result">
        <view class="rc-header">
          <view class="rc-icon-circle">
            <text class="rc-icon">{{ resultIcon }}</text>
          </view>
          <view class="rc-title-area">
            <text class="rc-title">{{ resultData.result === 'PASS' ? '放行通过' : resultData.result === 'NO_LEAVE' ? '无有效假条' : '人脸不匹配' }}</text>
            <text class="rc-time">{{ nowTime }}</text>
          </view>
        </view>
        <view class="rc-divider"></view>
        <view class="rc-body">
          <view class="rc-row">
            <text class="rc-key">人脸匹配度</text>
            <view class="score-bar-wrap">
              <view class="score-bar">
                <view class="score-fill" :style="{ width: resultData.faceScore + '%' }" :class="resultData.faceScore >= 80 ? 'fill-ok' : 'fill-fail'"></view>
              </view>
              <text class="score-val">{{ resultData.faceScore }} 分</text>
            </view>
          </view>
          <view v-if="resultData.leaveNo" class="rc-row">
            <text class="rc-key">假条编号</text>
            <text class="rc-val">{{ resultData.leaveNo }}</text>
          </view>
          <view v-if="resultData.studentName" class="rc-row">
            <text class="rc-key">学生姓名</text>
            <text class="rc-val">{{ resultData.studentName }}</text>
          </view>
          <view class="rc-row">
            <text class="rc-key">核验备注</text>
            <text class="rc-val">{{ resultData.message }}</text>
          </view>
        </view>
        <!-- 重新核验 -->
        <view class="rc-reset" @click="reset">重新核验</view>
      </view>

      <!-- 快捷功能 -->
      <view class="quick-section">
        <view class="quick-title">快捷功能</view>
        <view class="quick-grid">
          <view class="quick-item" @click="navToday">
            <view class="qi-icon-wrap qi-blue">
              <text class="qi-icon">📋</text>
            </view>
            <text class="qi-label">今日假条</text>
          </view>
          <view class="quick-item" @click="navTemp">
            <view class="qi-icon-wrap qi-amber">
              <text class="qi-icon">⚡</text>
            </view>
            <text class="qi-label">临时放行</text>
          </view>
          <view class="quick-item" @click="navHistory">
            <view class="qi-icon-wrap qi-green">
              <text class="qi-icon">📊</text>
            </view>
            <text class="qi-label">核验记录</text>
          </view>
        </view>
      </view>

      <view style="height:60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { request, assetUrl } from '../../api/request';

const statusBarH = ref(20);
onMounted(() => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
});

const studentNo = ref('');
const studentInfo = ref(null);
const capturePhotoUrl = ref('');
const verifying = ref(false);
const searching = ref(false);
const searched = ref(false);
const resultData = ref(null);

const nowTime = ref('');

const canVerify = computed(() => !!studentNo.value && !!capturePhotoUrl.value);

const scanState = computed(() => {
  if (verifying.value) return 'scanning';
  if (!resultData.value) return '';
  return resultData.value.result === 'PASS' ? 'ok' : 'fail';
});

const resultIcon = computed(() => {
  if (!resultData.value) return '';
  if (resultData.value.result === 'PASS') return '✓';
  if (resultData.value.result === 'NO_LEAVE') return '!';
  return '✕';
});

const searchStudent = async () => {
  if (!studentNo.value.trim()) return;
  searching.value = true;
  searched.value = false;
  studentInfo.value = null;
  resultData.value = null;
  try {
    // 先查学生档案
    const stuData = await request({ url: `/stu/students?keyword=${studentNo.value}&pageSize=5` });
    const stu = stuData?.records?.find(s => s.studentNo === studentNo.value) || stuData?.records?.[0];
    if (stu) {
      // 再查人脸档案
      let hasFace = false;
      let facePhotoUrl = '';
      try {
        const faceRec = await request({ url: `/leave/face/by-no/${studentNo.value}` });
        if (faceRec) { hasFace = true; facePhotoUrl = assetUrl(faceRec.facePhotoUrl); }
      } catch {}
      studentInfo.value = { ...stu, hasFace, facePhotoUrl };
    }
  } catch (e) {
    uni.showToast({ title: e.message || '查询失败', icon: 'none' });
  } finally {
    searching.value = false;
    searched.value = true;
  }
};

const capture = () => {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera'],
    success: (res) => {
      capturePhotoUrl.value = res.tempFilePaths[0];
      resultData.value = null;
    }
  });
};

const doVerify = async () => {
  if (!canVerify.value || verifying.value) return;
  verifying.value = true;
  resultData.value = null;
  try {
    // Step1: 人脸比对
    const compareRes = await request({
      url: '/leave/face/compare',
      method: 'POST',
      data: { studentNo: studentNo.value, capturePhotoUrl: capturePhotoUrl.value }
    });

    // Step2: 门卫核验（假条 + 人脸双重）
    const verifyRes = await request({
      url: '/leave/gate/verify',
      method: 'POST',
      data: {
        studentNo: studentNo.value,
        capturePhotoUrl: capturePhotoUrl.value,
        faceMatchScore: compareRes?.score || 0,
        verifyType: 'DEPART'
      }
    });

    nowTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    resultData.value = verifyRes;

    // 震动反馈
    if (verifyRes.result === 'PASS') {
      try { uni.vibrateShort(); } catch {}
    } else {
      try { uni.vibrateLong(); } catch {}
    }
  } catch (e) {
    uni.showToast({ title: e.message || '核验失败，请重试', icon: 'none' });
  } finally {
    verifying.value = false;
  }
};

const reset = () => {
  resultData.value = null;
  capturePhotoUrl.value = '';
  studentNo.value = '';
  studentInfo.value = null;
  searched.value = false;
};

const navToday = () => uni.navigateTo({ url: '/pages/gate/today-leaves' });
const navTemp = () => uni.navigateTo({ url: '/pages/gate/temp-depart' });
const navHistory = () => uni.navigateTo({ url: '/pages/gate/today-leaves' });
</script>

<style scoped>
/* ─── 全局 ─── */
.page { background: #060F2B; min-height: 100vh; }
.status-bar { background: #060F2B; }
.scroll-body { flex: 1; }

/* ─── Header ─── */
.header {
  display: flex; align-items: center;
  padding: 16rpx 32rpx 20rpx;
  background: #060F2B;
}
.header-left { width: 80rpx; }
.back-icon { font-size: 56rpx; color: rgba(255,255,255,0.7); line-height: 1; }
.header-center { flex: 1; text-align: center; }
.header-title { display: block; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.header-sub { display: block; font-size: 20rpx; color: rgba(200,210,255,0.5); margin-top: 4rpx; }
.today-btn { font-size: 24rpx; color: #E8C068; background: rgba(232,192,104,0.15); padding: 8rpx 20rpx; border-radius: 30rpx; }

/* ─── 扫描仪 ─── */
.scanner-wrap { padding: 32rpx 32rpx 0; display: flex; flex-direction: column; align-items: center; }
.scanner-outer { position: relative; width: 520rpx; height: 520rpx; display: flex; align-items: center; justify-content: center; }

/* 光晕 */
.glow-ring {
  position: absolute; inset: -20rpx; border-radius: 50%;
  background: radial-gradient(circle, rgba(43,127,255,0.08) 0%, transparent 70%);
  transition: all 0.4s;
}
.glow-ring.glow-active { background: radial-gradient(circle, rgba(43,127,255,0.25) 0%, transparent 70%); }
.glow-ring.glow-ok { background: radial-gradient(circle, rgba(12,166,120,0.3) 0%, transparent 70%); }
.glow-ring.glow-fail { background: radial-gradient(circle, rgba(239,68,68,0.3) 0%, transparent 70%); }

/* 扫描框 */
.scanner-frame {
  position: relative; width: 480rpx; height: 480rpx;
  border-radius: 32rpx; overflow: hidden;
  background: linear-gradient(145deg, #0D1F5C, #061634);
  border: 1rpx solid rgba(232,192,104,0.2);
  box-shadow: 0 0 60rpx rgba(0,0,0,0.5);
  transition: border-color 0.4s, box-shadow 0.4s;
}
.scanner-frame.scanning {
  border-color: rgba(43,127,255,0.6);
  box-shadow: 0 0 60rpx rgba(43,127,255,0.25);
}
.scanner-frame.ok {
  border-color: rgba(12,166,120,0.6);
  box-shadow: 0 0 60rpx rgba(12,166,120,0.3);
}
.scanner-frame.fail {
  border-color: rgba(239,68,68,0.5);
  box-shadow: 0 0 60rpx rgba(239,68,68,0.25);
}

.scan-photo { width: 100%; height: 100%; }
.scan-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.face-circle { width: 200rpx; height: 200rpx; border-radius: 50%; background: rgba(255,255,255,0.04); border: 2rpx solid rgba(232,192,104,0.25); display: flex; align-items: center; justify-content: center; }
.face-icon { font-size: 120rpx; opacity: 0.35; }
.scan-hint { margin-top: 32rpx; font-size: 22rpx; color: rgba(255,255,255,0.3); }

/* 四角金色括号 */
.corner {
  position: absolute; width: 44rpx; height: 44rpx;
  border-color: #E8C068;
  border-style: solid;
}
.c-tl { top: 20rpx; left: 20rpx; border-width: 5rpx 0 0 5rpx; border-radius: 8rpx 0 0 0; }
.c-tr { top: 20rpx; right: 20rpx; border-width: 5rpx 5rpx 0 0; border-radius: 0 8rpx 0 0; }
.c-bl { bottom: 20rpx; left: 20rpx; border-width: 0 0 5rpx 5rpx; border-radius: 0 0 0 8rpx; }
.c-br { bottom: 20rpx; right: 20rpx; border-width: 0 5rpx 5rpx 0; border-radius: 0 0 8rpx 0; }

/* 扫描线 */
.scan-laser {
  position: absolute; left: 20rpx; right: 20rpx; height: 3rpx;
  background: linear-gradient(90deg, transparent, #E8C068, rgba(43,127,255,0.8), #E8C068, transparent);
  box-shadow: 0 0 20rpx rgba(232,192,104,0.8);
  animation: laserScan 2s ease-in-out infinite;
}
@keyframes laserScan { 0% { top: 30rpx; opacity: 1; } 50% { top: 440rpx; opacity: 0.8; } 100% { top: 30rpx; opacity: 1; } }

/* 结果遮罩 */
.result-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
}
.result-overlay.PASS { background: rgba(12,166,120,0.45); }
.result-overlay.NO_LEAVE { background: rgba(245,158,11,0.45); }
.result-overlay.FACE_MISMATCH { background: rgba(239,68,68,0.4); }
.result-icon-wrap {
  width: 140rpx; height: 140rpx; border-radius: 50%;
  background: rgba(255,255,255,0.95);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 40rpx rgba(0,0,0,0.3);
}
.result-icon { font-size: 72rpx; font-weight: 900; color: #1a1a1a; }

/* 状态文字 */
.scan-status-text { margin-top: 24rpx; font-size: 24rpx; color: rgba(200,210,255,0.55); text-align: center; }
.text-verifying { color: #E8C068; animation: textPulse 1.5s ease-in-out infinite; }
@keyframes textPulse { 0%,100% { opacity: 0.6; } 50% { opacity: 1; } }

/* ─── 学生信息卡 ─── */
.section-card {
  margin: 28rpx 28rpx 0;
  background: rgba(255,255,255,0.05);
  border: 1rpx solid rgba(255,255,255,0.08);
  border-radius: 24rpx;
  padding: 28rpx;
  backdrop-filter: blur(20rpx);
}
.card-label { display: flex; align-items: center; gap: 12rpx; margin-bottom: 20rpx; }
.label-dot { width: 8rpx; height: 32rpx; background: linear-gradient(180deg, #E8C068, #2B7FFF); border-radius: 4rpx; }
.label-text { font-size: 28rpx; font-weight: 600; color: rgba(255,255,255,0.9); }

.input-row { display: flex; gap: 16rpx; align-items: center; }
.input-wrap { flex: 1; display: flex; align-items: center; background: rgba(255,255,255,0.07); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 16rpx; padding: 0 20rpx; height: 80rpx; }
.input-icon { font-size: 28rpx; margin-right: 12rpx; }
.stu-input { flex: 1; height: 80rpx; font-size: 28rpx; color: #fff; background: transparent; }
.search-btn {
  height: 80rpx; padding: 0 36rpx; line-height: 80rpx;
  background: linear-gradient(135deg, #E8C068, #C4973A);
  color: #1a1a1a; border-radius: 16rpx; font-size: 28rpx; font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(232,192,104,0.3);
}
.search-btn.loading { opacity: 0.6; }

/* 学生信息 */
.stu-info-card { display: flex; align-items: center; gap: 20rpx; margin-top: 24rpx; padding: 20rpx; background: rgba(255,255,255,0.06); border-radius: 16rpx; border: 1rpx solid rgba(232,192,104,0.2); }
.stu-avatar-wrap { position: relative; flex-shrink: 0; }
.stu-avatar { width: 88rpx; height: 88rpx; border-radius: 20rpx; background: #1a2a6c; }
.stu-avatar-badge { position: absolute; bottom: -4rpx; right: -4rpx; width: 24rpx; height: 24rpx; border-radius: 50%; background: #0CA678; border: 3rpx solid #060F2B; }
.stu-meta { flex: 1; }
.stu-name { display: block; font-size: 30rpx; font-weight: 700; color: #fff; }
.stu-class { display: block; font-size: 22rpx; color: rgba(200,210,255,0.55); margin-top: 4rpx; }
.face-status { display: inline-block; margin-top: 8rpx; font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 20rpx; }
.face-ok { background: rgba(12,166,120,0.2); color: #0CA678; }
.face-none { background: rgba(245,158,11,0.2); color: #F59F00; }
.stu-check { color: #E8C068; font-size: 36rpx; font-weight: 700; }
.not-found { margin-top: 20rpx; padding: 20rpx; background: rgba(239,68,68,0.1); border-radius: 12rpx; font-size: 26rpx; color: rgba(239,68,68,0.8); text-align: center; }

/* ─── 操作按钮 ─── */
.action-area { display: flex; gap: 20rpx; margin: 28rpx 28rpx 0; }
.btn-capture {
  flex: 1; height: 96rpx;
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
  background: rgba(255,255,255,0.07); border: 1rpx solid rgba(255,255,255,0.12);
  border-radius: 20rpx;
}
.btn-cap-icon { font-size: 36rpx; }
.btn-cap-text { font-size: 28rpx; color: rgba(255,255,255,0.85); font-weight: 500; }
.btn-verify {
  flex: 1.4; height: 96rpx;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1947C8 0%, #2B7FFF 50%, #00B8D9 100%);
  border-radius: 20rpx;
  box-shadow: 0 12rpx 32rpx rgba(43,127,255,0.4);
}
.btn-verify-text { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 4rpx; }
.btn-disabled { opacity: 0.4; box-shadow: none; }
.btn-loading { opacity: 0.8; }

/* 加载点 */
.loading-dots { display: flex; gap: 12rpx; align-items: center; }
.dot { font-size: 20rpx; color: rgba(255,255,255,0.7); }
.d1 { animation: dotFade 1.2s ease-in-out infinite 0s; }
.d2 { animation: dotFade 1.2s ease-in-out infinite 0.2s; }
.d3 { animation: dotFade 1.2s ease-in-out infinite 0.4s; }
@keyframes dotFade { 0%,100% { opacity: 0.3; } 50% { opacity: 1; } }

/* ─── 结果卡 ─── */
.result-card { margin: 28rpx 28rpx 0; border-radius: 24rpx; overflow: hidden; }
.rc-PASS { background: linear-gradient(135deg, #0A2A1E, #0D3D27); border: 1rpx solid rgba(12,166,120,0.35); }
.rc-NO_LEAVE { background: linear-gradient(135deg, #2A1F0A, #3D2D0D); border: 1rpx solid rgba(245,158,11,0.35); }
.rc-FACE_MISMATCH { background: linear-gradient(135deg, #2A0A0A, #3D1010); border: 1rpx solid rgba(239,68,68,0.35); }
.rc-header { display: flex; align-items: center; gap: 20rpx; padding: 28rpx; }
.rc-icon-circle { width: 72rpx; height: 72rpx; border-radius: 50%; background: rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.rc-icon { font-size: 36rpx; font-weight: 900; color: #fff; }
.rc-title-area { flex: 1; }
.rc-title { display: block; font-size: 30rpx; font-weight: 700; color: #fff; }
.rc-time { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 4rpx; }
.rc-divider { height: 1rpx; background: rgba(255,255,255,0.08); margin: 0 28rpx; }
.rc-body { padding: 20rpx 28rpx; }
.rc-row { display: flex; align-items: center; justify-content: space-between; padding: 12rpx 0; border-bottom: 1rpx solid rgba(255,255,255,0.05); }
.rc-row:last-child { border-bottom: none; }
.rc-key { font-size: 24rpx; color: rgba(255,255,255,0.45); }
.rc-val { font-size: 24rpx; color: rgba(255,255,255,0.85); font-weight: 500; flex: 1; text-align: right; }
.score-bar-wrap { display: flex; align-items: center; gap: 16rpx; flex: 1; justify-content: flex-end; }
.score-bar { width: 160rpx; height: 8rpx; background: rgba(255,255,255,0.1); border-radius: 4rpx; overflow: hidden; }
.score-fill { height: 100%; border-radius: 4rpx; transition: width 0.5s; }
.fill-ok { background: linear-gradient(90deg, #0CA678, #00D4FF); }
.fill-fail { background: linear-gradient(90deg, #FA5252, #FF6B6B); }
.score-val { font-size: 26rpx; color: #fff; font-weight: 600; min-width: 70rpx; text-align: right; }
.rc-reset { text-align: center; padding: 20rpx; font-size: 26rpx; color: rgba(255,255,255,0.45); border-top: 1rpx solid rgba(255,255,255,0.06); }

/* ─── 快捷功能 ─── */
.quick-section { margin: 28rpx 28rpx 0; }
.quick-title { font-size: 24rpx; color: rgba(200,210,255,0.4); margin-bottom: 16rpx; }
.quick-grid { display: flex; gap: 16rpx; }
.quick-item { flex: 1; background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.06); border-radius: 20rpx; padding: 24rpx 0; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.qi-icon-wrap { width: 72rpx; height: 72rpx; border-radius: 18rpx; display: flex; align-items: center; justify-content: center; }
.qi-blue { background: rgba(43,127,255,0.2); }
.qi-amber { background: rgba(245,158,11,0.2); }
.qi-green { background: rgba(12,166,120,0.2); }
.qi-icon { font-size: 38rpx; }
.qi-label { font-size: 22rpx; color: rgba(255,255,255,0.5); }
</style>
