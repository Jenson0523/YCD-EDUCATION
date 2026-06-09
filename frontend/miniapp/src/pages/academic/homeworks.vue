<template>
  <view class="page">
    <view class="header">
      <text class="title">作业列表</text>
    </view>

    <view v-if="loading" class="loading"><text>加载中...</text></view>

    <view v-else-if="homeworks.length === 0" class="empty"><text>暂无作业</text></view>

    <view v-else class="list">
      <view v-for="item in homeworks" :key="item.id" class="hw-card">
        <view class="hw-title">{{ item.title }}</view>
        <view class="hw-meta">
          <text>{{ item.assignedDate }} 布置</text>
          <text>截止 {{ item.dueDate }}</text>
        </view>
        <view v-if="item.content" class="hw-content">{{ item.content }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const homeworks = ref([]);
const loading = ref(false);

const load = async () => {
  loading.value = true;
  try {
    const classId = uni.getStorageSync('classId') || '';
    if (!classId) { homeworks.value = []; return; }
    const res = await request({ url: '/edu/homeworks', method: 'GET', data: { classId, pageNo: 1, pageSize: 20 } });
    homeworks.value = res?.records || [];
  } catch {
    homeworks.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(load);
</script>

<style scoped>
.page {
  padding: 0 0 40rpx;
}

.header {
  padding: 40rpx 32rpx 20rpx;
  background: #1b5ea6;
}

.title {
  color: #fff;
  font-size: 36rpx;
  font-weight: 700;
}

.loading, .empty {
  padding: 60rpx 32rpx;
  text-align: center;
  color: #9ca3af;
  font-size: 28rpx;
}

.list {
  padding: 20rpx 32rpx;
}

.hw-card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 12rpx;
  box-shadow: 0 1rpx 6rpx rgba(0,0,0,0.06);
}

.hw-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.hw-meta {
  margin-top: 10rpx;
  display: flex;
  gap: 20rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.hw-content {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: #374151;
  line-height: 1.6;
}
</style>
