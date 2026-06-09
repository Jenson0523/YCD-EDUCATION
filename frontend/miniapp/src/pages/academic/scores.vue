<template>
  <view class="page">
    <view class="header">
      <text class="title">我的成绩</text>
    </view>
    <view class="filter">
      <picker mode="selector" :range="semesterOptions" @change="onSemesterChange">
        <view class="picker-btn">{{ currentSemester || '选择学期' }}</view>
      </picker>
    </view>

    <view v-if="loading" class="loading"><text>加载中...</text></view>

    <view v-else-if="scores.length === 0" class="empty"><text>暂无成绩记录</text></view>

    <view v-else class="list">
      <view v-for="item in scores" :key="item.id" class="score-card">
        <view class="score-row">
          <text class="subject">{{ subjectMap[item.subjectId] || '学科' + item.subjectId }}</text>
          <text class="score-value">{{ item.score ?? '-' }}</text>
        </view>
        <view class="score-meta">
          <text>{{ examTypeLabel[item.examType] || item.examType }}</text>
          <text v-if="item.rankInClass">班级第 {{ item.rankInClass }} 名</text>
          <text v-if="item.rankInGrade">年级第 {{ item.rankInGrade }} 名</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const semesterOptions = ['2024-1', '2024-2', '2025-1', '2025-2'];
const currentSemester = ref('2024-2');
const scores = ref([]);
const loading = ref(false);
const subjectMap = ref({});

const examTypeLabel = { MONTHLY: '月考', MID: '期中', FINAL: '期末', QUIZ: '随堂' };

const onSemesterChange = (e) => {
  currentSemester.value = semesterOptions[e.detail.value];
  load();
};

const load = async () => {
  loading.value = true;
  try {
    const studentId = uni.getStorageSync('studentId') || '';
    if (!studentId) { scores.value = []; return; }
    const res = await request({ url: '/edu/scores', method: 'GET', data: { studentId, semester: currentSemester.value, pageNo: 1, pageSize: 50 } });
    scores.value = res?.records || [];
  } catch {
    scores.value = [];
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

.filter {
  padding: 20rpx 32rpx;
}

.picker-btn {
  display: inline-block;
  padding: 12rpx 28rpx;
  border: 1rpx solid #d1d5db;
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #374151;
}

.loading, .empty {
  padding: 60rpx 32rpx;
  text-align: center;
  color: #9ca3af;
  font-size: 28rpx;
}

.list {
  padding: 0 32rpx;
}

.score-card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 12rpx;
  box-shadow: 0 1rpx 6rpx rgba(0,0,0,0.06);
}

.score-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subject {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.score-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #1b5ea6;
}

.score-meta {
  margin-top: 12rpx;
  display: flex;
  gap: 20rpx;
  font-size: 24rpx;
  color: #6b7280;
}
</style>
