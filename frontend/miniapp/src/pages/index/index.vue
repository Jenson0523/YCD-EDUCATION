<template>
  <view class="page">
    <!-- 顶部渐变头部 -->
    <view class="hero">
      <view class="hero-decor"></view>
      <view class="hero-content">
        <view class="hero-row">
          <view>
            <text class="greeting">{{ greeting }}，{{ realName }}</text>
            <text class="role-chip">{{ roleName }}</text>
          </view>
          <view class="avatar">{{ realName.charAt(0) }}</view>
        </view>
        <view class="brand-line">
          <text class="brand">云辰盾</text>
          <text class="brand-sub">智慧家校共育 · 安全离校</text>
        </view>
      </view>
    </view>

    <!-- 功能宫格 -->
    <view class="grid-wrap">
      <view class="section-label">
        <text class="bar"></text>
        <text>{{ panelTitle }}</text>
      </view>
      <view class="grid">
        <view
          v-for="(it, idx) in entries"
          :key="idx"
          class="entry"
          @click="nav(it.url)"
        >
          <view class="entry-icon" :style="{ background: it.grad }">
            <text class="emoji">{{ it.icon }}</text>
          </view>
          <text class="entry-title">{{ it.title }}</text>
          <text class="entry-desc">{{ it.desc }}</text>
        </view>
      </view>
    </view>

    <view class="logout-btn" @click="handleLogout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

const realName = ref(uni.getStorageSync('ycd_realName') || '用户');
const roleCode = ref(uni.getStorageSync('ycd_roleCode') || '');

const greeting = (() => {
  const h = new Date().getHours();
  if (h < 6) return '凌晨好';
  if (h < 12) return '上午好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
})();

const roleName = computed(() => ({
  ADMIN: '系统管理员', PRINCIPAL: '校长', ACADEMIC: '教务',
  HEAD_TEACHER: '班主任', TEACHER: '任课教师', GATE: '门卫',
  PARENT: '家长', STUDENT: '学生'
}[roleCode.value] || '用户'));

const G_BLUE = 'linear-gradient(135deg,#2b6cff,#15c8e0)';
const G_GREEN = 'linear-gradient(135deg,#0bbf8a,#5be3b0)';
const G_ORANGE = 'linear-gradient(135deg,#ff7a3c,#ffb347)';
const G_PURPLE = 'linear-gradient(135deg,#7c5cff,#b18bff)';

const MENUS = {
  PARENT: {
    title: '家长服务',
    items: [
      { icon: '📝', title: '申请请假', desc: '事假·病假', url: '/pages/leave/apply', grad: G_BLUE },
      { icon: '📋', title: '我的请假', desc: '审批进度', url: '/pages/leave/my-leaves', grad: G_GREEN },
      { icon: '🏠', title: '居家报备', desc: '作息·情绪', url: '/pages/home-report/home-report', grad: G_ORANGE },
      { icon: '📬', title: '我的报备', desc: '老师回复', url: '/pages/my-reports/my-reports', grad: G_PURPLE },
    ]
  },
  HEAD_TEACHER: {
    title: '班主任工作台',
    items: [
      { icon: '✅', title: '请假审批', desc: '待办·补批', url: '/pages/leave/approve', grad: G_BLUE },
      { icon: '⚡', title: '临时补批', desc: '24h处理', url: '/pages/leave/approve?tab=TEMP_PENDING', grad: G_ORANGE },
      { icon: '🏠', title: '居家报备', desc: '班级跟进', url: '/pages/home-report/home-report', grad: G_GREEN },
      { icon: '📊', title: '审批记录', desc: '历史查询', url: '/pages/leave/approve?tab=APPROVED', grad: G_PURPLE },
    ]
  },
  GATE: {
    title: '门卫工作台',
    items: [
      { icon: '🛂', title: '人脸核验', desc: '双重校验', url: '/pages/gate/verify', grad: G_BLUE },
      { icon: '📋', title: '今日假条', desc: '有效列表', url: '/pages/gate/today-leaves', grad: G_GREEN },
      { icon: '⚡', title: '临时放行', desc: '紧急登记', url: '/pages/gate/temp-depart', grad: G_ORANGE },
      { icon: '⚠️', title: '异常记录', desc: '拦截日志', url: '/pages/gate/today-leaves', grad: G_PURPLE },
    ]
  },
  TEACHER: {
    title: '教师工作台',
    items: [
      { icon: '📝', title: '代学生请假', desc: '代为发起', url: '/pages/leave/apply', grad: G_BLUE },
      { icon: '📋', title: '我的申请', desc: '提交记录', url: '/pages/leave/my-leaves', grad: G_GREEN },
      { icon: '🏠', title: '班级报备', desc: '居家跟进', url: '/pages/home-report/home-report', grad: G_ORANGE },
      { icon: '📚', title: '教务管理', desc: '作业成绩', url: '/pages/academic/homeworks', grad: G_PURPLE },
    ]
  },
};

const current = computed(() => {
  if (['PARENT', 'STUDENT'].includes(roleCode.value)) return MENUS.PARENT;
  if (roleCode.value === 'HEAD_TEACHER') return MENUS.HEAD_TEACHER;
  if (roleCode.value === 'GATE') return MENUS.GATE;
  return MENUS.TEACHER;
});
const panelTitle = computed(() => current.value.title);
const entries = computed(() => current.value.items);

onMounted(() => {
  if (!uni.getStorageSync('ycd_token')) uni.reLaunch({ url: '/pages/login/login' });
});

const nav = (url) => uni.navigateTo({ url });

const handleLogout = () => {
  uni.showModal({
    title: '提示', content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        ['ycd_token', 'ycd_userId', 'ycd_realName', 'ycd_roleCode'].forEach(k => uni.removeStorageSync(k));
        uni.reLaunch({ url: '/pages/login/login' });
      }
    }
  });
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef2f9; }

/* 顶部渐变头部 */
.hero {
  position: relative;
  background: linear-gradient(150deg, #0b2a6b 0%, #1746b5 55%, #2b6cff 100%);
  padding: 80rpx 40rpx 90rpx;
  border-radius: 0 0 40rpx 40rpx;
  overflow: hidden;
}
.hero-decor {
  position: absolute; width: 380rpx; height: 380rpx; border-radius: 50%;
  background: rgba(21, 200, 224, 0.25); filter: blur(40rpx);
  top: -120rpx; right: -100rpx;
}
.hero-content { position: relative; }
.hero-row { display: flex; justify-content: space-between; align-items: flex-start; }
.greeting { display: block; font-size: 34rpx; font-weight: 700; color: #fff; }
.role-chip {
  display: inline-block; margin-top: 14rpx; padding: 6rpx 22rpx;
  background: rgba(255, 255, 255, 0.18); border: 1rpx solid rgba(255,255,255,0.25);
  border-radius: 50rpx; font-size: 22rpx; color: #fff;
}
.avatar {
  width: 84rpx; height: 84rpx; border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.2); border: 1rpx solid rgba(255,255,255,0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 38rpx; font-weight: 700; color: #fff;
}
.brand-line { margin-top: 44rpx; }
.brand { font-size: 48rpx; font-weight: 800; color: #fff; letter-spacing: 6rpx; }
.brand-sub { display: block; margin-top: 8rpx; font-size: 22rpx; color: rgba(255,255,255,0.6); }

/* 宫格 */
.grid-wrap { padding: 0 32rpx; margin-top: -50rpx; position: relative; }
.section-label {
  display: flex; align-items: center; gap: 14rpx;
  margin: 36rpx 8rpx 24rpx; font-size: 30rpx; font-weight: 700; color: #1e293b;
}
.bar { width: 8rpx; height: 30rpx; border-radius: 4rpx; background: linear-gradient(#2b6cff, #15c8e0); }

.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 22rpx; }
.entry {
  background: #fff; border-radius: 26rpx; padding: 32rpx 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 40, 100, 0.07);
}
.entry-icon {
  width: 92rpx; height: 92rpx; border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 22rpx; box-shadow: 0 8rpx 20rpx rgba(43, 108, 255, 0.25);
}
.emoji { font-size: 46rpx; }
.entry-title { display: block; font-size: 30rpx; font-weight: 700; color: #111827; }
.entry-desc { display: block; margin-top: 8rpx; font-size: 22rpx; color: #94a3b8; }

.logout-btn {
  margin: 56rpx 32rpx; text-align: center; padding: 26rpx;
  background: #fff; border-radius: 18rpx; color: #ef4444; font-size: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 40, 100, 0.05);
}
</style>
