<template>
  <view class="page">
    <view class="bg-layer"><view class="bg-grid"></view><view class="bg-glow"></view></view>
    <view :style="{ height: statusBarH + 'px' }"></view>

    <view class="header">
      <view class="hd-back" hover-class="hd-hover" @click="goBack">‹</view>
      <text class="hd-title">请假详情</text>
      <view style="width:64rpx"></view>
    </view>

    <scroll-view scroll-y class="scroll">
      <view v-if="loading" class="loading">加载中…</view>
      <view v-else-if="!data" class="loading">记录不存在</view>
      <template v-else>
        <!-- 状态卡 -->
        <view class="status-card" :class="'sc-' + data.status">
          <view class="sc-left">
            <text class="sc-status">{{ statusLabel(data.status) }}</text>
            <text class="sc-no mono">{{ data.leaveNo }}</text>
          </view>
          <view class="sc-type">{{ data.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}<text v-if="data.isTemp" class="sc-temp">⚡临时</text></view>
        </view>

        <!-- 学生信息 -->
        <view class="info-card">
          <view class="ic-head"><view class="ic-bar"></view><text class="ic-title">学生信息</text></view>
          <view class="ic-row"><text class="ic-k">姓名</text><text class="ic-v">{{ data.studentName }}</text></view>
          <view class="ic-row"><text class="ic-k">学籍号</text><text class="ic-v mono">{{ data.studentNo }}</text></view>
          <view class="ic-row"><text class="ic-k">班级</text><text class="ic-v">{{ data.className || '—' }}</text></view>
          <view class="ic-row"><text class="ic-k">申请人</text><text class="ic-v">{{ data.applicantLabel || roleLabel(data.applicantRole) }}</text></view>
        </view>

        <!-- 请假信息 -->
        <view class="info-card">
          <view class="ic-head"><view class="ic-bar"></view><text class="ic-title">请假信息</text></view>
          <view class="ic-row"><text class="ic-k">离校时间</text><text class="ic-v mono">{{ fmt(data.leaveStart) }}</text></view>
          <view class="ic-row"><text class="ic-k">返校时间</text><text class="ic-v mono">{{ fmt(data.leaveEnd) }}</text></view>
          <view class="ic-row col"><text class="ic-k">请假原因</text><text class="ic-v-block">{{ data.reason }}</text></view>
          <view v-if="data.proofPhotoUrl" class="ic-row col">
            <text class="ic-k">凭证 / 病例</text>
            <image :src="resolve(data.proofPhotoUrl)" class="proof-img" mode="aspectFill" @click="preview(data.proofPhotoUrl)" />
          </view>
        </view>

        <!-- 审批信息 -->
        <view v-if="data.status !== 'PENDING'" class="info-card">
          <view class="ic-head"><view class="ic-bar"></view><text class="ic-title">审批信息</text></view>
          <view class="ic-row"><text class="ic-k">审批结果</text>
            <text class="ic-v" :class="data.status === 'REJECTED' ? 'txt-red' : 'txt-green'">
              {{ data.status === 'REJECTED' ? '已驳回' : '已通过' }}
            </text>
          </view>
          <view v-if="data.approvedAt" class="ic-row"><text class="ic-k">审批时间</text><text class="ic-v mono">{{ fmt(data.approvedAt) }}</text></view>
          <view v-if="data.approveRemark" class="ic-row col"><text class="ic-k">审批意见</text><text class="ic-v-block">{{ data.approveRemark }}</text></view>
          <view v-if="data.approveSignatureUrl" class="ic-row col">
            <text class="ic-k">审批签字</text>
            <view class="sig-box"><image :src="resolve(data.approveSignatureUrl)" class="sig-img" mode="aspectFit" /></view>
          </view>
        </view>

        <!-- 离校核验 -->
        <view v-if="data.departAt" class="info-card">
          <view class="ic-head"><view class="ic-bar"></view><text class="ic-title">离校核验</text></view>
          <view class="ic-row"><text class="ic-k">离校时间</text><text class="ic-v mono">{{ fmt(data.departAt) }}</text></view>
          <view class="ic-row"><text class="ic-k">核验方式</text><text class="ic-v">门卫人脸核验</text></view>
        </view>

        <!-- 审批操作 -->
        <view v-if="data && (data.status === 'PENDING' || data.status === 'TEMP_PENDING')" class="action-bar">
          <view class="btn-reject" @click="openReject">驳回</view>
          <view class="btn-approve" @click="openApprove">批准并签字</view>
        </view>
        <!-- 人脸核验入口（已批准未离校，门卫用） -->
        <view v-if="data && data.status === 'APPROVED' && !data.departAt" class="action-bar">
          <view class="btn-verify" @click="goVerify">🔍 刷脸核验离校</view>
        </view>
        <view style="height: 60rpx;"></view>
      </template>
    </scroll-view>

    <!-- 驳回弹窗 -->
    <view v-if="rejectVisible" class="modal-mask" catchtouchmove="true" @click.self="closeReject">
      <view class="modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">填写驳回原因</text>
          <text class="modal-close" @click="closeReject">✕</text>
        </view>
        <textarea
          v-model="rejectRemark"
          placeholder="请填写驳回原因（必填）"
          class="modal-textarea"
          :maxlength="200"
          :auto-height="true"
          :fixed="true"
          :show-confirm-bar="false"
          :adjust-position="true"
        />
        <view class="modal-actions">
          <view class="modal-btn-cancel" @click="closeReject">取消</view>
          <view class="modal-btn-confirm" @click="confirmReject">确认驳回</view>
        </view>
      </view>
    </view>

    <!-- 签字批准弹窗 -->
    <view v-if="signVisible" class="modal-mask" catchtouchmove="true" @click.self="closeSign">
      <view class="sign-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">审批签字</text>
          <text class="modal-close" @click="closeSign">✕</text>
        </view>
        <text class="sign-tip">请在下方区域手写签名确认批准</text>
        <view class="sign-canvas-wrap">
          <canvas
            canvas-id="sigCanvasDetail"
            id="sigCanvasDetail"
            class="sign-canvas"
            disable-scroll
            @touchstart="sigStart"
            @touchmove="sigMove"
            @touchend="sigEnd"
          ></canvas>
          <text v-if="sigEmpty" class="sign-placeholder">在此处签名</text>
        </view>
        <view class="sign-toolbar">
          <view class="sign-clear" @click="clearSign">🗑 清除重写</view>
        </view>
        <textarea
          v-model="approveRemark"
          placeholder="审批意见（选填）"
          class="sign-remark"
          :maxlength="200"
          :auto-height="true"
          :fixed="true"
          :show-confirm-bar="false"
          :adjust-position="true"
        />
        <view class="modal-actions">
          <view class="modal-btn-cancel" @click="closeSign">取消</view>
          <view class="modal-btn-confirm green" :class="{ disabled: submitting }" @click="confirmApprove">
            {{ submitting ? '提交中…' : '确认批准' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue';
import { request, uploadFile, assetUrl } from '../../api/request';

const statusBarH = ref(20);
const loading = ref(true);
const data = ref(null);

// 审批操作状态
const rejectVisible = ref(false);
const rejectId = ref(null);
const rejectRemark = ref('');
const signVisible = ref(false);
const signId = ref(null);
const approveRemark = ref('');
const submitting = ref(false);
const sigEmpty = ref(true);
let sigCtx = null;
let lastX = 0, lastY = 0;
const instance = getCurrentInstance();

const resolve = (u) => assetUrl(u);
const fmt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({ PENDING:'待审批', APPROVED:'已批准', REJECTED:'已驳回', DEPARTED:'已离校', RETURNED:'已返校', TEMP_PENDING:'临时待补批' }[s] || s);
const roleLabel = (r) => ({ PARENT:'家长', TEACHER:'老师代申请', GATE:'门卫登记' }[r] || r);
const preview = (u) => uni.previewImage({ urls: [assetUrl(u)] });
const goBack = () => uni.navigateBack();
const goVerify = () => { uni.navigateTo({ url: '/pages/gate/verify' }); };

// ── 审批操作 ──
const openReject = () => {
  rejectRemark.value = '';
  rejectVisible.value = true;
};
const closeReject = () => { rejectVisible.value = false; };
const confirmReject = async () => {
  if (!rejectRemark.value.trim()) {
    uni.showToast({ title: '驳回原因不能为空', icon: 'none' }); return;
  }
  try {
    await request({ url: `/leave/applications/${data.value.id}/approve`, method: 'PUT',
      data: { action: 'REJECTED', remark: rejectRemark.value } });
    uni.showToast({ title: '已驳回', icon: 'none' });
    rejectVisible.value = false;
    data.value = await request({ url: `/leave/applications/${data.value.id}` });
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' });
  }
};

const openApprove = () => {
  approveRemark.value = '';
  sigEmpty.value = true;
  signVisible.value = true;
  setTimeout(() => {
    sigCtx = uni.createCanvasContext('sigCanvasDetail', instance);
    if (sigCtx) {
      sigCtx.setStrokeStyle('#1947C8');
      sigCtx.setLineWidth(4);
      sigCtx.setLineCap('round');
      sigCtx.setLineJoin('round');
    }
  }, 200);
};
const closeSign = () => { signVisible.value = false; sigCtx = null; };

const sigStart = (e) => {
  const t = e.touches[0];
  lastX = t.x; lastY = t.y;
  sigEmpty.value = false;
};
const sigMove = (e) => {
  if (!sigCtx) return;
  const t = e.touches[0];
  sigCtx.beginPath();
  sigCtx.moveTo(lastX, lastY);
  sigCtx.lineTo(t.x, t.y);
  sigCtx.stroke();
  sigCtx.draw(true);
  lastX = t.x; lastY = t.y;
};
const sigEnd = () => {};
const clearSign = () => {
  if (!sigCtx) return;
  sigCtx.clearRect(0, 0, 2000, 2000);
  sigCtx.draw();
  sigEmpty.value = true;
};

const confirmApprove = async () => {
  if (sigEmpty.value) { uni.showToast({ title: '请先签名', icon: 'none' }); return; }
  submitting.value = true;
  try {
    const tempPath = await new Promise((resolve, reject) => {
      uni.canvasToTempFilePath({
        canvasId: 'sigCanvasDetail',
        success: (res) => resolve(res.tempFilePath),
        fail: (err) => reject(err)
      }, instance);
    });
    let sigUrl = '';
    try { const up = await uploadFile(tempPath, 'signature'); sigUrl = up.url; } catch {}
    await request({
      url: `/leave/applications/${data.value.id}/approve`, method: 'PUT',
      data: { action: 'APPROVED', remark: approveRemark.value, signatureUrl: sigUrl }
    });
    uni.showToast({ title: '✓ 已签字批准', icon: 'none' });
    signVisible.value = false; sigCtx = null;
    data.value = await request({ url: `/leave/applications/${data.value.id}` });
  } catch (e) {
    uni.showToast({ title: e.message || '批准失败', icon: 'none' });
  } finally { submitting.value = false; }
};

onMounted(async () => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
  const pages = getCurrentPages();
  const id = pages[pages.length - 1]?.options?.id;
  if (!id) { loading.value = false; return; }
  try {
    data.value = await request({ url: `/leave/applications/${id}` });
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' });
  } finally { loading.value = false; }
});
</script>

<style scoped>
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx); background-size: 48rpx 48rpx; mask-image: linear-gradient(180deg,#000,transparent 70%); }
.bg-glow { position: absolute; width: 500rpx; height: 500rpx; border-radius: 50%; filter: blur(90rpx); background: rgba(43,127,255,0.15); top: -100rpx; right: -140rpx; }
.scroll { position: relative; z-index: 1; height: calc(100vh - 100rpx); }
.header { position: relative; z-index: 2; display: flex; align-items: center; padding: 16rpx 32rpx 20rpx; }
.hd-back { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center; font-size: 52rpx; color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 12rpx; }
.hd-hover { background: rgba(0,229,255,0.12); }
.hd-title { flex: 1; text-align: center; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.loading { text-align: center; padding: 120rpx 0; color: rgba(255,255,255,0.4); font-size: 28rpx; }

.status-card { margin: 16rpx 40rpx 0; display: flex; justify-content: space-between; align-items: center; padding: 32rpx; border-radius: 18rpx; border: 1rpx solid; }
.sc-PENDING { background: rgba(232,192,104,0.08); border-color: rgba(232,192,104,0.3); }
.sc-APPROVED, .sc-DEPARTED, .sc-RETURNED { background: rgba(0,230,150,0.08); border-color: rgba(0,230,150,0.3); }
.sc-REJECTED { background: rgba(255,80,80,0.08); border-color: rgba(255,80,80,0.3); }
.sc-TEMP_PENDING { background: rgba(255,140,40,0.08); border-color: rgba(255,140,40,0.3); }
.sc-status { display: block; font-size: 36rpx; font-weight: 800; color: #fff; }
.sc-no { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 8rpx; }
.sc-type { font-size: 26rpx; color: rgba(255,255,255,0.8); }
.sc-temp { color: #FF8C28; margin-left: 10rpx; }

.info-card { margin: 20rpx 40rpx 0; background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.08); border-radius: 18rpx; padding: 28rpx; }
.ic-head { display: flex; align-items: center; gap: 14rpx; margin-bottom: 20rpx; }
.ic-bar { width: 6rpx; height: 28rpx; background: linear-gradient(180deg,#00E5FF,#2B7FFF); border-radius: 3rpx; box-shadow: 0 0 10rpx rgba(0,229,255,0.5); }
.ic-title { font-size: 28rpx; font-weight: 700; color: #fff; }
.ic-row { display: flex; justify-content: space-between; align-items: center; padding: 14rpx 0; border-bottom: 1rpx solid rgba(255,255,255,0.05); }
.ic-row:last-child { border-bottom: none; }
.ic-row.col { flex-direction: column; align-items: flex-start; gap: 12rpx; }
.ic-k { font-size: 24rpx; color: rgba(255,255,255,0.4); }
.ic-v { font-size: 26rpx; color: #fff; font-weight: 500; text-align: right; }
.ic-v.mono { font-family: 'Courier New', monospace; color: #00E5FF; }
.ic-v-block { font-size: 26rpx; color: rgba(255,255,255,0.85); line-height: 1.6; }
.txt-green { color: #00E696 !important; }
.txt-red { color: #FF6B6B !important; }
.proof-img { width: 200rpx; height: 200rpx; border-radius: 12rpx; border: 1rpx solid rgba(255,255,255,0.1); }
.sig-box { width: 100%; height: 200rpx; background: #fff; border-radius: 12rpx; padding: 10rpx; box-sizing: border-box; }
.sig-img { width: 100%; height: 100%; }

/* 审批操作按钮 */
.action-bar { display: flex; gap: 20rpx; margin: 28rpx 40rpx 0; }
.btn-reject { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: rgba(255,80,80,0.15); border: 1rpx solid rgba(255,80,80,0.3); border-radius: 14rpx; font-size: 28rpx; color: #FF6B6B; font-weight: 600; }
.btn-approve { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: linear-gradient(135deg, #00C2D6, #2B7FFF); border-radius: 14rpx; font-size: 28rpx; color: #fff; font-weight: 700; }
.btn-verify { height: 88rpx; line-height: 88rpx; text-align: center; background: linear-gradient(135deg, #00E5FF, #2B7FFF); border-radius: 14rpx; font-size: 28rpx; color: #fff; font-weight: 700; box-shadow: 0 6rpx 20rpx rgba(0,229,255,0.3); }

/* 弹窗通用 */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: flex-end; justify-content: center; z-index: 999; }
.modal { background: #1A2340; border-radius: 28rpx 28rpx 0 0; padding: 32rpx; width: 100%; box-sizing: border-box; max-height: 85vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.modal-title { font-size: 32rpx; font-weight: 700; color: #fff; }
.modal-close { font-size: 32rpx; color: rgba(255,255,255,0.4); padding: 8rpx; }
.modal-textarea { width: 100%; min-height: 160rpx; padding: 20rpx; background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 16rpx; font-size: 28rpx; color: #fff; box-sizing: border-box; }
.modal-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.modal-btn-cancel { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: rgba(255,255,255,0.08); border-radius: 16rpx; font-size: 28rpx; color: rgba(255,255,255,0.6); }
.modal-btn-confirm { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: linear-gradient(135deg, #EF4444, #DC2626); border-radius: 16rpx; font-size: 28rpx; color: #fff; font-weight: 700; }
.modal-btn-confirm.green { background: linear-gradient(135deg, #059669, #10B981); }
.modal-btn-confirm.disabled { opacity: 0.6; }

/* 签字弹窗 */
.sign-modal { background: #1A2340; border-radius: 28rpx 28rpx 0 0; padding: 32rpx; width: 100%; box-sizing: border-box; max-height: 90vh; overflow-y: auto; }
.sign-tip { display: block; font-size: 24rpx; color: rgba(255,255,255,0.4); margin-bottom: 16rpx; }
.sign-canvas-wrap { position: relative; width: 100%; height: 320rpx; background: rgba(255,255,255,0.05); border: 2rpx dashed rgba(255,255,255,0.15); border-radius: 16rpx; overflow: hidden; }
.sign-canvas { width: 100%; height: 320rpx; }
.sign-placeholder { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 40rpx; color: rgba(255,255,255,0.1); pointer-events: none; }
.sign-toolbar { display: flex; justify-content: flex-end; margin-top: 14rpx; }
.sign-clear { font-size: 24rpx; color: rgba(255,255,255,0.5); padding: 8rpx 20rpx; background: rgba(255,255,255,0.06); border-radius: 10rpx; }
.sign-remark { width: 100%; min-height: 80rpx; margin-top: 16rpx; padding: 16rpx 20rpx; background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 14rpx; font-size: 26rpx; color: #fff; box-sizing: border-box; }
</style>
