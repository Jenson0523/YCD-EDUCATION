<template>
  <view class="page">
    <!-- Tab 筛选 -->
    <view class="tab-bar">
      <view v-for="tab in tabs" :key="tab.value" class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="switchTab(tab.value)">
        {{ tab.label }}
      </view>
    </view>

    <view v-if="loading" class="center">加载中…</view>
    <view v-else-if="list.length === 0" class="center">暂无记录</view>
    <view v-else class="list">
      <view v-for="item in list" :key="item.id" class="card">
        <view class="card-header">
          <text class="leave-no">{{ item.leaveNo }}</text>
          <view class="status-badge" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</view>
        </view>
        <view class="card-body">
          <view class="row">
            <text class="label">类型</text>
            <text class="value">{{ item.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}</text>
          </view>
          <view class="row">
            <text class="label">原因</text>
            <text class="value">{{ item.reason }}</text>
          </view>
          <view class="row">
            <text class="label">离校</text>
            <text class="value">{{ fmtDt(item.leaveStart) }}</text>
          </view>
          <view class="row">
            <text class="label">返校</text>
            <text class="value">{{ fmtDt(item.leaveEnd) }}</text>
          </view>
          <view v-if="item.approveRemark" class="row remark">
            <text class="label">备注</text>
            <text class="value">{{ item.approveRemark }}</text>
          </view>
        </view>
        <!-- 临时待补批标识 -->
        <view v-if="item.isTemp === 1 && item.status === 'TEMP_PENDING'" class="temp-warn">
          ⚠️ 临时离校，班主任需在 {{ fmtDt(item.tempDeadline) }} 前补批
        </view>
      </view>
    </view>
    <view v-if="hasMore" class="load-more" @click="loadMore">加载更多</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const tabs = [
  { label: '全部', value: '' },
  { label: '待审批', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
];
const activeTab = ref('');
const list = ref([]);
const loading = ref(true);
const pageNo = ref(1);
const pageSize = 10;
const hasMore = ref(false);

const load = async (reset = false) => {
  if (reset) { pageNo.value = 1; list.value = []; }
  loading.value = true;
  try {
    const params = `pageNo=${pageNo.value}&pageSize=${pageSize}&role=my${activeTab.value ? '&status=' + activeTab.value : ''}`;
    const data = await request({ url: `/leave/applications?${params}` });
    const records = data?.records || [];
    list.value = reset ? records : [...list.value, ...records];
    hasMore.value = list.value.length < (data?.total || 0);
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally { loading.value = false; }
};

const switchTab = (val) => { activeTab.value = val; load(true); };
const loadMore = () => { pageNo.value++; load(); };

const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
  DEPARTED: '已离校', RETURNED: '已返校', TEMP_PENDING: '临时待补批'
}[s] || s);
const statusClass = (s) => ({
  PENDING: 'badge-yellow', APPROVED: 'badge-green', REJECTED: 'badge-red',
  DEPARTED: 'badge-blue', TEMP_PENDING: 'badge-orange'
}[s] || 'badge-gray');

onMounted(() => load(true));
</script>

<style scoped>
.tab-bar { display: flex; background: #fff; border-bottom: 1rpx solid #e5e7eb; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: #6b7280; }
.tab-item.active { color: #1b5ea6; border-bottom: 4rpx solid #1b5ea6; font-weight: 600; }
.page { background: #f6f8fb; min-height: 100vh; }
.center { text-align: center; padding: 100rpx 0; color: #9ca3af; font-size: 28rpx; }
.list { padding: 20rpx 28rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.leave-no { font-size: 24rpx; color: #9ca3af; }
.status-badge { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 50rpx; }
.badge-yellow { background: #fef3c7; color: #d97706; }
.badge-green { background: #d1fae5; color: #059669; }
.badge-red { background: #fee2e2; color: #dc2626; }
.badge-blue { background: #dbeafe; color: #2563eb; }
.badge-orange { background: #ffedd5; color: #ea580c; }
.badge-gray { background: #f3f4f6; color: #6b7280; }
.row { display: flex; margin-bottom: 10rpx; }
.label { width: 80rpx; font-size: 24rpx; color: #6b7280; flex-shrink: 0; }
.value { font-size: 24rpx; color: #374151; flex: 1; }
.remark .value { color: #ef4444; }
.temp-warn { margin-top: 16rpx; padding: 16rpx; background: #fff7ed; border-radius: 8rpx; font-size: 24rpx; color: #ea580c; }
.load-more { text-align: center; padding: 24rpx; color: #1b5ea6; font-size: 28rpx; }
</style>
