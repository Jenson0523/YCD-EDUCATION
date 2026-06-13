<template>
  <view class="page">
    <view class="bg-layer">
      <view class="bg-grid"></view>
      <view class="bg-glow"></view>
    </view>

    <view :style="{ height: statusBarH + 'px' }"></view>

    <!-- Header -->
    <view class="header">
      <view class="hd-back" hover-class="hd-hover" @click="goBack">‹</view>
      <view class="hd-center">
        <text class="hd-title">刷脸核验离校</text>
        <text class="hd-sub">AI 人脸库 1:N 自动识别</text>
      </view>
      <view class="hd-action" hover-class="hd-hover" @click="navToday">假条</view>
    </view>

    <scroll-view scroll-y class="scroll">
      <!-- 目标学生提示（从今日假条带入） -->
      <view v-if="targetStudentNo && !result" class="target-banner">
        <text class="tb-icon">🎯</text>
        <text class="tb-text">正在为学籍号 {{ targetStudentNo }} 刷脸放行，请采集该生面部</text>
      </view>

      <!-- 扫描区 -->
      <view class="scan-section">
        <view class="scan-frame" :class="scanState">
          <view class="scan-glow-ring" :class="{ active: scanning }"></view>
          <image v-if="capturePhoto" :src="capturePhoto" class="scan-img" mode="aspectFill" />
          <view v-else class="scan-empty">
            <view class="scan-face-ring">
              <text class="scan-face">👤</text>
            </view>
          </view>
          <view class="corner c-tl"></view>
          <view class="corner c-tr"></view>
          <view class="corner c-bl"></view>
          <view class="corner c-br"></view>
          <view v-if="scanning" class="laser"></view>
          <view v-if="picked && !scanning" class="ok-mask">
            <text class="ok-icon">✓</text>
          </view>
        </view>
        <text class="scan-tip" :class="{ active: scanning }">{{ scanTip }}</text>
      </view>

      <!-- 刷脸按钮 -->
      <view v-if="!picked" class="capture-btn" :class="{ disabled: scanning }" hover-class="cap-hover" @click="startScan">
        <view class="cap-icon-wrap">
          <text class="cap-icon">📷</text>
        </view>
        <text class="cap-text">{{ scanning ? '识别中…' : (capturePhoto ? '重新刷脸' : '点击刷脸识别') }}</text>
      </view>

      <!-- 未识别到（全部已离校或无未离校学生） -->
      <view v-if="scanned && candidates.length === 0 && !picked" class="no-cand">
        <text class="no-cand-icon">🔍</text>
        <text class="no-cand-text">未识别到待离校学生</text>
        <text class="no-cand-sub">今日所有已批准学生均已核验离校</text>
      </view>

      <!-- 识别候选列表 -->
      <view v-if="candidates.length > 0 && !picked" class="candidates">
        <view class="cand-head">
          <view class="cand-bar"></view>
          <text class="cand-title">识别到 {{ candidates.length }} 位候选</text>
          <text class="cand-hint">点击确认身份</text>
        </view>
        <view
          v-for="(c, i) in candidates"
          :key="c.studentId"
          class="cand-item"
          :class="{ 'cand-top': i === 0 }"
          hover-class="cand-hover"
          @click="pickCandidate(c)"
        >
          <image :src="resolvePhoto(c.facePhotoUrl)" class="cand-avatar" mode="aspectFill" />
          <view class="cand-info">
            <view class="cand-name-row">
              <text class="cand-name">{{ c.realName }}</text>
              <view v-if="i === 0" class="cand-best">最佳匹配</view>
            </view>
            <text class="cand-meta">{{ c.gradeName || '' }}{{ c.gradeName && c.className ? ' · ' : '' }}{{ c.className || '' }}</text>
            <text class="cand-meta-sub" v-if="c.headTeacherName">班主任：{{ c.headTeacherName }}</text>
          </view>
          <view class="cand-score">
            <text class="cand-score-num">{{ c.score }}</text>
            <text class="cand-score-unit">%</text>
          </view>
        </view>
      </view>

      <!-- 已选学生 + 核验 -->
      <view v-if="picked" class="picked-section">
        <view class="picked-card">
          <image :src="resolvePhoto(picked.facePhotoUrl)" class="pk-avatar" mode="aspectFill" />
          <view class="pk-info">
            <text class="pk-name">{{ picked.realName }}</text>
            <text class="pk-meta">{{ picked.gradeName || '' }}{{ picked.gradeName && picked.className ? ' · ' : '' }}{{ picked.className || '—' }}</text>
            <text class="pk-meta-sub" v-if="picked.headTeacherName">班主任：{{ picked.headTeacherName }}</text>
            <view class="pk-score">
              <view class="pk-score-bar"><view class="pk-score-fill" :style="{ width: picked.score + '%' }"></view></view>
              <text class="pk-score-text">匹配度 {{ picked.score }}%</text>
            </view>
          </view>
          <view class="pk-change" @click="resetPick">重选</view>
        </view>

        <!-- 预检中 -->
        <view v-if="checkingLeave" class="leave-checking">正在核验假条…</view>

        <!-- 有有效假条 → 确认放行 -->
        <template v-else-if="leaveInfo && leaveInfo.hasLeave">
          <view class="leave-found">
            <view class="lf-head"><text class="lf-badge">✓ 有效假条</text><text class="lf-no mono">{{ leaveInfo.leaveNo }}</text></view>
            <view class="lf-row"><text class="lf-k">类型</text><text class="lf-v">{{ leaveInfo.leaveType }}</text></view>
            <view class="lf-row"><text class="lf-k">离校</text><text class="lf-v mono">{{ fmtDt(leaveInfo.leaveStart) }}</text></view>
            <view class="lf-row"><text class="lf-k">返校</text><text class="lf-v mono">{{ fmtDt(leaveInfo.leaveEnd) }}</text></view>
            <view class="lf-row"><text class="lf-k">事由</text><text class="lf-v">{{ leaveInfo.reason }}</text></view>
          </view>
          <view class="verify-btn" :class="{ disabled: verifying }" hover-class="vb-hover" @click="doVerify">
            <text v-if="!verifying" class="vb-text">确 认 放 行</text>
            <view v-else class="vb-loading"><text class="vb-dot">●</text><text class="vb-dot">●</text><text class="vb-dot">●</text></view>
          </view>
        </template>

        <!-- 无有效假条 → 不予放行 + 临时放行 -->
        <template v-else-if="leaveInfo && !leaveInfo.hasLeave">
          <view class="leave-none">
            <text class="ln-icon">⛔</text>
            <text class="ln-title">无当日有效假条</text>
            <text class="ln-sub">该学生未提交请假或未审批通过，正常情况不予放行</text>
          </view>
          <view class="temp-block">
            <text class="temp-label">⚡ 临时紧急放行</text>
            <textarea
              v-model="tempReason"
              class="temp-input"
              placeholder="请填写临时放行原因（如家长来电、突发情况…），放行后将同步班主任补批"
              :maxlength="200"
            />
            <view class="temp-btn" :class="{ disabled: tempReleasing }" hover-class="tb-hover" @click="doTempRelease">
              <text v-if="!tempReleasing" class="tb-text">⚡ 临时放行并同步补批</text>
              <view v-else class="vb-loading"><text class="vb-dot">●</text><text class="vb-dot">●</text><text class="vb-dot">●</text></view>
            </view>
          </view>
        </template>
      </view>

      <!-- 核验结果 -->
      <view v-if="result" class="result-card" :class="'rc-' + result.result">
        <view class="rc-head">
          <view class="rc-icon-circle"><text class="rc-icon">{{ resultIcon }}</text></view>
          <view class="rc-titles">
            <text class="rc-title">{{ resultTitle }}</text>
            <text class="rc-time">{{ resultTime }}</text>
          </view>
        </view>
        <view class="rc-divider"></view>
        <view class="rc-rows">
          <view class="rc-row"><text class="rc-k">人脸匹配</text><text class="rc-v mono">{{ result.faceScore }} 分</text></view>
          <view v-if="result.studentName" class="rc-row"><text class="rc-k">学生</text><text class="rc-v">{{ result.studentName }}</text></view>
          <view v-if="result.className" class="rc-row"><text class="rc-k">班级</text><text class="rc-v">{{ result.gradeName }} {{ result.className }}</text></view>
          <view v-if="result.leaveNo" class="rc-row"><text class="rc-k">假条编号</text><text class="rc-v mono">{{ result.leaveNo }}</text></view>
          <view v-if="result.leaveType" class="rc-row"><text class="rc-k">请假类型</text><text class="rc-v">{{ result.leaveType }}</text></view>
          <view v-if="result.leaveStart" class="rc-row"><text class="rc-k">离校时间</text><text class="rc-v mono">{{ result.leaveStart }}</text></view>
          <view v-if="result.leaveEnd" class="rc-row"><text class="rc-k">预计返校</text><text class="rc-v mono">{{ result.leaveEnd }}</text></view>
          <view v-if="result.reason" class="rc-row rc-row-wrap"><text class="rc-k">请假事由</text><text class="rc-v">{{ result.reason }}</text></view>
          <view v-if="result.approverName" class="rc-row"><text class="rc-k">审批人</text><text class="rc-v">{{ result.approverName }}</text></view>
        </view>
        <view v-if="result.result === 'PASS' && result.leaveId" class="rc-detail-btn" hover-class="rc-detail-hover" @click="goLeaveDetail">
          查看假条详情 ›
        </view>
        <view class="rc-reset" hover-class="rc-reset-hover" @click="resetAll">继续核验下一位</view>
      </view>

      <view style="height: 60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { request, uploadFile, assetUrl } from '../../api/request';

const statusBarH = ref(20);
const capturePhoto = ref('');
const capturePhotoUrl = ref('');
const scanning = ref(false);
const scanned = ref(false); // 是否已完成过一次刷脸识别
const verifying = ref(false);
const candidates = ref([]);
const picked = ref(null);
const result = ref(null);
const resultTime = ref('');

const scanState = computed(() => {
  if (scanning.value) return 'scanning';
  if (result.value) return result.value.result === 'PASS' ? 'ok' : 'fail';
  if (picked.value) return 'locked';
  return '';
});

const scanTip = computed(() => {
  if (scanning.value) return '正在比对人脸库，请稍候…';
  if (picked.value) return '已锁定身份，点击核验放行';
  if (candidates.value.length) return '请确认识别结果';
  if (capturePhoto.value) return '未识别到匹配，请重试';
  return '请将学生面部对准取景框刷脸';
});

const resultIcon = computed(() => {
  if (!result.value) return '';
  if (result.value.result === 'PASS') return '✓';
  if (result.value.result === 'TEMP') return '⚡';
  if (result.value.result === 'NO_LEAVE') return '!';
  return '✕';
});
const resultTitle = computed(() => {
  if (!result.value) return '';
  return { PASS: '核验通过 · 放行', TEMP: '临时放行 · 待补批', NO_LEAVE: '无有效假条 · 拦截', FACE_MISMATCH: '人脸不匹配 · 拦截' }[result.value.result] || '已记录';
});

const resolvePhoto = (u) => assetUrl(u) || '/static/avatar-default.png';
const fmtDt = (dt) => dt ? String(dt).replace('T', ' ').slice(5, 16) : '—';

const targetStudentNo = ref(''); // 从今日假条"刷脸放行"带入的目标学生

onMounted(() => {
  // 角色守卫：仅门卫可进入人脸核验页
  const roleCode = uni.getStorageSync('ycd_roleCode') || '';
  if (roleCode !== 'GATE') {
    uni.showToast({ title: '仅门卫可操作核验', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 1500);
    return;
  }
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
  const pages = getCurrentPages();
  const opts = pages[pages.length - 1]?.options || {};
  if (opts.studentNo) targetStudentNo.value = decodeURIComponent(opts.studentNo);
});

const startScan = () => {
  if (scanning.value) return;
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: async (res) => {
      capturePhoto.value = res.tempFilePaths[0];
      result.value = null;
      candidates.value = [];
      picked.value = null;
      scanning.value = true;
      try {
        // 上传抓拍照
        let uploadedUrl = '';
        try { const up = await uploadFile(capturePhoto.value, 'capture'); uploadedUrl = up.url; } catch {}
        capturePhotoUrl.value = uploadedUrl;
        // 人脸库1:N识别
        const list = await request({
          url: '/leave/face/recognize', method: 'POST',
          data: { capturePhotoUrl: uploadedUrl }
        });
        // 模拟识别延迟，体现扫描动效
        await new Promise(r => setTimeout(r, 1200));
        candidates.value = list || [];
        scanned.value = true;
        // 若带了目标学生且识别结果中有该生，自动锁定（提升门卫效率）
        if (targetStudentNo.value && candidates.value.length) {
          const hit = candidates.value.find(c => c.studentNo === targetStudentNo.value);
          if (hit) { await pickCandidate(hit); }
        }
        if (!candidates.value.length) {
          uni.showToast({ title: '未识别到匹配学生，请重试或核对照片', icon: 'none' });
        }
      } catch (e) {
        uni.showToast({ title: e.message || '识别失败', icon: 'none' });
      } finally {
        scanning.value = false;
      }
    }
  });
};

// 锁定身份后预检假条状态
const leaveInfo = ref(null);   // { hasLeave, leaveNo, leaveType, leaveStart, leaveEnd, reason }
const checkingLeave = ref(false);
const tempReleasing = ref(false);
const tempReason = ref('');

const pickCandidate = async (c) => {
  picked.value = c;
  try { uni.vibrateShort(); } catch {}
  // 预检该学生今日是否有有效假条
  checkingLeave.value = true;
  leaveInfo.value = null;
  try {
    leaveInfo.value = await request({ url: `/leave/gate/check?studentNo=${c.studentNo}` });
  } catch (e) {
    leaveInfo.value = { hasLeave: false };
  } finally {
    checkingLeave.value = false;
  }
};
const resetPick = () => { picked.value = null; leaveInfo.value = null; tempReason.value = ''; };

// 有假条 → 确认放行核验
const doVerify = async () => {
  if (!picked.value || verifying.value) return;
  verifying.value = true;
  try {
    const verifyRes = await request({
      url: '/leave/gate/verify', method: 'POST',
      data: {
        studentNo: picked.value.studentNo,
        capturePhotoUrl: capturePhotoUrl.value,
        faceMatchScore: picked.value.score,
        verifyType: 'DEPART'
      }
    });
    const d = new Date();
    resultTime.value = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`;
    result.value = verifyRes;
    try { verifyRes.result === 'PASS' ? uni.vibrateShort() : uni.vibrateLong(); } catch {}
  } catch (e) {
    uni.showToast({ title: e.message || '核验失败', icon: 'none' });
  } finally {
    verifying.value = false;
  }
};

// 无假条 → 临时放行（生成临时假条，同步班主任补批）
const doTempRelease = async () => {
  if (!picked.value || tempReleasing.value) return;
  if (!tempReason.value.trim()) { uni.showToast({ title: '请填写临时放行原因', icon: 'none' }); return; }
  tempReleasing.value = true;
  try {
    await request({
      url: '/leave/applications/temp-depart', method: 'POST',
      data: {
        studentId: picked.value.studentId,
        studentNo: picked.value.studentNo,
        studentName: picked.value.realName,
        classId: picked.value.classId || null,
        className: picked.value.className || '',
        leaveType: 'PERSONAL',
        reason: tempReason.value
      }
    });
    // 同时记录一次核验放行日志
    try {
      await request({
        url: '/leave/gate/verify', method: 'POST',
        data: { studentNo: picked.value.studentNo, capturePhotoUrl: capturePhotoUrl.value,
                faceMatchScore: picked.value.score, verifyType: 'DEPART' }
      });
    } catch {}
    const d = new Date();
    resultTime.value = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`;
    result.value = {
      result: 'TEMP', message: '⚡ 已临时放行，假条已同步班主任补批',
      faceScore: picked.value.score, studentName: picked.value.realName
    };
    try { uni.vibrateShort(); } catch {}
  } catch (e) {
    uni.showToast({ title: e.message || '临时放行失败', icon: 'none' });
  } finally {
    tempReleasing.value = false;
  }
};

const resetAll = () => {
  capturePhoto.value = '';
  capturePhotoUrl.value = '';
  candidates.value = [];
  picked.value = null;
  result.value = null;
  scanned.value = false;
  leaveInfo.value = null;
  tempReason.value = '';
};

const goBack = () => uni.navigateBack();
const navToday = () => uni.navigateTo({ url: '/pages/gate/today-leaves' });
const goLeaveDetail = () => {
  if (result.value && result.value.leaveId) {
    uni.navigateTo({ url: '/pages/leave/detail?id=' + result.value.leaveId });
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid { position: absolute; inset: 0;
  background-image: linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx);
  background-size: 48rpx 48rpx; mask-image: linear-gradient(180deg, #000, transparent 70%); }
.bg-glow { position: absolute; width: 600rpx; height: 600rpx; border-radius: 50%; filter: blur(90rpx);
  background: rgba(0,229,255,0.15); top: -120rpx; right: -160rpx; }
.scroll { position: relative; z-index: 1; height: calc(100vh - 100rpx); }

/* Header */
.header { position: relative; z-index: 2; display: flex; align-items: center; padding: 16rpx 32rpx 20rpx; }
.hd-back { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center; font-size: 52rpx; color: rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 12rpx; }
.hd-hover { background: rgba(0,229,255,0.12); }
.hd-center { flex: 1; text-align: center; }
.hd-title { display: block; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.hd-sub { display: block; font-size: 20rpx; color: rgba(0,229,255,0.6); margin-top: 4rpx; font-family: 'Courier New', monospace; }
.hd-action { padding: 0 24rpx; height: 64rpx; line-height: 64rpx; font-size: 24rpx; color: #00E5FF;
  background: rgba(0,229,255,0.08); border: 1rpx solid rgba(0,229,255,0.3); border-radius: 12rpx; }

/* 目标学生提示 */
.target-banner { display: flex; align-items: center; gap: 12rpx; margin: 8rpx 40rpx 0; padding: 18rpx 24rpx;
  background: rgba(0,229,255,0.08); border: 1rpx solid rgba(0,229,255,0.3); border-radius: 14rpx; }
.tb-icon { font-size: 28rpx; }
.tb-text { flex: 1; font-size: 24rpx; color: #00E5FF; }

/* 扫描区 */
.scan-section { display: flex; flex-direction: column; align-items: center; padding: 24rpx 40rpx 0; }
.scan-frame { position: relative; width: 480rpx; height: 480rpx; border-radius: 24rpx; overflow: hidden;
  background: radial-gradient(circle at center, #0A1A40, #04081C); border: 1rpx solid rgba(0,229,255,0.2);
  transition: all 0.3s; }
.scan-frame.scanning { border-color: rgba(0,229,255,0.6); box-shadow: 0 0 50rpx rgba(0,229,255,0.3); }
.scan-frame.locked { border-color: rgba(0,229,255,0.5); }
.scan-frame.ok { border-color: rgba(0,230,150,0.6); box-shadow: 0 0 50rpx rgba(0,230,150,0.3); }
.scan-frame.fail { border-color: rgba(255,80,80,0.5); box-shadow: 0 0 50rpx rgba(255,80,80,0.25); }
.scan-glow-ring { position: absolute; inset: 0; opacity: 0; transition: opacity 0.3s; }
.scan-glow-ring.active { opacity: 1; background: radial-gradient(circle, transparent 55%, rgba(0,229,255,0.15) 75%); }
.scan-img { width: 100%; height: 100%; }
.scan-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.scan-face-ring { width: 200rpx; height: 200rpx; border-radius: 50%; border: 2rpx dashed rgba(0,229,255,0.25);
  display: flex; align-items: center; justify-content: center; }
.scan-face { font-size: 110rpx; opacity: 0.3; }
.corner { position: absolute; width: 44rpx; height: 44rpx; border-color: #00E5FF; border-style: solid; }
.c-tl { top: 18rpx; left: 18rpx; border-width: 4rpx 0 0 4rpx; border-radius: 8rpx 0 0 0; }
.c-tr { top: 18rpx; right: 18rpx; border-width: 4rpx 4rpx 0 0; border-radius: 0 8rpx 0 0; }
.c-bl { bottom: 18rpx; left: 18rpx; border-width: 0 0 4rpx 4rpx; border-radius: 0 0 0 8rpx; }
.c-br { bottom: 18rpx; right: 18rpx; border-width: 0 4rpx 4rpx 0; border-radius: 0 0 8rpx 0; }
.laser { position: absolute; left: 18rpx; right: 18rpx; height: 3rpx;
  background: linear-gradient(90deg, transparent, #00E5FF, transparent); box-shadow: 0 0 20rpx #00E5FF;
  animation: laser 1.6s ease-in-out infinite; }
@keyframes laser { 0% { top: 30rpx; } 50% { top: 440rpx; } 100% { top: 30rpx; } }
.ok-mask { position: absolute; inset: 0; background: rgba(0,229,255,0.12); display: flex; align-items: center; justify-content: center; }
.ok-icon { width: 120rpx; height: 120rpx; border-radius: 50%; background: rgba(0,229,255,0.9); color: #04081C;
  font-size: 64rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 30rpx rgba(0,229,255,0.6); }
.scan-tip { margin-top: 24rpx; font-size: 24rpx; color: rgba(255,255,255,0.45); }
.scan-tip.active { color: #00E5FF; }

/* 刷脸按钮 */
.capture-btn { margin: 36rpx 40rpx 0; display: flex; align-items: center; justify-content: center; gap: 18rpx; height: 104rpx;
  background: linear-gradient(135deg, rgba(0,229,255,0.15), rgba(43,127,255,0.15)); border: 1rpx solid rgba(0,229,255,0.4);
  border-radius: 18rpx; }
.cap-hover { background: rgba(0,229,255,0.25); }
.capture-btn.disabled { opacity: 0.5; }
.cap-icon-wrap { width: 56rpx; height: 56rpx; border-radius: 50%; background: rgba(0,229,255,0.2);
  display: flex; align-items: center; justify-content: center; }
.cap-icon { font-size: 32rpx; }
.cap-text { font-size: 30rpx; font-weight: 700; color: #00E5FF; letter-spacing: 4rpx; }

/* 候选列表 */
.candidates { margin: 36rpx 40rpx 0; }

/* 无候选提示 */
.no-cand { margin: 36rpx 40rpx 0; text-align: center; padding: 80rpx 0; }
.no-cand-icon { display: block; font-size: 80rpx; margin-bottom: 24rpx; }
.no-cand-text { display: block; font-size: 28rpx; font-weight: 600; color: rgba(255,255,255,0.5); }
.no-cand-sub { display: block; font-size: 22rpx; color: rgba(255,255,255,0.25); margin-top: 12rpx; }
.cand-head { display: flex; align-items: center; gap: 14rpx; margin-bottom: 20rpx; }
.cand-bar { width: 6rpx; height: 28rpx; background: linear-gradient(180deg,#00E5FF,#2B7FFF); border-radius: 3rpx; box-shadow: 0 0 10rpx rgba(0,229,255,0.6); }
.cand-title { font-size: 28rpx; font-weight: 700; color: #fff; flex: 1; }
.cand-hint { font-size: 22rpx; color: rgba(255,255,255,0.35); }
.cand-item { display: flex; align-items: center; gap: 20rpx; padding: 22rpx; margin-bottom: 16rpx;
  background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.08); border-radius: 16rpx; }
.cand-top { background: rgba(0,229,255,0.06); border-color: rgba(0,229,255,0.3); }
.cand-hover { background: rgba(0,229,255,0.12); }
.cand-avatar { width: 96rpx; height: 96rpx; border-radius: 14rpx; background: #0A1A40; flex-shrink: 0; border: 1rpx solid rgba(255,255,255,0.1); }
.cand-info { flex: 1; }
.cand-name-row { display: flex; align-items: center; gap: 12rpx; }
.cand-name { font-size: 30rpx; font-weight: 700; color: #fff; }
.cand-best { font-size: 18rpx; color: #00E5FF; background: rgba(0,229,255,0.12); border: 1rpx solid rgba(0,229,255,0.3); padding: 2rpx 12rpx; border-radius: 6rpx; }
.cand-meta { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 6rpx; font-family: 'Courier New', monospace; }
.cand-meta-sub { display: block; font-size: 20rpx; color: rgba(232,192,104,0.6); margin-top: 4rpx; }
.cand-score { display: flex; align-items: baseline; }
.cand-score-num { font-size: 40rpx; font-weight: 800; color: #00E5FF; font-family: 'Courier New', monospace; text-shadow: 0 0 12rpx rgba(0,229,255,0.4); }
.cand-score-unit { font-size: 22rpx; color: rgba(0,229,255,0.6); }

/* 已选 + 核验 */
.picked-section { margin: 36rpx 40rpx 0; }
.picked-card { display: flex; align-items: center; gap: 22rpx; padding: 28rpx;
  background: rgba(0,229,255,0.06); border: 1rpx solid rgba(0,229,255,0.35); border-radius: 18rpx; }
.pk-avatar { width: 120rpx; height: 120rpx; border-radius: 16rpx; background: #0A1A40; flex-shrink: 0; border: 1rpx solid rgba(0,229,255,0.3); }
.pk-info { flex: 1; }
.pk-name { display: block; font-size: 34rpx; font-weight: 800; color: #fff; }
.pk-meta { display: block; font-size: 22rpx; color: rgba(255,255,255,0.45); margin-top: 6rpx; font-family: 'Courier New', monospace; }
.pk-meta-sub { display: block; font-size: 20rpx; color: rgba(232,192,104,0.5); margin-top: 4rpx; }
.pk-score { margin-top: 16rpx; }
.pk-score-bar { width: 100%; height: 8rpx; background: rgba(255,255,255,0.1); border-radius: 4rpx; overflow: hidden; }
.pk-score-fill { height: 100%; background: linear-gradient(90deg, #00E5FF, #2B7FFF); border-radius: 4rpx; }
.pk-score-text { display: block; font-size: 20rpx; color: #00E5FF; margin-top: 8rpx; font-family: 'Courier New', monospace; }
.pk-change { font-size: 24rpx; color: rgba(255,255,255,0.5); padding: 8rpx 16rpx; }
.verify-btn { margin-top: 24rpx; height: 104rpx; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #00C2D6, #2B7FFF); border-radius: 18rpx; box-shadow: 0 12rpx 32rpx rgba(0,194,214,0.35); }
.vb-hover { opacity: 0.85; }
.verify-btn.disabled { opacity: 0.6; }
.vb-text { font-size: 34rpx; font-weight: 800; color: #fff; letter-spacing: 8rpx; }
.vb-loading { display: flex; gap: 12rpx; }
.vb-dot { font-size: 18rpx; color: #fff; animation: blink 1.2s infinite; }
.vb-dot:nth-child(2) { animation-delay: 0.2s; }
.vb-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%,100% { opacity: 0.3; } 50% { opacity: 1; } }

/* 预检中 */
.leave-checking { margin-top: 24rpx; padding: 28rpx; text-align: center; font-size: 26rpx; color: #00E5FF;
  background: rgba(0,229,255,0.05); border: 1rpx solid rgba(0,229,255,0.2); border-radius: 16rpx; }

/* 有有效假条 */
.leave-found { margin-top: 24rpx; padding: 24rpx; background: rgba(0,230,150,0.06);
  border: 1rpx solid rgba(0,230,150,0.3); border-radius: 16rpx; }
.lf-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx;
  padding-bottom: 16rpx; border-bottom: 1rpx solid rgba(255,255,255,0.08); }
.lf-badge { font-size: 26rpx; font-weight: 700; color: #00E696; }
.lf-no { font-size: 22rpx; color: rgba(255,255,255,0.4); }
.lf-row { display: flex; padding: 8rpx 0; }
.lf-k { width: 90rpx; font-size: 24rpx; color: rgba(255,255,255,0.4); flex-shrink: 0; }
.lf-v { flex: 1; font-size: 24rpx; color: rgba(255,255,255,0.85); }
.lf-v.mono { font-family: 'Courier New', monospace; color: #00E5FF; }

/* 无有效假条 */
.leave-none { margin-top: 24rpx; padding: 32rpx; text-align: center; background: rgba(255,80,80,0.06);
  border: 1rpx solid rgba(255,80,80,0.3); border-radius: 16rpx; }
.ln-icon { display: block; font-size: 56rpx; margin-bottom: 12rpx; }
.ln-title { display: block; font-size: 30rpx; font-weight: 700; color: #FF6B6B; }
.ln-sub { display: block; font-size: 22rpx; color: rgba(255,255,255,0.45); margin-top: 10rpx; line-height: 1.5; }

/* 临时放行 */
.temp-block { margin-top: 20rpx; padding: 24rpx; background: rgba(232,192,104,0.06);
  border: 1rpx solid rgba(232,192,104,0.3); border-radius: 16rpx; }
.temp-label { display: block; font-size: 26rpx; font-weight: 700; color: #E8C068; margin-bottom: 16rpx; }
.temp-input { width: 100%; min-height: 120rpx; padding: 18rpx; box-sizing: border-box;
  background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.12);
  border-radius: 12rpx; font-size: 26rpx; color: #fff; }
.temp-btn { margin-top: 18rpx; height: 96rpx; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #C4973A, #E8C068); border-radius: 16rpx; box-shadow: 0 10rpx 28rpx rgba(196,151,58,0.35); }
.tb-hover { opacity: 0.85; }
.temp-btn.disabled { opacity: 0.6; }
.tb-text { font-size: 30rpx; font-weight: 800; color: #1A1200; letter-spacing: 2rpx; }

/* 结果 */
.result-card { margin: 36rpx 40rpx 0; border-radius: 18rpx; overflow: hidden; border: 1rpx solid; }
.rc-PASS { background: rgba(0,230,150,0.08); border-color: rgba(0,230,150,0.35); }
.rc-TEMP { background: rgba(232,192,104,0.08); border-color: rgba(232,192,104,0.35); }
.rc-NO_LEAVE { background: rgba(232,192,104,0.08); border-color: rgba(232,192,104,0.35); }
.rc-FACE_MISMATCH { background: rgba(255,80,80,0.08); border-color: rgba(255,80,80,0.35); }
.rc-head { display: flex; align-items: center; gap: 20rpx; padding: 28rpx; }
.rc-icon-circle { width: 72rpx; height: 72rpx; border-radius: 50%; background: rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; }
.rc-icon { font-size: 38rpx; font-weight: 900; color: #fff; }
.rc-titles { flex: 1; }
.rc-title { display: block; font-size: 30rpx; font-weight: 700; color: #fff; }
.rc-time { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 4rpx; font-family: 'Courier New', monospace; }
.rc-divider { height: 1rpx; background: rgba(255,255,255,0.08); margin: 0 28rpx; }
.rc-rows { padding: 16rpx 28rpx; }
.rc-row { display: flex; justify-content: space-between; align-items: center; padding: 12rpx 0; border-bottom: 1rpx solid rgba(255,255,255,0.05); }
.rc-row:last-child { border-bottom: none; }
.rc-k { font-size: 24rpx; color: rgba(255,255,255,0.45); }
.rc-v { font-size: 24rpx; color: #fff; font-weight: 500; flex: 1; text-align: right; }
.rc-v.mono { font-family: 'Courier New', monospace; color: #00E5FF; }
.rc-row-wrap { flex-wrap: wrap; }
.rc-row-wrap .rc-v { text-align: left; margin-top: 6rpx; }
.rc-detail-btn { text-align: center; padding: 20rpx; font-size: 26rpx; color: #2B7FFF; font-weight: 600;
  border-top: 1rpx solid rgba(43,127,255,0.2); margin: 0 28rpx; }
.rc-detail-hover { background: rgba(43,127,255,0.08); }
.rc-reset { text-align: center; padding: 24rpx; font-size: 26rpx; color: rgba(0,229,255,0.7); border-top: 1rpx solid rgba(255,255,255,0.06); }
.rc-reset-hover { background: rgba(0,229,255,0.06); }
</style>
