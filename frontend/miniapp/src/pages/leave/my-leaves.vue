<template>
  <view class="page">
    <!-- Hero Header -->
    <view class="hero">
      <view class="hero-bg-orb"></view>
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @click="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <text class="hero-title">我的请假记录</text>
          <view class="new-btn" @click="navApply">
            <text class="new-icon">+</text>
          </view>
        </view>
      </view>

      <!-- Tab Bar -->
      <view class="tab-bar">
        <view
          v-for="tab in tabs" :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </view>
        <view class="tab-ink" :style="{ left: tabInkLeft + 'rpx' }"></view>
      </view>
    </view>

    <!-- 内容 -->
    <view class="content">
      <!-- Loading Skeleton -->
      <view v-if="loading" class="skeleton-area">
        <view class="sk-card" v-for="i in 3" :key="i">
          <view class="sk-row sk-w40"></view>
          <view class="sk-row sk-w70 sk-h20"></view>
          <view class="sk-row sk-w50 sk-h16"></view>
        </view>
      </view>

      <!-- Empty -->
      <view v-else-if="list.length === 0" class="empty">
        <image src="/static/empty-leave.png" class="empty-img" mode="aspectFit" />
        <text class="empty-title">暂无请假记录</text>
        <text class="empty-sub">{{ activeTab === 'PENDING' ? '目前没有待审批的请假申请' : '暂时没有相关记录' }}</text>
        <view class="empty-btn" @click="navApply">+ 发起请假申请</view>
      </view>

      <!-- 列表 -->
      <view v-else class="list">
        <view v-for="item in list" :key="item.id" class="leave-card">
          <!-- 状态指示条 -->
          <view class="status-line" :class="'sl-' + item.status"></view>

          <view class="card-main">
            <!-- 顶部 -->
            <view class="card-top">
              <view class="type-badge" :class="item.leaveType === 'SICK' ? 'type-sick' : 'type-personal'">
                {{ item.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}
                <text v-if="item.isTemp" class="temp-mark"> · ⚡临时</text>
              </view>
              <view class="status-badge" :class="'sb-' + item.status">
                {{ statusLabel(item.status) }}
              </view>
            </view>

            <!-- 假条编号 -->
            <text class="leave-no">{{ item.leaveNo }}</text>

            <!-- 时间行 -->
            <view class="time-row">
              <view class="time-item">
                <text class="time-label">离校</text>
                <text class="time-val">{{ fmtDt(item.leaveStart) }}</text>
              </view>
              <view class="time-arrow">→</view>
              <view class="time-item">
                <text class="time-label">返校</text>
                <text class="time-val">{{ fmtDt(item.leaveEnd) }}</text>
              </view>
            </view>

            <!-- 原因 -->
            <view class="reason-row">
              <text class="reason-label">原因：</text>
              <text class="reason-text">{{ item.reason }}</text>
            </view>

            <!-- 审批备注 -->
            <view v-if="item.approveRemark" class="remark-row">
              <text class="remark-icon">💬</text>
              <text class="remark-text">{{ item.approveRemark }}</text>
            </view>

            <!-- 临时补批提示 -->
            <view v-if="item.isTemp === 1 && item.status === 'TEMP_PENDING'" class="temp-alert">
              <text class="ta-icon">⚠️</text>
              <text class="ta-text">临时放行 · 班主任须在 {{ fmtDt(item.tempDeadline) }} 前补批</text>
            </view>
          </view>
        </view>

        <view v-if="hasMore" class="load-more" @click="loadMore">
          <text class="load-more-text">加载更多</text>
          <text class="load-more-icon">↓</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
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

const tabInkLeft = computed(() => {
  const idx = tabs.findIndex(t => t.value === activeTab.value);
  return (idx >= 0 ? idx : 0) * (750 / tabs.length);
});

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
    uni.showToast({ title: e.message || '加载失败', icon: 'none' });
  } finally { loading.value = false; }
};

const switchTab = (val) => { activeTab.value = val; load(true); };
const loadMore = () => { pageNo.value++; load(); };
const navApply = () => uni.navigateTo({ url: '/pages/leave/apply' });

const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
  DEPARTED: '已离校', RETURNED: '已返校', TEMP_PENDING: '临时待补批'
}[s] || s);

onMounted(() => load(true));
</script>

<style scoped>
.page { background: #F0F4FA; min-height: 100vh; }

/* Hero */
.hero {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #06133D 0%, #112A80 50%, #1947C8 100%);
  padding: 56rpx 32rpx 0;
}
.hero-bg-orb {
  position: absolute; width: 500rpx; height: 500rpx;
  background: rgba(21,200,224,0.15); filter: blur(80rpx);
  border-radius: 50%; top: -150rpx; right: -150rpx;
}
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; margin-bottom: 28rpx; }
.back-btn { width: 64rpx; height: 64rpx; background: rgba(255,255,255,0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 52rpx; color: #fff; }
.hero-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 700; color: #fff; }
.new-btn { width: 64rpx; height: 64rpx; background: rgba(232,192,104,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 1rpx solid rgba(232,192,104,0.4); }
.new-icon { font-size: 44rpx; color: #E8C068; line-height: 1; }

/* Tab */
.tab-bar { position: relative; display: flex; background: rgba(255,255,255,0.06); border-radius: 16rpx 16rpx 0 0; overflow: hidden; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: rgba(255,255,255,0.5); position: relative; z-index: 1; transition: color 0.2s; }
.tab-item.active { color: #fff; font-weight: 600; }
.tab-ink {
  position: absolute; bottom: 0; width: calc(100% / 4); height: 4rpx;
  background: linear-gradient(90deg, #E8C068, #2B7FFF);
  border-radius: 2rpx; transition: left 0.25s;
}

/* Content */
.content { padding: 24rpx 28rpx; }

/* Skeleton */
.skeleton-area {}
.sk-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.sk-row { background: #EEF2F8; border-radius: 6rpx; margin-bottom: 14rpx; height: 24rpx; }
.sk-w40 { width: 40%; }
.sk-w70 { width: 70%; }
.sk-w50 { width: 50%; }
.sk-h20 { height: 20rpx; }
.sk-h16 { height: 16rpx; }

/* Empty */
.empty { text-align: center; padding: 80rpx 0; }
.empty-img { width: 200rpx; height: 200rpx; opacity: 0.6; margin-bottom: 24rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 600; color: #374151; }
.empty-sub { display: block; font-size: 24rpx; color: #9CA3AF; margin-top: 8rpx; }
.empty-btn { display: inline-block; margin-top: 32rpx; padding: 16rpx 48rpx; background: linear-gradient(135deg, #1947C8, #2B7FFF); color: #fff; border-radius: 50rpx; font-size: 26rpx; font-weight: 600; }

/* Leave Card */
.leave-card { background: #fff; border-radius: 20rpx; margin-bottom: 20rpx; overflow: hidden; display: flex; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05); }
.status-line { width: 6rpx; flex-shrink: 0; }
.sl-PENDING { background: linear-gradient(180deg, #F59F00, #FBBF24); }
.sl-APPROVED { background: linear-gradient(180deg, #059669, #10B981); }
.sl-REJECTED { background: linear-gradient(180deg, #DC2626, #EF4444); }
.sl-DEPARTED { background: linear-gradient(180deg, #2563EB, #60A5FA); }
.sl-RETURNED { background: linear-gradient(180deg, #6D28D9, #A78BFA); }
.sl-TEMP_PENDING { background: linear-gradient(180deg, #EA580C, #FB923C); }

.card-main { flex: 1; padding: 24rpx; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10rpx; }
.type-badge { font-size: 24rpx; color: #374151; font-weight: 500; }
.temp-mark { color: #EA580C; }
.status-badge { font-size: 20rpx; padding: 6rpx 18rpx; border-radius: 30rpx; font-weight: 600; }
.sb-PENDING { background: #FEF9C3; color: #CA8A04; }
.sb-APPROVED { background: #D1FAE5; color: #059669; }
.sb-REJECTED { background: #FEE2E2; color: #DC2626; }
.sb-DEPARTED { background: #DBEAFE; color: #2563EB; }
.sb-RETURNED { background: #EDE9FE; color: #6D28D9; }
.sb-TEMP_PENDING { background: #FFEDD5; color: #EA580C; }

.leave-no { display: block; font-size: 20rpx; color: #CBD5E1; margin-bottom: 16rpx; }

.time-row { display: flex; align-items: center; gap: 12rpx; background: #F8FAFC; border-radius: 12rpx; padding: 16rpx; margin-bottom: 14rpx; }
.time-item { flex: 1; }
.time-label { display: block; font-size: 20rpx; color: #94A3B8; }
.time-val { display: block; font-size: 24rpx; color: #1E293B; font-weight: 600; margin-top: 4rpx; }
.time-arrow { font-size: 26rpx; color: #CBD5E1; }

.reason-row { display: flex; align-items: flex-start; gap: 8rpx; margin-bottom: 10rpx; }
.reason-label { font-size: 22rpx; color: #94A3B8; flex-shrink: 0; }
.reason-text { font-size: 24rpx; color: #374151; flex: 1; }

.remark-row { display: flex; align-items: flex-start; gap: 10rpx; padding: 14rpx; background: #FFF7F7; border-radius: 10rpx; margin-top: 10rpx; }
.remark-icon { font-size: 24rpx; flex-shrink: 0; }
.remark-text { font-size: 22rpx; color: #EF4444; flex: 1; }

.temp-alert { display: flex; align-items: center; gap: 10rpx; padding: 14rpx; background: #FFF7ED; border-radius: 10rpx; margin-top: 10rpx; }
.ta-icon { font-size: 24rpx; flex-shrink: 0; }
.ta-text { font-size: 22rpx; color: #EA580C; flex: 1; }

.load-more { display: flex; align-items: center; justify-content: center; gap: 12rpx; padding: 24rpx; }
.load-more-text { font-size: 26rpx; color: #2B7FFF; }
.load-more-icon { font-size: 28rpx; color: #2B7FFF; }
</style>
