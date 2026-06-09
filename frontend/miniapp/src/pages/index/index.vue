<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <text class="title">云辰盾</text>
        <text class="welcome">{{ realName }}，你好</text>
      </view>
      <text class="subtitle">家校共育共同体</text>
    </view>

    <!-- 家长视图 -->
    <view v-if="isParent" class="section">
      <view class="section-title">常用功能</view>
      <view class="grid">
        <view class="entry" @click="nav('/pages/home-report/home-report')">
          <text class="entry-icon">📋</text>
          <text class="entry-title">居家报备</text>
          <text class="entry-desc">作息·情绪·学习·外出</text>
        </view>
        <view class="entry" @click="nav('/pages/my-reports/my-reports')">
          <text class="entry-icon">📬</text>
          <text class="entry-title">我的报备</text>
          <text class="entry-desc">历史记录与老师回复</text>
        </view>
        <view class="entry" @click="nav('/pages/academic/scores')">
          <text class="entry-icon">📊</text>
          <text class="entry-title">成绩查询</text>
          <text class="entry-desc">各科成绩排名</text>
        </view>
        <view class="entry" @click="nav('/pages/academic/homeworks')">
          <text class="entry-icon">📝</text>
          <text class="entry-title">作业列表</text>
          <text class="entry-desc">布置·截止日期</text>
        </view>
      </view>
    </view>

    <!-- 教师/班主任视图 -->
    <view v-else class="section">
      <view class="section-title">教师工作台</view>
      <view class="grid">
        <view class="entry" @click="nav('/pages/teacher/pending-reports')">
          <text class="entry-icon">🔔</text>
          <text class="entry-title">待跟进报备</text>
          <text class="entry-desc">查看并回复家长</text>
        </view>
        <view class="entry" @click="nav('/pages/academic/homeworks')">
          <text class="entry-icon">📝</text>
          <text class="entry-title">作业管理</text>
          <text class="entry-desc">布置·查看完成情况</text>
        </view>
        <view class="entry" @click="nav('/pages/academic/scores')">
          <text class="entry-icon">📊</text>
          <text class="entry-title">成绩录入</text>
          <text class="entry-desc">班级成绩管理</text>
        </view>
        <view class="entry">
          <text class="entry-icon">👤</text>
          <text class="entry-title">学生档案</text>
          <text class="entry-desc">查看班级学生</text>
        </view>
      </view>
    </view>

    <!-- 退出 -->
    <view class="logout-btn" @click="handleLogout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const realName = ref(uni.getStorageSync('ycd_realName') || '用户');
const roleCode = ref(uni.getStorageSync('ycd_roleCode') || '');
const isParent = ref(['PARENT', 'STUDENT'].includes(roleCode.value));

onMounted(() => {
  const token = uni.getStorageSync('ycd_token');
  if (!token) {
    uni.reLaunch({ url: '/pages/login/login' });
  }
});

const nav = (url) => uni.navigateTo({ url });

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('ycd_token');
        uni.removeStorageSync('ycd_userId');
        uni.removeStorageSync('ycd_realName');
        uni.removeStorageSync('ycd_roleCode');
        uni.reLaunch({ url: '/pages/login/login' });
      }
    }
  });
};
</script>

<style scoped>
.hero {
  background: #1b5ea6;
  padding: 48rpx 32rpx 36rpx;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.title {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
}

.welcome {
  font-size: 24rpx;
  color: rgba(255,255,255,0.8);
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255,255,255,0.65);
}

.section { padding: 28rpx 32rpx 0; }

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20rpx;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.entry {
  padding: 28rpx 24rpx;
  background: #fff;
  border: 1rpx solid #e5e7eb;
  border-radius: 16rpx;
}

.entry-icon { display: block; font-size: 44rpx; margin-bottom: 12rpx; }
.entry-title { display: block; font-size: 28rpx; font-weight: 700; color: #111827; }
.entry-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: #6b7280; }

.logout-btn {
  margin: 48rpx 32rpx;
  text-align: center;
  padding: 24rpx;
  background: #f3f4f6;
  border-radius: 12rpx;
  color: #ef4444;
  font-size: 28rpx;
}
</style>
