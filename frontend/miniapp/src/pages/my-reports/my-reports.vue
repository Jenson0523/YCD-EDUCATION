<template>
  <view class="page">
    <view v-if="loading" class="center">加载中…</view>
    <view v-else-if="list.length === 0" class="center">暂无报备记录</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card">
        <!-- 头部：日期 + 跟进状态 -->
        <view class="card-header">
          <text class="date">{{ item.reportDate }}</text>
          <view class="badge" :class="item.followStatus === 'FOLLOWED' ? 'badge-done' : 'badge-pending'">
            {{ item.followStatus === 'FOLLOWED' ? '已跟进' : '待跟进' }}
          </view>
        </view>

        <!-- 状态标签行 -->
        <view class="tag-row">
          <view class="tag">😴 {{ item.sleepStatus || '—' }}</view>
          <view class="tag">😊 {{ item.emotionStatus || '—' }}</view>
          <view class="tag">📖 {{ item.studyStatus || '—' }}</view>
        </view>

        <!-- 特殊情况 -->
        <view v-if="item.familySpecialSituation" class="detail-row">
          <text class="detail-label">特殊情况：</text>
          <text class="detail-text">{{ item.familySpecialSituation }}</text>
        </view>

        <!-- 外出报备 -->
        <view v-if="item.outgoingReport" class="detail-row">
          <text class="detail-label">外出报备：</text>
          <text class="detail-text">{{ item.outgoingReport }}</text>
        </view>

        <!-- 老师回复 -->
        <view v-if="item.followRemark" class="reply-box">
          <text class="reply-label">老师回复</text>
          <text class="reply-text">{{ item.followRemark }}</text>
          <text class="reply-time">{{ formatTime(item.followAt) }}</text>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="hasMore" class="load-more" @click="loadMore">加载更多</view>
      <view v-else class="no-more">已加载全部记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const list = ref([]);
const loading = ref(true);
const pageNo = ref(1);
const pageSize = 10;
const hasMore = ref(false);

const load = async (reset = false) => {
  if (reset) { pageNo.value = 1; list.value = []; }
  loading.value = true;
  try {
    const data = await request({
      url: `/fs/home-reports?pageNo=${pageNo.value}&pageSize=${pageSize}`
    });
    const records = data?.records || [];
    list.value = reset ? records : [...list.value, ...records];
    hasMore.value = list.value.length < (data?.total || 0);
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const loadMore = () => { pageNo.value++; load(); };

const formatTime = (ts) => {
  if (!ts) return '';
  return ts.slice(0, 16).replace('T', ' ');
};

onMounted(() => load(true));
</script>

<style scoped>
.page { padding: 24rpx 28rpx; background: #f6f8fb; min-height: 100vh; }

.center { text-align: center; padding: 100rpx 0; color: #9ca3af; font-size: 28rpx; }

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
}

.date { font-size: 30rpx; font-weight: 700; color: #111827; }

.badge {
  font-size: 22rpx;
  padding: 6rpx 18rpx;
  border-radius: 50rpx;
}
.badge-pending { background: #fef3c7; color: #d97706; }
.badge-done { background: #d1fae5; color: #059669; }

.tag-row { display: flex; gap: 16rpx; flex-wrap: wrap; margin-bottom: 18rpx; }

.tag {
  font-size: 24rpx;
  padding: 8rpx 18rpx;
  background: #eff6ff;
  color: #1b5ea6;
  border-radius: 8rpx;
}

.detail-row { display: flex; margin-bottom: 12rpx; }
.detail-label { font-size: 24rpx; color: #6b7280; flex-shrink: 0; width: 140rpx; }
.detail-text { font-size: 24rpx; color: #374151; }

.reply-box {
  margin-top: 20rpx;
  padding: 18rpx 20rpx;
  background: #f0f9ff;
  border-left: 6rpx solid #1b5ea6;
  border-radius: 0 12rpx 12rpx 0;
}
.reply-label { display: block; font-size: 22rpx; color: #1b5ea6; font-weight: 600; margin-bottom: 8rpx; }
.reply-text { display: block; font-size: 26rpx; color: #1e293b; line-height: 1.5; }
.reply-time { display: block; font-size: 22rpx; color: #9ca3af; margin-top: 8rpx; }

.load-more {
  text-align: center;
  padding: 24rpx;
  color: #1b5ea6;
  font-size: 28rpx;
}
.no-more { text-align: center; padding: 24rpx; color: #d1d5db; font-size: 24rpx; }
</style>
