<template>
  <view class="page">
    <!-- Hero Header -->
    <view class="hero">
      <view class="hero-orb"></view>
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @tap="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <text class="hero-title">请假审批</text>
          <view class="pending-chip" v-if="pendingCount > 0">
            {{ pendingCount }} 条待审批
          </view>
          <view style="width:60rpx;" v-else></view>
        </view>
      </view>

      <!-- Tab Bar -->
      <view class="tab-bar">
        <view
          v-for="tab in tabs" :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @tap="switchTab(tab.value)"
        >
          {{ tab.label }}
          <view v-if="tab.value === 'ALL_PENDING' && pendingCount > 0" class="tab-badge">{{ pendingCount }}</view>
        </view>
        <view class="tab-ink" :style="{ left: tabInkLeft + 'rpx' }"></view>
      </view>
    </view>

    <!-- 内容区 -->
    <scroll-view scroll-y class="content-scroll" :style="{ height: 'calc(100vh - 300rpx)' }">
      <view v-if="loading" class="skeleton-area">
        <view class="sk-card" v-for="i in 3" :key="i">
          <view class="sk-row sk-w60"></view>
          <view class="sk-row sk-w40 sk-h18"></view>
          <view class="sk-row sk-w80 sk-h18"></view>
        </view>
      </view>

      <view v-else-if="list.length === 0" class="empty">
        <text class="empty-icon">{{ emptyIcon }}</text>
        <text class="empty-title">{{ emptyTitle }}</text>
        <text class="empty-sub">{{ emptySub }}</text>
      </view>

      <view v-else class="list">
        <view
          v-for="item in list" :key="item.id"
          class="apply-card"
          :class="{ 'card-pending': item.status === 'PENDING' || item.status === 'TEMP_PENDING' }"
        >
          <!-- 待审批卡片：点击整张卡片进入详情页 -->
          <view
            v-if="item.status === 'PENDING' || item.status === 'TEMP_PENDING'"
            class="ac-clickable"
            @tap="goDetail(item.id)"
          >
            <view class="ac-header">
              <view class="ac-avatar">
                <text class="ac-initial">{{ item.studentName?.charAt(0) || '学' }}</text>
              </view>
              <view class="ac-meta">
                <view class="ac-name-row">
                  <text class="ac-name">{{ item.studentName }}</text>
                  <text class="ac-class">{{ item.className || '未分配班级' }}</text>
                </view>
                <view class="ac-applicant" v-if="item.applicantLabel">{{ item.applicantLabel }}</view>
                <text class="ac-time">申请于 {{ fmtDt(item.createdAt) }}</text>
              </view>
              <view class="ac-status ast-PENDING">
                {{ item.status === 'TEMP_PENDING' ? '待补批' : '待审批' }}
              </view>
            </view>
            <view class="ac-body">
              <view class="ac-row">
                <view class="ac-type" :class="item.leaveType === 'SICK' ? 'type-sick' : 'type-personal'">
                  {{ item.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}
                </view>
                <view v-if="item.isTemp" class="ac-temp-tag">⚡ 临时</view>
              </view>
              <view class="ac-reason">{{ item.reason }}</view>
              <view class="ac-time-range">
                <view class="atr-item">
                  <text class="atr-label">离校</text>
                  <text class="atr-val">{{ fmtDt(item.leaveStart) }}</text>
                </view>
                <text class="atr-sep">→</text>
                <view class="atr-item">
                  <text class="atr-label">返校</text>
                  <text class="atr-val">{{ fmtDt(item.leaveEnd) }}</text>
                </view>
              </view>
            </view>
            <!-- 快捷操作：点击卡片进入签字，但保留驳回按钮 -->
            <view class="ac-actions-inline">
              <view class="btn-reject-sm" @tap.stop="openReject(item)">
                <text class="btn-reject-text">✕ 驳回</text>
              </view>
              <view class="btn-approve-sm" @tap.stop="openApprove(item)">
                <text class="btn-approve-text">✓ 签字批准</text>
              </view>
            </view>
            <view class="ac-tap-hint">
              <text>点击卡片直接审批 ›</text>
            </view>
          </view>

          <!-- 已处理卡片：点击查看详情 -->
          <view v-else @tap="goDetail(item.id)">
            <view class="ac-header">
              <view class="ac-avatar">
                <text class="ac-initial">{{ item.studentName?.charAt(0) || '学' }}</text>
              </view>
              <view class="ac-meta">
                <view class="ac-name-row">
                  <text class="ac-name">{{ item.studentName }}</text>
                  <text class="ac-class">{{ item.className || '未分配班级' }}</text>
                </view>
                <text class="ac-time">申请于 {{ fmtDt(item.createdAt) }}</text>
              </view>
              <view class="ac-status" :class="'ast-' + item.status">
                {{ statusLabel(item.status) }}
              </view>
            </view>
            <view class="ac-body">
              <view class="ac-row">
                <view class="ac-type" :class="item.leaveType === 'SICK' ? 'type-sick' : 'type-personal'">
                  {{ item.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}
                </view>
                <view v-if="item.isTemp" class="ac-temp-tag">⚡ 临时</view>
                <text class="ac-detail-link">详情 ›</text>
              </view>
              <view class="ac-reason">{{ item.reason }}</view>
              <view class="ac-time-range">
                <view class="atr-item">
                  <text class="atr-label">离校</text>
                  <text class="atr-val">{{ fmtDt(item.leaveStart) }}</text>
                </view>
                <text class="atr-sep">→</text>
                <view class="atr-item">
                  <text class="atr-label">返校</text>
                  <text class="atr-val">{{ fmtDt(item.leaveEnd) }}</text>
                </view>
              </view>
            </view>
            <!-- 审批结果 -->
            <view v-if="item.approveRemark || item.approveSignatureUrl" class="ac-result">
              <text v-if="item.approveRemark" class="ar-label">审批意见：</text>
              <text v-if="item.approveRemark" class="ar-text">{{ item.approveRemark }}</text>
              <view v-if="item.approveSignatureUrl" class="ar-sig">
                <text class="ar-sig-label">审批签字：</text>
                <image :src="resolve(item.approveSignatureUrl)" class="ar-sig-img" mode="aspectFit" />
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 驳回弹窗 -->
    <view v-if="rejectVisible" class="modal-mask" catchtouchmove="true" @tap.self="closeReject">
      <view class="modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">填写驳回原因</text>
          <text class="modal-close" @tap="closeReject">✕</text>
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
          <view class="modal-btn-cancel" @tap="closeReject">取消</view>
          <view class="modal-btn-confirm" @tap="confirmReject">确认驳回</view>
        </view>
      </view>
    </view>

    <!-- 签字批准弹窗 -->
    <view v-if="signVisible" class="modal-mask" catchtouchmove="true" @tap.self="closeSign">
      <view class="sign-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">审批签字</text>
          <text class="modal-close" @tap="closeSign">✕</text>
        </view>
        <text class="sign-tip">请在下方区域手写签名确认批准</text>
        <view class="sign-canvas-wrap">
          <canvas
            canvas-id="sigCanvas"
            id="sigCanvas"
            class="sign-canvas"
            disable-scroll
            @touchstart="sigStart"
            @touchmove="sigMove"
            @touchend="sigEnd"
          ></canvas>
          <text v-if="sigEmpty" class="sign-placeholder">在此处签名</text>
        </view>
        <view class="sign-toolbar">
          <view class="sign-clear" @tap="clearSign">🗑 清除重写</view>
        </view>
        <!-- 用 textarea 替代 input，解决键盘弹出闪退问题 -->
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
          <view class="modal-btn-cancel" @tap="closeSign">取消</view>
          <view class="modal-btn-confirm green" :class="{ disabled: submitting }" @tap="confirmApprove">
            {{ submitting ? '提交中…' : '确认批准' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { getCurrentInstance } from 'vue';
import { request, uploadFile, assetUrl } from '../../api/request';

const tabs = [
  { label: '全部待处理', value: 'ALL_PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
];
const activeTab = ref('ALL_PENDING');
const list = ref([]);
const loading = ref(true);
const pendingCount = ref(0);
const rejectVisible = ref(false);
const rejectId = ref(null);
const rejectRemark = ref('');

// 签字批准
const signVisible = ref(false);
const signId = ref(null);
const approveRemark = ref('');
const submitting = ref(false);
const sigEmpty = ref(true);
let sigCtx = null;
let lastX = 0, lastY = 0;
const instance = getCurrentInstance();

const tabInkLeft = computed(() => {
  const idx = tabs.findIndex(t => t.value === activeTab.value);
  return (idx >= 0 ? idx : 0) * (750 / tabs.length);
});

const load = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 'ALL_PENDING') {
      const [d1, d2] = await Promise.all([
        request({ url: '/leave/applications?pageNo=1&pageSize=50&status=PENDING' }),
        request({ url: '/leave/applications?pageNo=1&pageSize=50&status=TEMP_PENDING' })
      ]);
      const r1 = (d1?.records || []);
      const r2 = (d2?.records || []);
      list.value = [...r1, ...r2];
      pendingCount.value = (d1?.total || 0) + (d2?.total || 0);
    } else {
      const data = await request({
        url: `/leave/applications?pageNo=1&pageSize=50&status=${activeTab.value}`
      });
      list.value = data?.records || [];
    }
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' });
  } finally { loading.value = false; }
};

const switchTab = (v) => { activeTab.value = v; load(); };

const doApprove = async (id, action, remark) => {
  try {
    await request({ url: `/leave/applications/${id}/approve`, method: 'PUT', data: { action, remark } });
    uni.showToast({ title: action === 'APPROVED' ? '✓ 已批准' : '已驳回', icon: 'none' });
    load();
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' });
  }
};

const openReject = (item) => {
  rejectId.value = item.id;
  rejectRemark.value = '';
  rejectVisible.value = true;
};

const closeReject = () => { rejectVisible.value = false; };

const confirmReject = () => {
  if (!rejectRemark.value.trim()) {
    uni.showToast({ title: '驳回原因不能为空', icon: 'none' }); return;
  }
  doApprove(rejectId.value, 'REJECTED', rejectRemark.value);
  rejectVisible.value = false;
};

// ── 签字批准 ──
const openApprove = (item) => {
  signId.value = item.id;
  approveRemark.value = '';
  sigEmpty.value = true;
  signVisible.value = true;
  // 延迟初始化 canvas（等待 DOM 渲染）
  setTimeout(() => {
    sigCtx = uni.createCanvasContext('sigCanvas', instance);
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
        canvasId: 'sigCanvas',
        success: (res) => resolve(res.tempFilePath),
        fail: (err) => reject(err)
      }, instance);
    });
    let sigUrl = '';
    try { const up = await uploadFile(tempPath, 'signature'); sigUrl = up.url; } catch {}
    await request({
      url: `/leave/applications/${signId.value}/approve`, method: 'PUT',
      data: { action: 'APPROVED', remark: approveRemark.value, signatureUrl: sigUrl }
    });
    uni.showToast({ title: '✓ 已签字批准', icon: 'none' });
    signVisible.value = false;
    sigCtx = null;
    load();
  } catch (e) {
    uni.showToast({ title: e.message || '批准失败', icon: 'none' });
  } finally { submitting.value = false; }
};

const resolve = (u) => assetUrl(u);
const goDetail = (id) => uni.navigateTo({ url: `/pages/leave/detail?id=${id}` });

const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
  DEPARTED: '已离校', TEMP_PENDING: '待补批'
}[s] || s);

const emptyIcon = computed(() => {
  if (activeTab.value === 'ALL_PENDING') return '📭';
  if (activeTab.value === 'APPROVED') return '✅';
  return '📋';
});
const emptyTitle = computed(() => {
  if (activeTab.value === 'ALL_PENDING') return '暂无待处理的请假申请';
  if (activeTab.value === 'APPROVED') return '暂无已批准的请假';
  return '暂无记录';
});
const emptySub = computed(() => {
  if (activeTab.value === 'ALL_PENDING') return '有新的请假申请时会自动显示在这里';
  return '';
});

onMounted(() => { load(); });
</script>

<style scoped>
.page { background: #F0F4FA; min-height: 100vh; }

/* Hero */
.hero {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #06133D 0%, #0C2470 50%, #1947C8 100%);
  padding: 56rpx 32rpx 0;
}
.hero-orb { position: absolute; width: 400rpx; height: 400rpx; background: rgba(43,127,255,0.2); filter: blur(60rpx); border-radius: 50%; top: -100rpx; left: -100rpx; }
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; margin-bottom: 28rpx; }
.back-btn { width: 64rpx; height: 64rpx; background: rgba(255,255,255,0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 52rpx; color: #fff; }
.hero-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 700; color: #fff; }
.pending-chip { background: rgba(239,68,68,0.2); border: 1rpx solid rgba(239,68,68,0.5); border-radius: 30rpx; padding: 6rpx 18rpx; font-size: 22rpx; color: #FCA5A5; }

/* Tab */
.tab-bar { position: relative; display: flex; background: rgba(255,255,255,0.06); }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: rgba(255,255,255,0.5); position: relative; z-index: 1; }
.tab-item.active { color: #fff; font-weight: 600; }
.tab-badge { position: absolute; top: 10rpx; right: 10rpx; background: #EF4444; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 30rpx; }
.tab-ink { position: absolute; bottom: 0; width: calc(100% / 3); height: 4rpx; background: linear-gradient(90deg, #E8C068, #2B7FFF); border-radius: 2rpx; transition: left 0.25s; }

/* Content */
.content-scroll { padding: 24rpx 28rpx; }

/* Skeleton */
.sk-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.sk-row { background: #EEF2F8; border-radius: 6rpx; margin-bottom: 14rpx; height: 24rpx; }
.sk-w60 { width: 60%; }
.sk-w40 { width: 40%; }
.sk-w80 { width: 80%; }
.sk-h18 { height: 18rpx; }

/* Empty */
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 20rpx; }
.empty-title { display: block; font-size: 30rpx; font-weight: 600; color: #374151; }
.empty-sub { display: block; font-size: 24rpx; color: #9CA3AF; margin-top: 8rpx; }

/* Apply Card */
.apply-card { background: #fff; border-radius: 20rpx; margin-bottom: 20rpx; overflow: hidden; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05); }
.apply-card.card-pending { border: 2rpx solid #1947C8; box-shadow: 0 4rpx 20rpx rgba(25,71,200,0.12); }
.ac-clickable { position: relative; }

.ac-header { display: flex; align-items: center; gap: 16rpx; padding: 24rpx 24rpx 20rpx; }
.ac-avatar { width: 80rpx; height: 80rpx; border-radius: 20rpx; background: linear-gradient(135deg, #1947C8, #2B7FFF); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ac-initial { font-size: 34rpx; font-weight: 700; color: #fff; }
.ac-meta { flex: 1; }
.ac-name-row { display: flex; align-items: center; gap: 12rpx; }
.ac-name { font-size: 30rpx; font-weight: 700; color: #111827; }
.ac-class { font-size: 22rpx; color: #9CA3AF; background: #F3F4F6; padding: 2rpx 12rpx; border-radius: 20rpx; }
.ac-time { display: block; font-size: 20rpx; color: #9CA3AF; margin-top: 6rpx; }
.ac-applicant { display: inline-block; font-size: 20rpx; color: #2B7FFF; background: #EFF6FF; padding: 3rpx 14rpx; border-radius: 20rpx; margin-top: 8rpx; }

.ac-status { font-size: 22rpx; padding: 6rpx 18rpx; border-radius: 30rpx; font-weight: 600; flex-shrink: 0; }
.ast-PENDING { background: #FEF9C3; color: #CA8A04; }
.ast-TEMP_PENDING { background: #FFEDD5; color: #EA580C; }
.ast-APPROVED { background: #D1FAE5; color: #059669; }
.ast-REJECTED { background: #FEE2E2; color: #DC2626; }

.ac-body { padding: 0 24rpx 20rpx; }
.ac-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.ac-type { font-size: 24rpx; font-weight: 500; }
.type-sick { color: #DC2626; }
.type-personal { color: #2563EB; }
.ac-temp-tag { background: #FFF7ED; color: #EA580C; font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 20rpx; }
.ac-detail-link { margin-left: auto; font-size: 22rpx; color: #2B7FFF; }
.ac-reason { font-size: 26rpx; color: #374151; margin-bottom: 16rpx; line-height: 1.6; }
.ac-time-range { display: flex; align-items: center; gap: 16rpx; background: #F8FAFC; border-radius: 12rpx; padding: 16rpx 20rpx; }
.atr-item { flex: 1; }
.atr-label { display: block; font-size: 20rpx; color: #9CA3AF; }
.atr-val { display: block; font-size: 24rpx; color: #374151; font-weight: 600; margin-top: 4rpx; }
.atr-sep { font-size: 26rpx; color: #CBD5E1; }

/* 点击提示 */
.ac-tap-hint { padding: 8rpx 24rpx 16rpx; text-align: right; }
.ac-tap-hint text { font-size: 22rpx; color: #2B7FFF; }

/* 行内快捷操作 */
.ac-actions-inline { display: flex; gap: 0; border-top: 1rpx solid #EEF2FF; }
.btn-reject-sm { flex: 1; text-align: center; padding: 22rpx 0; border-right: 1rpx solid #F3F4F6; background: #FEF2F2; }
.btn-reject-text { font-size: 26rpx; color: #EF4444; font-weight: 600; }
.btn-approve-sm { flex: 1; text-align: center; padding: 22rpx 0; background: #EEF2FF; }
.btn-approve-text { font-size: 26rpx; color: #1947C8; font-weight: 700; }

.ac-result { padding: 16rpx 24rpx; background: #FFF7F7; border-top: 1rpx solid #FEE2E2; display: flex; gap: 10rpx; }
.ar-label { font-size: 22rpx; color: #9CA3AF; flex-shrink: 0; }
.ar-text { font-size: 22rpx; color: #EF4444; flex: 1; }

/* 弹窗 */
.modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: flex-end; justify-content: center;
  z-index: 999;
}
.modal { background: #fff; border-radius: 28rpx 28rpx 0 0; padding: 32rpx; width: 100%; box-sizing: border-box; max-height: 85vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.modal-title { font-size: 32rpx; font-weight: 700; color: #111827; }
.modal-close { font-size: 32rpx; color: #9CA3AF; padding: 8rpx; }
.modal-textarea {
  width: 100%; min-height: 160rpx; padding: 20rpx;
  background: #F8FAFC; border: 1rpx solid #E2E8F0; border-radius: 16rpx;
  font-size: 28rpx; box-sizing: border-box;
}
.modal-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.modal-btn-cancel { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: #F3F4F6; border-radius: 16rpx; font-size: 28rpx; color: #374151; }
.modal-btn-confirm { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; background: linear-gradient(135deg, #DC2626, #EF4444); border-radius: 16rpx; font-size: 28rpx; color: #fff; font-weight: 700; }
.modal-btn-confirm.green { background: linear-gradient(135deg, #059669, #10B981); }
.modal-btn-confirm.disabled { opacity: 0.6; }

/* 签字弹窗 */
.sign-modal { background: #fff; border-radius: 28rpx 28rpx 0 0; padding: 32rpx; width: 100%; box-sizing: border-box; max-height: 90vh; overflow-y: auto; }
.sign-tip { display: block; font-size: 24rpx; color: #9CA3AF; margin-bottom: 16rpx; }
.sign-canvas-wrap { position: relative; width: 100%; height: 320rpx; background: #F8FAFC; border: 2rpx dashed #CBD5E1; border-radius: 16rpx; overflow: hidden; }
.sign-canvas { width: 100%; height: 320rpx; }
.sign-placeholder { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 40rpx; color: #E2E8F0; pointer-events: none; }
.sign-toolbar { display: flex; justify-content: flex-end; margin-top: 14rpx; }
.sign-clear { font-size: 24rpx; color: #64748B; padding: 8rpx 20rpx; background: #F1F5F9; border-radius: 10rpx; }
.sign-remark {
  width: 100%; min-height: 80rpx; margin-top: 16rpx; padding: 16rpx 20rpx;
  background: #F8FAFC; border: 1rpx solid #E2E8F0; border-radius: 14rpx;
  font-size: 26rpx; box-sizing: border-box;
}
.ar-sig { padding: 16rpx 24rpx; background: #FFF7F7; border-top: 1rpx solid #FEE2E2; }
.ar-sig-label { font-size: 22rpx; color: #9CA3AF; flex-shrink: 0; margin-bottom: 8rpx; display: block; }
.ar-sig-img { width: 100%; height: 120rpx; border: 1rpx solid #FEE2E2; border-radius: 8rpx; }
</style>
