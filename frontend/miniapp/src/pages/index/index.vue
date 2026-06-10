<template>
  <view class="page">
    <!-- 背景层 -->
    <view class="bg-layer">
      <view class="bg-grid"></view>
      <view class="bg-glow bg-glow-1"></view>
      <view class="bg-glow bg-glow-2"></view>
    </view>

    <scroll-view scroll-y class="scroll">
      <!-- 顶部状态栏占位 -->
      <view :style="{ height: statusBarH + 'px' }"></view>

      <!-- 头部 -->
      <view class="header">
        <view class="header-row">
          <view class="user-block">
            <text class="greeting">{{ greeting }}</text>
            <text class="username">{{ realName }}</text>
            <view class="role-chip">
              <view class="role-dot"></view>
              <text class="role-text">{{ roleName }}</text>
            </view>
          </view>
          <view class="avatar">
            <text class="avatar-text">{{ realName.charAt(0) }}</text>
            <view class="avatar-ring"></view>
          </view>
        </view>

        <!-- 品牌行 -->
        <view class="brand-block">
          <view class="brand-main">
            <text class="brand-zh">云辰盾</text>
            <view class="brand-badge">YCD</view>
          </view>
          <text class="brand-en">YUNCHENDUN · SMART CAMPUS</text>
        </view>

        <!-- 数据概览条 -->
        <view class="stat-strip">
          <view class="stat-cell">
            <text class="stat-num">{{ stats.today }}</text>
            <text class="stat-label">今日待办</text>
          </view>
          <view class="stat-sep"></view>
          <view class="stat-cell">
            <text class="stat-num">{{ stats.unread }}</text>
            <text class="stat-label">未读消息</text>
          </view>
          <view class="stat-sep"></view>
          <view class="stat-cell">
            <text class="stat-num accent">{{ nowTime }}</text>
            <text class="stat-label">{{ todayStr }}</text>
          </view>
        </view>
      </view>

      <!-- 功能区 -->
      <view class="panel">
        <view class="panel-head">
          <view class="panel-bar"></view>
          <text class="panel-title">{{ panelTitle }}</text>
          <text class="panel-count">{{ entries.length }} 项</text>
        </view>

        <view class="grid">
          <view
            v-for="(it, idx) in entries"
            :key="idx"
            class="cell"
            :hover-class="'cell-hover'"
            @click="nav(it.url)"
          >
            <view class="cell-glow" :style="{ background: it.glow }"></view>
            <view class="cell-icon">
              <text class="cell-emoji">{{ it.icon }}</text>
            </view>
            <view class="cell-body">
              <text class="cell-title">{{ it.title }}</text>
              <text class="cell-desc">{{ it.desc }}</text>
            </view>
            <view class="cell-arrow">›</view>
          </view>
        </view>
      </view>

      <!-- 底部 -->
      <view class="footer">
        <view class="logout" hover-class="logout-hover" @click="handleLogout">
          <text class="logout-text">退出登录</text>
        </view>
        <text class="copyright">Powered by 云辰盾 · v1.0</text>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { request } from '../../api/request';

const statusBarH = ref(20);
const realName = ref(uni.getStorageSync('ycd_realName') || '用户');
const roleCode = ref(uni.getStorageSync('ycd_roleCode') || '');
const stats = ref({ today: 0, unread: 0 });
const nowTime = ref('');
const todayStr = ref('');

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

// 荧光色（青/蓝量子色）
const GLOW_CYAN = 'radial-gradient(circle, rgba(0,229,255,0.5), transparent 70%)';
const GLOW_BLUE = 'radial-gradient(circle, rgba(43,127,255,0.5), transparent 70%)';
const GLOW_GREEN = 'radial-gradient(circle, rgba(0,230,150,0.5), transparent 70%)';
const GLOW_GOLD = 'radial-gradient(circle, rgba(232,192,104,0.5), transparent 70%)';

const MENUS = {
  PARENT: {
    title: '家长服务台',
    items: [
      { icon: '📝', title: '申请请假', desc: '事假 · 病假', url: '/pages/leave/apply', glow: GLOW_CYAN },
      { icon: '📋', title: '我的请假', desc: '审批进度', url: '/pages/leave/my-leaves', glow: GLOW_BLUE },
      { icon: '🏠', title: '居家报备', desc: '作息 · 情绪', url: '/pages/home-report/home-report', glow: GLOW_GREEN },
      { icon: '📬', title: '我的报备', desc: '老师回复', url: '/pages/my-reports/my-reports', glow: GLOW_GOLD },
    ]
  },
  STUDENT: {
    title: '学生服务台',
    items: [
      { icon: '📋', title: '我的请假', desc: '老师家长代申请', url: '/pages/leave/my-leaves', glow: GLOW_CYAN },
      { icon: '📬', title: '我的报备', desc: '查看记录', url: '/pages/my-reports/my-reports', glow: GLOW_BLUE },
      { icon: '📊', title: '我的成绩', desc: '成绩查询', url: '/pages/academic/scores', glow: GLOW_GREEN },
      { icon: '📚', title: '作业列表', desc: '待完成', url: '/pages/academic/homeworks', glow: GLOW_GOLD },
    ]
  },
  HEAD_TEACHER: {
    title: '班主任工作台',
    items: [
      { icon: '✅', title: '请假审批', desc: '待办 · 签字', url: '/pages/leave/approve', glow: GLOW_CYAN },
      { icon: '📝', title: '代学生请假', desc: '代为发起', url: '/pages/leave/apply', glow: GLOW_BLUE },
      { icon: '⚡', title: '临时补批', desc: '24h 处理', url: '/pages/leave/approve?tab=TEMP_PENDING', glow: GLOW_GOLD },
      { icon: '📊', title: '审批记录', desc: '历史查询', url: '/pages/leave/approve?tab=APPROVED', glow: GLOW_GREEN },
    ]
  },
  GATE: {
    title: '门卫核验台',
    items: [
      { icon: '🛂', title: '刷脸核验', desc: 'AI 自动识别', url: '/pages/gate/verify', glow: GLOW_CYAN },
      { icon: '📋', title: '今日假条', desc: '有效列表', url: '/pages/gate/today-leaves', glow: GLOW_BLUE },
      { icon: '⚡', title: '临时放行', desc: '紧急登记', url: '/pages/gate/temp-depart', glow: GLOW_GOLD },
      { icon: '⚠️', title: '异常记录', desc: '拦截日志', url: '/pages/gate/today-leaves', glow: GLOW_GREEN },
    ]
  },
  TEACHER: {
    title: '教师工作台',
    items: [
      { icon: '📝', title: '代学生请假', desc: '代为发起', url: '/pages/leave/apply', glow: GLOW_CYAN },
      { icon: '📋', title: '我的申请', desc: '提交记录', url: '/pages/leave/my-leaves', glow: GLOW_BLUE },
      { icon: '🏠', title: '班级报备', desc: '居家跟进', url: '/pages/home-report/home-report', glow: GLOW_GREEN },
      { icon: '📚', title: '教务管理', desc: '作业成绩', url: '/pages/academic/homeworks', glow: GLOW_GOLD },
    ]
  },
};

const current = computed(() => {
  if (roleCode.value === 'STUDENT') return MENUS.STUDENT;
  if (roleCode.value === 'PARENT') return MENUS.PARENT;
  if (roleCode.value === 'HEAD_TEACHER') return MENUS.HEAD_TEACHER;
  if (roleCode.value === 'GATE') return MENUS.GATE;
  return MENUS.TEACHER;
});
const panelTitle = computed(() => current.value.title);
const entries = computed(() => current.value.items);

const updateClock = () => {
  const d = new Date();
  nowTime.value = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
  todayStr.value = `${d.getMonth() + 1}/${d.getDate()} 周${week}`;
};

const loadStats = async () => {
  try {
    const r = await request({ url: '/sys/messages/unread-count' });
    stats.value.unread = r?.count || 0;
  } catch {}
  // 待办：班主任看待审批数，门卫看今日假条数
  try {
    if (roleCode.value === 'HEAD_TEACHER') {
      const d = await request({ url: '/leave/applications?status=PENDING&pageSize=1' });
      stats.value.today = d?.total || 0;
    } else if (roleCode.value === 'GATE') {
      const d = await request({ url: '/leave/applications/today-valid' });
      stats.value.today = (d || []).length;
    } else {
      const d = await request({ url: '/leave/applications?role=my&pageSize=1' });
      stats.value.today = d?.total || 0;
    }
  } catch {}
};

onMounted(() => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
  if (!uni.getStorageSync('ycd_token')) { uni.reLaunch({ url: '/pages/login/login' }); return; }
  updateClock();
  setInterval(updateClock, 30000);
  loadStats();
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
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }

/* ── 背景层 ── */
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx);
  background-size: 48rpx 48rpx;
  mask-image: linear-gradient(180deg, #000 0%, transparent 60%);
}
.bg-glow { position: absolute; border-radius: 50%; filter: blur(80rpx); }
.bg-glow-1 { width: 520rpx; height: 520rpx; background: rgba(0,229,255,0.18); top: -160rpx; right: -160rpx; }
.bg-glow-2 { width: 460rpx; height: 460rpx; background: rgba(43,127,255,0.16); top: 180rpx; left: -180rpx; }

.scroll { position: relative; z-index: 1; height: 100vh; }

/* ── 头部 ── */
.header { padding: 24rpx 40rpx 0; }
.header-row { display: flex; justify-content: space-between; align-items: flex-start; }
.user-block { display: flex; flex-direction: column; }
.greeting { font-size: 24rpx; color: rgba(255,255,255,0.45); letter-spacing: 2rpx; }
.username { font-size: 44rpx; font-weight: 800; color: #fff; margin-top: 6rpx; letter-spacing: 1rpx; }
.role-chip { display: inline-flex; align-items: center; gap: 10rpx; margin-top: 16rpx; padding: 6rpx 18rpx;
  background: rgba(0,229,255,0.08); border: 1rpx solid rgba(0,229,255,0.3); border-radius: 8rpx; align-self: flex-start; }
.role-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: #00E5FF; box-shadow: 0 0 10rpx #00E5FF; }
.role-text { font-size: 22rpx; color: #00E5FF; letter-spacing: 1rpx; }

.avatar { position: relative; width: 96rpx; height: 96rpx; border-radius: 16rpx;
  background: linear-gradient(135deg, rgba(43,127,255,0.3), rgba(0,229,255,0.2));
  border: 1rpx solid rgba(0,229,255,0.4);
  display: flex; align-items: center; justify-content: center; }
.avatar-text { font-size: 44rpx; font-weight: 800; color: #fff; }
.avatar-ring { position: absolute; inset: -6rpx; border-radius: 18rpx; border: 1rpx solid rgba(0,229,255,0.25); }

/* 品牌 */
.brand-block { margin-top: 48rpx; }
.brand-main { display: flex; align-items: center; gap: 18rpx; }
.brand-zh { font-size: 56rpx; font-weight: 800; color: #fff; letter-spacing: 8rpx; }
.brand-badge { padding: 4rpx 16rpx; background: rgba(0,229,255,0.12); border: 1rpx solid rgba(0,229,255,0.35);
  border-radius: 8rpx; font-size: 22rpx; color: #00E5FF; font-family: 'Courier New', monospace; font-weight: 700; letter-spacing: 2rpx; }
.brand-en { display: block; margin-top: 10rpx; font-size: 20rpx; color: rgba(255,255,255,0.35);
  letter-spacing: 3rpx; font-family: 'Courier New', monospace; }

/* 数据条 */
.stat-strip { display: flex; align-items: center; margin-top: 40rpx;
  background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.08);
  border-radius: 16rpx; padding: 28rpx 0; backdrop-filter: blur(20rpx); }
.stat-cell { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.stat-num { font-size: 44rpx; font-weight: 800; color: #fff; font-family: 'Courier New', monospace; line-height: 1; }
.stat-num.accent { color: #00E5FF; text-shadow: 0 0 16rpx rgba(0,229,255,0.5); }
.stat-label { font-size: 20rpx; color: rgba(255,255,255,0.4); letter-spacing: 1rpx; }
.stat-sep { width: 1rpx; height: 56rpx; background: rgba(255,255,255,0.1); }

/* ── 功能面板 ── */
.panel { padding: 48rpx 40rpx 0; }
.panel-head { display: flex; align-items: center; gap: 16rpx; margin-bottom: 28rpx; }
.panel-bar { width: 6rpx; height: 32rpx; background: linear-gradient(180deg, #00E5FF, #2B7FFF); border-radius: 3rpx;
  box-shadow: 0 0 12rpx rgba(0,229,255,0.6); }
.panel-title { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; flex: 1; }
.panel-count { font-size: 22rpx; color: rgba(255,255,255,0.35); font-family: 'Courier New', monospace; }

.grid { display: flex; flex-direction: column; gap: 20rpx; }
.cell { position: relative; display: flex; align-items: center; gap: 24rpx;
  background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.08);
  border-radius: 18rpx; padding: 28rpx 28rpx; overflow: hidden;
  backdrop-filter: blur(20rpx); transition: all 0.2s; }
.cell-hover { background: rgba(0,229,255,0.06); border-color: rgba(0,229,255,0.3); transform: scale(0.985); }
.cell-glow { position: absolute; width: 180rpx; height: 180rpx; left: -40rpx; top: -50rpx; opacity: 0.6; }
.cell-icon { position: relative; width: 88rpx; height: 88rpx; border-radius: 16rpx;
  background: rgba(255,255,255,0.06); border: 1rpx solid rgba(255,255,255,0.12);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cell-emoji { font-size: 44rpx; }
.cell-body { position: relative; flex: 1; }
.cell-title { display: block; font-size: 30rpx; font-weight: 700; color: #fff; letter-spacing: 1rpx; }
.cell-desc { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 8rpx; }
.cell-arrow { position: relative; font-size: 40rpx; color: rgba(255,255,255,0.25); font-weight: 300; }

/* ── 底部 ── */
.footer { padding: 56rpx 40rpx 0; }
.logout { text-align: center; padding: 26rpx; background: rgba(255,255,255,0.03);
  border: 1rpx solid rgba(255,80,80,0.25); border-radius: 14rpx; }
.logout-hover { background: rgba(255,80,80,0.1); }
.logout-text { font-size: 28rpx; color: #FF6B6B; letter-spacing: 2rpx; }
.copyright { display: block; text-align: center; margin-top: 32rpx; font-size: 20rpx;
  color: rgba(255,255,255,0.25); letter-spacing: 1rpx; font-family: 'Courier New', monospace; }
</style>
