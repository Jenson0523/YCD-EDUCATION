<template>
  <view class="page">
    <view class="bg-layer"><view class="bg-grid"></view><view class="bg-glow"></view></view>
    <view :style="{ height: statusBarH + 'px' }"></view>

    <!-- Header -->
    <view class="header">
      <view class="hd-back" hover-class="hd-hover" @tap="goBack">‹</view>
      <view class="hd-center">
        <text class="hd-title">刷脸核验离校</text>
        <text class="hd-sub">AI 自动识别 · 有假条即放行</text>
      </view>
      <view class="hd-action" hover-class="hd-hover" @tap="navToday">今日假条</view>
    </view>

    <scroll-view scroll-y class="scroll">
      <!-- 扫描区 -->
      <view class="scan-section">
        <view class="scan-frame" :class="scanState">
          <image v-if="capturePhoto" :src="capturePhoto" class="scan-img" mode="aspectFill" />
          <view v-else class="scan-empty">
            <view class="scan-face-ring"><text class="scan-face">👤</text></view>
          </view>
          <view class="corner c-tl"></view><view class="corner c-tr"></view>
          <view class="corner c-bl"></view><view class="corner c-br"></view>
          <view v-if="scanning" class="laser"></view>
          <view v-if="result && !scanning" class="res-mask" :class="'rm-' + result.result">
            <text class="res-icon">{{ resultIcon }}</text>
          </view>
        </view>
        <text class="scan-tip" :class="{ active: scanning }">{{ scanTip }}</text>
      </view>

      <!-- 刷脸大按钮 -->
      <view class="scan-btn" :class="{ disabled: scanning }" hover-class="scan-press" @tap="startScan">
        <text class="scan-btn-icon">📷</text>
        <text class="scan-btn-text">{{ scanning ? '识别中…' : '点击刷脸核验' }}</text>
      </view>

      <!-- 结果：自动放行 -->
      <view v-if="result && result.result === 'PASS'" class="res-card rc-pass">
        <view class="rc-top">
          <text class="rc-badge rc-badge-pass">✓ 已自动放行</text>
          <text class="rc-time">{{ resultTime }}</text>
        </view>
        <view class="rc-stu">{{ result.studentName }} <text class="rc-cls">{{ result.className || '' }}</text></view>
        <view class="rc-rows">
          <view class="rc-row"><text class="rc-k">假条</text><text class="rc-v mono">{{ result.leaveNo }}</text></view>
          <view class="rc-row"><text class="rc-k">类型</text><text class="rc-v">{{ leaveTypeText(result.leaveType) }}</text></view>
          <view class="rc-row"><text class="rc-k">匹配度</text><text class="rc-v mono">{{ result.faceScore }} 分</text></view>
        </view>
      </view>

      <!-- 结果：无有效假条 → 可发起临时审批 -->
      <view v-if="result && result.result === 'NO_LEAVE'" class="res-card rc-warn">
        <view class="rc-top"><text class="rc-badge rc-badge-warn">⛔ 无有效假条</text></view>
        <view class="rc-stu">{{ result.studentName }} <text class="rc-cls">{{ result.className || '' }}</text></view>
        <text class="rc-hint">该生今日无已审批假条，正常不予放行。如确需离校，可发起临时审批（放行并通知班主任24h内补批）。</text>
        <textarea v-model="tempReason" class="temp-input" placeholder="请填写临时离校原因（必填）" :maxlength="100" />
        <view class="temp-btn" :class="{ disabled: tempLoading }" hover-class="scan-press" @tap="doTempApproval">
          <text class="temp-btn-text">{{ tempLoading ? '提交中…' : '⚡ 发起临时审批并放行' }}</text>
        </view>
      </view>

      <!-- 结果：未识别 -->
      <view v-if="result && result.result === 'NO_MATCH'" class="res-card rc-none">
        <text class="rc-none-icon">🔍</text>
        <text class="rc-none-text">{{ result.message }}</text>
      </view>

      <!-- 继续 -->
      <view v-if="result" class="next-btn" hover-class="scan-press" @tap="reset">继续核验下一位</view>

      <view style="height: 60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { request, uploadFile } from '../../api/request';

const statusBarH = ref(20);
const capturePhoto = ref('');
const scanning = ref(false);
const result = ref(null);
const resultTime = ref('');
const tempReason = ref('');
const tempLoading = ref(false);
let lastStudentNo = '';

const scanState = computed(() => {
  if (scanning.value) return 'scanning';
  if (!result.value) return '';
  return result.value.result === 'PASS' ? 'ok' : (result.value.result === 'NO_MATCH' ? '' : 'fail');
});
const scanTip = computed(() => {
  if (scanning.value) return '正在识别人脸库，请稍候…';
  if (result.value) return result.value.message || '';
  return '请让学生正对镜头，点击下方按钮';
});
const resultIcon = computed(() => {
  if (!result.value) return '';
  return result.value.result === 'PASS' ? '✓' : (result.value.result === 'NO_LEAVE' ? '!' : '');
});
const leaveTypeText = (t) => t === 'SICK' ? '病假' : (t === 'PERSONAL' ? '事假' : (t || '—'));

onMounted(() => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
});

const startScan = () => {
  if (scanning.value) return;
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: async (res) => {
      capturePhoto.value = res.tempFilePaths[0];
      result.value = null;
      tempReason.value = '';
      scanning.value = true;
      try {
        let url = '';
        try { const up = await uploadFile(capturePhoto.value, 'capture'); url = up.url; } catch {}
        const r = await request({ url: '/leave/gate/scan', method: 'POST', data: { capturePhotoUrl: url } });
        await new Promise(rs => setTimeout(rs, 800)); // 扫描动效
        const d = new Date();
        resultTime.value = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`;
        result.value = r;
        lastStudentNo = r.studentNo || '';
        try { r.result === 'PASS' ? uni.vibrateShort() : uni.vibrateLong(); } catch {}
      } catch (e) {
        uni.showToast({ title: e.message || '核验失败', icon: 'none' });
      } finally {
        scanning.value = false;
      }
    }
  });
};

// 无假条 → 发起临时审批（临时放行 + 班主任补批）
const doTempApproval = async () => {
  if (tempLoading.value) return;
  if (!tempReason.value.trim()) { uni.showToast({ title: '请填写临时离校原因', icon: 'none' }); return; }
  tempLoading.value = true;
  try {
    await request({ url: '/leave/applications/temp-depart', method: 'POST',
      data: { studentNo: lastStudentNo, studentName: result.value.studentName, reason: tempReason.value } });
    const d = new Date();
    resultTime.value = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
    result.value = { result: 'PASS', studentName: result.value.studentName, className: result.value.className,
      leaveNo: '临时放行', leaveType: 'PERSONAL', faceScore: result.value.faceScore || '—' };
    uni.showToast({ title: '✓ 已临时放行，已通知班主任补批', icon: 'none', duration: 2500 });
    try { uni.vibrateShort(); } catch {}
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' });
  } finally {
    tempLoading.value = false;
  }
};

const reset = () => { capturePhoto.value = ''; result.value = null; tempReason.value = ''; lastStudentNo = ''; };
const goBack = () => uni.navigateBack();
const navToday = () => uni.navigateTo({ url: '/pages/gate/today-leaves' });
</script>

<style scoped>
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx); background-size: 48rpx 48rpx; mask-image: linear-gradient(180deg,#000,transparent 70%); }
.bg-glow { position: absolute; width: 600rpx; height: 600rpx; border-radius: 50%; filter: blur(90rpx); background: rgba(0,229,255,0.15); top: -120rpx; right: -160rpx; }
.scroll { position: relative; z-index: 1; height: calc(100vh - 100rpx); }
.header { position: relative; z-index: 2; display: flex; align-items: center; padding: 16rpx 32rpx 20rpx; }
.hd-back { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center; font-size: 52rpx; color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 12rpx; }
.hd-hover { background: rgba(0,229,255,0.12); }
.hd-center { flex: 1; text-align: center; }
.hd-title { display: block; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.hd-sub { display: block; font-size: 20rpx; color: rgba(0,229,255,0.6); margin-top: 4rpx; }
.hd-action { padding: 0 24rpx; height: 64rpx; line-height: 64rpx; font-size: 24rpx; color: #00E5FF; background: rgba(0,229,255,0.08); border: 1rpx solid rgba(0,229,255,0.3); border-radius: 12rpx; }

.scan-section { display: flex; flex-direction: column; align-items: center; padding: 32rpx 40rpx 0; }
.scan-frame { position: relative; width: 480rpx; height: 480rpx; border-radius: 24rpx; overflow: hidden; background: radial-gradient(circle at center, #0A1A40, #04081C); border: 1rpx solid rgba(0,229,255,0.2); transition: all 0.3s; }
.scan-frame.scanning { border-color: rgba(0,229,255,0.6); box-shadow: 0 0 50rpx rgba(0,229,255,0.3); }
.scan-frame.ok { border-color: rgba(0,230,150,0.6); box-shadow: 0 0 50rpx rgba(0,230,150,0.3); }
.scan-frame.fail { border-color: rgba(255,140,40,0.6); box-shadow: 0 0 50rpx rgba(255,140,40,0.25); }
.scan-img { width: 100%; height: 100%; }
.scan-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.scan-face-ring { width: 200rpx; height: 200rpx; border-radius: 50%; border: 2rpx dashed rgba(0,229,255,0.25); display: flex; align-items: center; justify-content: center; }
.scan-face { font-size: 110rpx; opacity: 0.3; }
.corner { position: absolute; width: 44rpx; height: 44rpx; border-color: #00E5FF; border-style: solid; }
.c-tl { top: 18rpx; left: 18rpx; border-width: 4rpx 0 0 4rpx; border-radius: 8rpx 0 0 0; }
.c-tr { top: 18rpx; right: 18rpx; border-width: 4rpx 4rpx 0 0; border-radius: 0 8rpx 0 0; }
.c-bl { bottom: 18rpx; left: 18rpx; border-width: 0 0 4rpx 4rpx; border-radius: 0 0 0 8rpx; }
.c-br { bottom: 18rpx; right: 18rpx; border-width: 0 4rpx 4rpx 0; border-radius: 0 0 8rpx 0; }
.laser { position: absolute; left: 18rpx; right: 18rpx; height: 3rpx; background: linear-gradient(90deg, transparent, #00E5FF, transparent); box-shadow: 0 0 20rpx #00E5FF; animation: laser 1.4s ease-in-out infinite; }
@keyframes laser { 0% { top: 30rpx; } 50% { top: 440rpx; } 100% { top: 30rpx; } }
.res-mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
.rm-PASS { background: rgba(0,230,150,0.25); }
.rm-NO_LEAVE { background: rgba(255,140,40,0.25); }
.res-icon { width: 130rpx; height: 130rpx; border-radius: 50%; background: rgba(255,255,255,0.95); color: #04081C; font-size: 70rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; }
.scan-tip { margin-top: 24rpx; font-size: 24rpx; color: rgba(255,255,255,0.5); text-align: center; padding: 0 40rpx; }
.scan-tip.active { color: #00E5FF; }

.scan-btn { margin: 36rpx 40rpx 0; height: 110rpx; display: flex; align-items: center; justify-content: center; gap: 16rpx; background: linear-gradient(135deg, #00C2D6, #2B7FFF); border-radius: 20rpx; box-shadow: 0 12rpx 32rpx rgba(0,194,214,0.35); }
.scan-press { opacity: 0.85; }
.scan-btn.disabled { opacity: 0.6; }
.scan-btn-icon { font-size: 40rpx; }
.scan-btn-text { font-size: 34rpx; font-weight: 800; color: #fff; letter-spacing: 4rpx; }

.res-card { margin: 32rpx 40rpx 0; border-radius: 20rpx; padding: 28rpx; border: 1rpx solid; }
.rc-pass { background: rgba(0,230,150,0.08); border-color: rgba(0,230,150,0.35); }
.rc-warn { background: rgba(255,140,40,0.08); border-color: rgba(255,140,40,0.35); }
.rc-none { background: rgba(255,255,255,0.04); border-color: rgba(255,255,255,0.1); text-align: center; }
.rc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.rc-badge { font-size: 28rpx; font-weight: 800; }
.rc-badge-pass { color: #00E696; }
.rc-badge-warn { color: #FF8C28; }
.rc-time { font-size: 22rpx; color: rgba(255,255,255,0.4); font-family: 'Courier New', monospace; }
.rc-stu { font-size: 34rpx; font-weight: 800; color: #fff; margin-bottom: 16rpx; }
.rc-cls { font-size: 24rpx; color: rgba(255,255,255,0.5); font-weight: 400; margin-left: 10rpx; }
.rc-rows { background: rgba(255,255,255,0.04); border-radius: 14rpx; padding: 10rpx 20rpx; }
.rc-row { display: flex; justify-content: space-between; padding: 12rpx 0; border-bottom: 1rpx solid rgba(255,255,255,0.05); }
.rc-row:last-child { border-bottom: none; }
.rc-k { font-size: 24rpx; color: rgba(255,255,255,0.45); }
.rc-v { font-size: 24rpx; color: #fff; }
.rc-v.mono { font-family: 'Courier New', monospace; color: #00E5FF; }
.rc-hint { display: block; font-size: 23rpx; color: rgba(255,255,255,0.55); line-height: 1.6; margin-bottom: 18rpx; }
.temp-input { width: 100%; min-height: 110rpx; padding: 18rpx; box-sizing: border-box; background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.12); border-radius: 12rpx; font-size: 26rpx; color: #fff; }
.temp-btn { margin-top: 18rpx; height: 92rpx; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #C4973A, #E8C068); border-radius: 16rpx; }
.temp-btn.disabled { opacity: 0.6; }
.temp-btn-text { font-size: 30rpx; font-weight: 800; color: #1A1200; letter-spacing: 2rpx; }
.rc-none-icon { display: block; font-size: 60rpx; margin-bottom: 12rpx; }
.rc-none-text { font-size: 26rpx; color: rgba(255,255,255,0.6); }
.next-btn { margin: 28rpx 40rpx 0; height: 88rpx; line-height: 88rpx; text-align: center; font-size: 28rpx; color: rgba(0,229,255,0.8); background: rgba(0,229,255,0.06); border: 1rpx solid rgba(0,229,255,0.25); border-radius: 16rpx; }
</style>
