<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <text class="title">云辰盾</text>
        <text class="welcome">{{ realName }}，你好</text>
      </view>
      <text class="subtitle">家校共育共同体 · 智慧离校管理</text>
    </view>

    <!-- 家长视图 -->
    <view v-if="isParent" class="section">
      <view class="section-title">常用功能</view>
      <view class="grid">
        <view class="entry" @click="nav('/pages/leave/apply')">
          <text class="entry-icon">📝</text>
          <text class="entry-title">申请请假</text>
          <text class="entry-desc">事假·病假·提交申请</text>
        </view>
        <view class="entry" @click="nav('/pages/leave/my-leaves')">
          <text class="entry-icon">📋</text>
          <text class="entry-title">我的请假</text>
          <text class="entry-desc">查看审批进度</text>
        </view>
        <view class="entry" @click="nav('/pages/home-report/home-report')">
          <text class="entry-icon">🏠</text>
          <text class="entry-title">居家报备</text>
          <text class="entry-desc">作息·情绪·学习</text>
        </view>
        <view class="entry" @click="nav('/pages/my-reports/my-reports')">
          <text class="entry-icon">📬</text>
          <text class="entry-title">我的报备</text>
          <text class="entry-desc">历史记录与老师回复</text>
        </view>
      </view>
    </view>

    <!-- 班主任视图 -->
    <view v-else-if="isHeadTeacher" class="section">
      <view class="section-title">班主任工作台</view>
      <view class="grid">
        <view class="entry accent" @click="nav('/pages/leave/approve')">
          <text class="entry-icon">✅</text>
          <text class="entry-title">请假审批</text>
          <text class="entry-desc">待审批·临时补批</text>
        </view>
        <view class="entry" @click="nav('/pages/leave/approve?tab=TEMP_PENDING')">
          <text class="entry-icon">⚡</text>
          <text class="entry-title">临时补批</text>
          <text class="entry-desc">24h内处理</text>
        </view>
        <view class="entry" @click="nav('/pages/home-report/home-report')">
          <text class="entry-icon">🏠</text>
          <text class="entry-title">查看报备</text>
          <text class="entry-desc">班级居家报备</text>
        </view>
        <view class="entry" @click="nav('/pages/leave/approve?tab=APPROVED')">
          <text class="entry-icon">📊</text>
          <text class="entry-title">已处理记录</text>
          <text class="entry-desc">审批历史</text>
        </view>
      </view>
    </view>

    <!-- 门卫视图 -->
    <view v-else-if="isGate" class="section">
      <view class="section-title">门卫工作台</view>
      <view class="grid">
        <view class="entry accent-green" @click="nav('/pages/gate/verify')">
          <text class="entry-icon">🔍</text>
          <text class="entry-title">人脸核验</text>
          <text class="entry-desc">双重校验放行</text>
        </view>
        <view class="entry" @click="nav('/pages/gate/today-leaves')">
          <text class="entry-icon">📋</text>
          <text class="entry-title">今日假条</text>
          <text class="entry-desc">有效假条列表</text>
        </view>
        <view class="entry accent-orange" @click="nav('/pages/gate/temp-depart')">
          <text class="entry-icon">⚡</text>
          <text class="entry-title">临时放行</text>
          <text class="entry-desc">紧急情况先放行</text>
        </view>
        <view class="entry" @click="nav('/pages/gate/today-leaves')">
          <text class="entry-icon">⚠️</text>
          <text class="entry-title">异常记录</text>
          <text class="entry-desc">今日拦截日志</text>
        </view>
      </view>
    </view>

    <!-- 通用教师视图 -->
    <view v-else class="section">
      <view class="section-title">教师工作台</view>
      <view class="grid">
        <view class="entry" @click="nav('/pages/leave/apply')">
          <text class="entry-icon">📝</text>
          <text class="entry-title">代学生请假</text>
          <text class="entry-desc">代为发起请假申请</text>
        </view>
        <view class="entry" @click="nav('/pages/leave/my-leaves')">
          <text class="entry-icon">📋</text>
          <text class="entry-title">我的申请</text>
          <text class="entry-desc">查看提交记录</text>
        </view>
        <view class="entry" @click="nav('/pages/home-report/home-report')">
          <text class="entry-icon">🏠</text>
          <text class="entry-title">班级报备</text>
          <text class="entry-desc">居家状态跟进</text>
        </view>
        <view class="entry">
          <text class="entry-icon">📚</text>
          <text class="entry-title">教务管理</text>
          <text class="entry-desc">作业·成绩</text>
        </view>
      </view>
    </view>

    <view class="logout-btn" @click="handleLogout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const realName = ref(uni.getStorageSync('ycd_realName') || '用户');
const roleCode = ref(uni.getStorageSync('ycd_roleCode') || '');

const isParent = ref(['PARENT', 'STUDENT'].includes(roleCode.value));
const isHeadTeacher = ref(roleCode.value === 'HEAD_TEACHER');
const isGate = ref(roleCode.value === 'GATE');

onMounted(() => {
  const token = uni.getStorageSync('ycd_token');
  if (!token) uni.reLaunch({ url: '/pages/login/login' });
});

const nav = (url) => uni.navigateTo({ url });

const handleLogout = () => {
  uni.showModal({
    title: '提示', content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        ['ycd_token','ycd_userId','ycd_realName','ycd_roleCode'].forEach(k => uni.removeStorageSync(k));
        uni.reLaunch({ url: '/pages/login/login' });
      }
    }
  });
};
</script>

<style scoped>
.hero { background: #1b5ea6; padding: 48rpx 32rpx 36rpx; }
.hero-top { display: flex; justify-content: space-between; align-items: flex-end; }
.title { font-size: 44rpx; font-weight: 700; color: #fff; }
.welcome { font-size: 24rpx; color: rgba(255,255,255,0.8); }
.subtitle { display: block; margin-top: 8rpx; font-size: 22rpx; color: rgba(255,255,255,0.65); }
.section { padding: 28rpx 32rpx 0; }
.section-title { font-size: 28rpx; font-weight: 600; color: #374151; margin-bottom: 20rpx; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20rpx; }
.entry { padding: 28rpx 24rpx; background: #fff; border: 1rpx solid #e5e7eb; border-radius: 16rpx; }
.entry.accent { border-color: #1b5ea6; background: #eff6ff; }
.entry.accent-green { border-color: #059669; background: #ecfdf5; }
.entry.accent-orange { border-color: #ea580c; background: #fff7ed; }
.entry-icon { display: block; font-size: 44rpx; margin-bottom: 12rpx; }
.entry-title { display: block; font-size: 28rpx; font-weight: 700; color: #111827; }
.entry-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: #6b7280; }
.logout-btn { margin: 48rpx 32rpx; text-align: center; padding: 24rpx; background: #f3f4f6; border-radius: 12rpx; color: #ef4444; font-size: 28rpx; }
</style>
