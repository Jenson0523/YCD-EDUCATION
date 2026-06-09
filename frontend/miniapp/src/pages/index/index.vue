<template>
  <view class="page">
    <view class="hero">
      <text class="title">云辰盾</text>
      <text class="subtitle">家校共育共同体</text>
    </view>
    <view class="section grid">
      <view class="entry" @click="goHomeReport">
        <text class="entry-title">居家报备</text>
        <text class="entry-desc">作息、情绪、学习、外出</text>
      </view>
      <view class="entry" @click="goScores">
        <text class="entry-title">成绩查询</text>
        <text class="entry-desc">各科成绩、排名查看</text>
      </view>
      <view class="entry" @click="goHomeworks">
        <text class="entry-title">作业列表</text>
        <text class="entry-desc">当前作业与截止日期</text>
      </view>
      <view class="entry">
        <text class="entry-title">审批待办</text>
        <text class="entry-desc">请假、退费、流转事项</text>
      </view>
      <view class="entry">
        <text class="entry-title">成长档案</text>
        <text class="entry-desc">一人一档，全程沉淀</text>
      </view>
    </view>
  </view>
</template>

<script setup>
function goHomeReport() {
  uni.navigateTo({ url: '/pages/home-report/home-report' });
}
function goScores() {
  uni.navigateTo({ url: '/pages/academic/scores' });
}
function goHomeworks() {
  uni.navigateTo({ url: '/pages/academic/homeworks' });
}
</script>

<style scoped>
.hero {
  padding: 48rpx 32rpx 28rpx;
  background: #1b5ea6;
  color: #ffffff;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.entry {
  min-height: 144rpx;
  padding: 22rpx;
  border: 1rpx solid #d1d5db;
  border-radius: 12rpx;
}

.entry-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}

.entry-desc {
  display: block;
  margin-top: 10rpx;
  color: #4b5563;
  font-size: 24rpx;
}
</style>
