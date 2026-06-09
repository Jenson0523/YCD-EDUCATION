<template>
  <view class="page">
    <!-- Tab -->
    <view class="tab-bar">
      <view v-for="tab in tabs" :key="tab.value" class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="switchTab(tab.value)">
        {{ tab.label }}
        <text v-if="tab.value === 'PENDING' && pendingCount > 0" class="badge">{{ pendingCount }}</text>
      </view>
    </view>

    <view v-if="loading" class="center">加载中…</view>
    <view v-else-if="list.length === 0" class="center">暂无记录</view>
    <scroll-view v-else scroll-y class="list">
      <view v-for="item in list" :key="item.id" class="card">
        <view class="card-header">
          <view>
            <text class="student-name">{{ item.studentName }}</text>
            <text class="class-name">{{ item.className }}</text>
          </view>
          <view :class="['status-badge', statusClass(item.status)]">{{ statusLabel(item.status) }}</view>
        </view>
        <view class="card-body">
          <view class="row">
            <text class="lbl">类型</text>
            <text>{{ item.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}</text>
            <text v-if="item.isTemp" class="temp-tag">⚡临时</text>
          </view>
          <view class="row"><text class="lbl">原因</text><text>{{ item.reason }}</text></view>
          <view class="row"><text class="lbl">离校</text><text>{{ fmtDt(item.leaveStart) }}</text></view>
          <view class="row"><text class="lbl">返校</text><text>{{ fmtDt(item.leaveEnd) }}</text></view>
          <view class="row"><text class="lbl">申请</text><text>{{ fmtDt(item.createdAt) }}</text></view>
        </view>
        <!-- 审批操作（仅 PENDING / TEMP_PENDING） -->
        <view v-if="item.status === 'PENDING' || item.status === 'TEMP_PENDING'" class="action-row">
          <button class="btn-reject" @click="openDialog(item, 'REJECTED')">驳回</button>
          <button class="btn-approve" @click="doApprove(item.id, 'APPROVED', '')">批准</button>
        </view>
      </view>
    </scroll-view>

    <!-- 驳回备注弹窗 -->
    <view v-if="rejectDialog.visible" class="modal-mask">
      <view class="modal">
        <view class="modal-title">填写驳回原因</view>
        <textarea v-model="rejectDialog.remark" placeholder="请填写驳回原因（必填）" class="modal-textarea" />
        <view class="modal-btns">
          <button class="btn-cancel" @click="rejectDialog.visible = false">取消</button>
          <button class="btn-confirm" @click="confirmReject">确认驳回</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const tabs = [
  { label: '待审批', value: 'PENDING' },
  { label: '临时补批', value: 'TEMP_PENDING' },
  { label: '已处理', value: 'APPROVED' },
];
const activeTab = ref('PENDING');
const list = ref([]);
const loading = ref(true);
const pendingCount = ref(0);
const rejectDialog = ref({ visible: false, id: null, remark: '' });

const load = async () => {
  loading.value = true;
  try {
    const data = await request({
      url: `/leave/applications?pageNo=1&pageSize=50&status=${activeTab.value}`
    });
    list.value = data?.records || [];
    if (activeTab.value === 'PENDING') pendingCount.value = data?.total || 0;
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally { loading.value = false; }
};

const switchTab = (v) => { activeTab.value = v; load(); };

const doApprove = async (id, action, remark) => {
  try {
    await request({ url: `/leave/applications/${id}/approve`, method: 'PUT', data: { action, remark } });
    uni.showToast({ title: action === 'APPROVED' ? '已批准 ✓' : '已驳回', icon: 'none' });
    load();
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  }
};

const openDialog = (item, action) => {
  rejectDialog.value = { visible: true, id: item.id, remark: '' };
};

const confirmReject = () => {
  if (!rejectDialog.value.remark.trim()) {
    uni.showToast({ title: '驳回原因不能为空', icon: 'none' }); return;
  }
  doApprove(rejectDialog.value.id, 'REJECTED', rejectDialog.value.remark);
  rejectDialog.value.visible = false;
};

const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
  DEPARTED: '已离校', TEMP_PENDING: '临时待补批'
}[s] || s);
const statusClass = (s) => ({
  PENDING: 'badge-yellow', TEMP_PENDING: 'badge-orange',
  APPROVED: 'badge-green', REJECTED: 'badge-red',
}[s] || 'badge-gray');

onMounted(load);
</script>

<style scoped>
.page { background: #f6f8fb; min-height: 100vh; }
.tab-bar { display: flex; background: #fff; border-bottom: 1rpx solid #e5e7eb; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: #6b7280; position: relative; }
.tab-item.active { color: #1b5ea6; border-bottom: 4rpx solid #1b5ea6; font-weight: 600; }
.badge { position: absolute; top: 10rpx; right: 12rpx; background: #ef4444; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 50rpx; }
.center { text-align: center; padding: 100rpx 0; color: #9ca3af; }
.list { padding: 20rpx 28rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16rpx; }
.student-name { font-size: 30rpx; font-weight: 700; color: #111827; }
.class-name { font-size: 22rpx; color: #9ca3af; margin-left: 12rpx; }
.status-badge { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 50rpx; }
.badge-yellow { background: #fef3c7; color: #d97706; }
.badge-orange { background: #ffedd5; color: #ea580c; }
.badge-green { background: #d1fae5; color: #059669; }
.badge-red { background: #fee2e2; color: #dc2626; }
.badge-gray { background: #f3f4f6; color: #6b7280; }
.row { display: flex; align-items: flex-start; margin-bottom: 10rpx; font-size: 26rpx; color: #374151; }
.lbl { width: 80rpx; color: #9ca3af; flex-shrink: 0; }
.temp-tag { margin-left: 12rpx; background: #fff7ed; color: #ea580c; font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; }
.action-row { display: flex; gap: 20rpx; margin-top: 20rpx; }
.btn-reject { flex: 1; height: 72rpx; background: #fff; border: 1rpx solid #e5e7eb; border-radius: 10rpx; color: #ef4444; font-size: 28rpx; }
.btn-approve { flex: 1; height: 72rpx; background: #1b5ea6; border: none; border-radius: 10rpx; color: #fff; font-size: 28rpx; }
/* 弹窗 */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal { background: #fff; border-radius: 20rpx; padding: 40rpx; width: 600rpx; }
.modal-title { font-size: 32rpx; font-weight: 700; margin-bottom: 24rpx; color: #111827; }
.modal-textarea { width: 100%; height: 160rpx; padding: 16rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.modal-btns { display: flex; gap: 20rpx; margin-top: 24rpx; }
.btn-cancel { flex: 1; height: 80rpx; background: #f3f4f6; border: none; border-radius: 10rpx; color: #374151; font-size: 28rpx; }
.btn-confirm { flex: 1; height: 80rpx; background: #ef4444; border: none; border-radius: 10rpx; color: #fff; font-size: 28rpx; }
</style>
