<template>
  <view class="page">
    <scroll-view scroll-y class="scroll" :enhanced="true" :show-scrollbar="false">
      <view :style="{ height: statusBarH + 16 + 'px' }"></view>

      <!-- ========== 头部欢迎区 ========== -->
      <view class="header">
        <!-- 顶部装饰条 -->
        <view class="header-accent"></view>
        
        <view class="header-top">
          <!-- 左侧：问候 + 姓名 -->
          <view class="greet-block">
            <text class="greeting">{{ greeting }}</text>
            <text class="username">{{ realName }}</text>
            <!-- 身份信息条 -->
            <view class="identity-strip">
              <view class="role-badge">
                <view class="role-dot"></view>
                <text class="role-name">{{ roleLabel }}</text>
              </view>
              <!-- 班主任：所辖班级 -->
              <view v-if="roleCode === 'HEAD_TEACHER' && myClassNames" class="context-tag">
                <text class="context-icon">🏫</text>
                <text class="context-text">所辖班级：{{ myClassNames }}</text>
              </view>
              <!-- 家长：绑定学生 -->
              <view v-if="roleCode === 'PARENT' && boundStudents.length > 0" class="context-tag">
                <text class="context-icon">👨‍👩‍👧</text>
                <text class="context-text">
                  已绑定：<text v-for="(s, i) in boundStudents" :key="i">
                    {{ s.studentName }}<text v-if="s.className">（{{ s.className }}）</text>{{ i < boundStudents.length - 1 ? '、' : '' }}
                  </text>
                </text>
              </view>
              <!-- 教师 -->
              <view v-if="roleCode === 'TEACHER' && myClassNames" class="context-tag">
                <text class="context-icon">📚</text>
                <text class="context-text">任教班级：{{ myClassNames }}</text>
              </view>
              <!-- 门卫 -->
              <view v-if="roleCode === 'GATE'" class="context-tag">
                <text class="context-icon">🛂</text>
                <text class="context-text">今日在岗 · 门禁核验</text>
              </view>
            </view>
          </view>

          <!-- 右侧：头像 -->
          <view class="avatar-wrap">
            <view class="avatar-inner">
              <text class="avatar-char">{{ realName.charAt(0) }}</text>
            </view>
          </view>
        </view>

        <!-- 数据概览卡片 - 可点击 -->
        <view class="stats-card">
          <view class="stat-item" hover-class="stat-hover" @tap.stop="navTodo">
            <text class="stat-num">{{ stats.today }}</text>
            <text class="stat-label">待办事项</text>
            <text class="stat-arrow">›</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item" hover-class="stat-hover" @tap.stop="navMessages">
            <text class="stat-num">{{ stats.unread }}</text>
            <text class="stat-label">未读消息</text>
            <text class="stat-arrow">›</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item stat-time">
            <text class="stat-num time-num">{{ nowTime }}</text>
            <text class="stat-label">{{ todayStr }}</text>
          </view>
        </view>
      </view>

      <!-- ========== 功能区 ========== -->
      <view class="panel">
        <view class="panel-header">
          <view class="panel-line"></view>
          <text class="panel-title">{{ panelTitle }}</text>
          <text class="panel-sub">{{ entries.length }} 项功能</text>
        </view>

        <view class="grid">
          <view
            v-for="(it, idx) in entries"
            :key="idx"
            class="card"
            hover-class="card-press"
            @tap="nav(it.url)"
          >
            <view class="card-icon-box" :style="{ background: it.iconBg || '#EEF2FF' }">
              <text class="card-emoji">{{ it.icon }}</text>
            </view>
            <view class="card-body">
              <text class="card-title">{{ it.title }}</text>
              <text class="card-desc">{{ it.desc }}</text>
            </view>
            <text class="card-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 底部品牌 -->
      <view class="footer">
        <text class="brand-name">云辰盾 · YUNCHENDUN</text>
        <text class="brand-slogan">智慧家校共育 · 安全离校管理</text>
        <view class="logout-btn" hover-class="logout-press" @tap="handleLogout">
          <text class="logout-text">退出登录</text>
        </view>
      </view>

      <view style="height: 60rpx;"></view>
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

// 角色专属数据
const myClassNames = ref('');
const boundStudents = ref([]);

const greeting = (() => {
  const h = new Date().getHours();
  if (h < 6) return '凌晨好，';
  if (h < 12) return '上午好，';
  if (h < 14) return '中午好，';
  if (h < 18) return '下午好，';
  return '晚上好，';
})();

const roleLabel = computed(() => ({
  ADMIN: '系统管理员', PRINCIPAL: '校长', ACADEMIC: '教务主任',
  HEAD_TEACHER: '班主任', TEACHER: '任课教师', GATE: '门卫',
  PARENT: '家长', STUDENT: '学生'
}[roleCode.value] || '用户'));

// 角色专属图标背景色
const ICON_BG = {
  gold: '#FFF9F0',
  blue: '#EEF2FF', 
  cyan: '#ECFEFF',
  emerald: '#ECFDF5',
};

const MENUS = {
  HEAD_TEACHER: {
    title: '班主任工作台',
    items: [
      { icon: '✅', title: '请假审批', desc: '待办 · 签字确认', url: '/pages/leave/approve', iconBg: ICON_BG.gold },
      { icon: '📝', title: '代学生请假', desc: '快捷发起申请', url: '/pages/leave/apply', iconBg: ICON_BG.blue },
      { icon: '⚡', title: '临时补批', desc: '24小时内处理', url: '/pages/leave/approve?tab=TEMP_PENDING', iconBg: ICON_BG.cyan },
      { icon: '📊', title: '审批记录', desc: '历史查阅', url: '/pages/leave/approve?tab=APPROVED', iconBg: ICON_BG.emerald },
      { icon: '🧑‍💻', title: '人脸录入', desc: '代学生授权采集', url: '/pages/face/enroll', iconBg: ICON_BG.blue },
    ]
  },
  PARENT: {
    title: '家长服务台',
    items: [
      { icon: '📝', title: '申请请假', desc: '事假 · 病假', url: '/pages/leave/apply', iconBg: ICON_BG.gold },
      { icon: '📋', title: '我的请假', desc: '审批进度追踪', url: '/pages/leave/my-leaves', iconBg: ICON_BG.blue },
      { icon: '🏠', title: '居家报备', desc: '作息 · 健康', url: '/pages/home-report/home-report', iconBg: ICON_BG.cyan },
      { icon: '📬', title: '我的报备', desc: '老师回复', url: '/pages/my-reports/my-reports', iconBg: ICON_BG.emerald },
      { icon: '🧑‍💻', title: '人脸录入', desc: '为孩子授权采集', url: '/pages/face/enroll', iconBg: ICON_BG.gold },
    ]
  },
  TEACHER: {
    title: '教师工作台',
    items: [
      { icon: '📝', title: '代学生请假', desc: '快捷发起申请', url: '/pages/leave/apply', iconBg: ICON_BG.gold },
      { icon: '📋', title: '我的申请', desc: '提交记录', url: '/pages/leave/my-leaves', iconBg: ICON_BG.blue },
      { icon: '🏠', title: '班级报备', desc: '居家跟进', url: '/pages/home-report/home-report', iconBg: ICON_BG.cyan },
      { icon: '📚', title: '教务管理', desc: '作业成绩', url: '/pages/academic/homeworks', iconBg: ICON_BG.emerald },
      { icon: '🧑‍💻', title: '人脸录入', desc: '代学生授权采集', url: '/pages/face/enroll', iconBg: ICON_BG.gold },
    ]
  },
  GATE: {
    title: '门卫核验台',
    items: [
      { icon: '🛂', title: '刷脸核验', desc: 'AI 自动识别', url: '/pages/gate/verify', iconBg: ICON_BG.gold },
      { icon: '📋', title: '今日假条', desc: '在效列表', url: '/pages/gate/today-leaves', iconBg: ICON_BG.blue },
      { icon: '⚡', title: '临时放行', desc: '紧急登记', url: '/pages/gate/temp-depart', iconBg: ICON_BG.cyan },
      { icon: '⚠️', title: '异常记录', desc: '拦截日志', url: '/pages/gate/today-leaves', iconBg: ICON_BG.emerald },
    ]
  },
  STUDENT: {
    title: '学生服务台',
    items: [
      { icon: '📋', title: '我的请假', desc: '家长代申请', url: '/pages/leave/my-leaves', iconBg: ICON_BG.gold },
      { icon: '📬', title: '我的报备', desc: '查看记录', url: '/pages/my-reports/my-reports', iconBg: ICON_BG.blue },
      { icon: '📊', title: '我的成绩', desc: '成绩查询', url: '/pages/academic/scores', iconBg: ICON_BG.cyan },
      { icon: '📚', title: '作业列表', desc: '待完成', url: '/pages/academic/homeworks', iconBg: ICON_BG.emerald },
    ]
  },
};

const current = computed(() => {
  const map = {
    HEAD_TEACHER: MENUS.HEAD_TEACHER,
    PARENT: MENUS.PARENT,
    TEACHER: MENUS.TEACHER,
    GATE: MENUS.GATE,
    STUDENT: MENUS.STUDENT,
  };
  return map[roleCode.value] || MENUS.PARENT;
});

const panelTitle = computed(() => current.value.title);
const entries = computed(() => current.value.items);

const updateClock = () => {
  const d = new Date();
  nowTime.value = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
  todayStr.value = `${d.getMonth() + 1}月${d.getDate()}日 周${week}`;
};

// 加载角色专属信息
const loadMyInfo = async () => {
  try {
    const info = await request({ url: '/auth/my-info' });
    if (info.classNames) {
      myClassNames.value = info.classNames;
      uni.setStorageSync('ycd_classNames', info.classNames);
      uni.setStorageSync('ycd_managedClasses', JSON.stringify(info.managedClasses || []));
    }
    if (info.boundStudents) {
      boundStudents.value = info.boundStudents;
      uni.setStorageSync('ycd_boundStudents', JSON.stringify(info.boundStudents));
    }
  } catch {
    myClassNames.value = uni.getStorageSync('ycd_classNames') || '';
    try {
      boundStudents.value = JSON.parse(uni.getStorageSync('ycd_boundStudents') || '[]');
    } catch { boundStudents.value = []; }
  }
};

const loadStats = async () => {
  try {
    const r = await request({ url: '/sys/messages/unread-count' });
    stats.value.unread = r?.count || 0;
  } catch {}
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

// 点击待办事项跳转
const navTodo = () => {
  if (roleCode.value === 'HEAD_TEACHER') {
    uni.navigateTo({ url: '/pages/leave/approve' });
  } else if (roleCode.value === 'GATE') {
    uni.navigateTo({ url: '/pages/gate/verify' });
  } else if (roleCode.value === 'PARENT') {
    uni.navigateTo({ url: '/pages/leave/my-leaves' });
  } else {
    uni.navigateTo({ url: '/pages/leave/my-leaves' });
  }
};

// 点击未读消息跳转
const navMessages = () => {
  uni.navigateTo({ url: '/pages/messages/messages' });
};

onMounted(() => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
  if (!uni.getStorageSync('ycd_token')) {
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  updateClock();
  setInterval(updateClock, 30000);
  loadMyInfo();
  loadStats();
});

const nav = (url) => uni.navigateTo({ url });

const handleLogout = () => {
  uni.showModal({
    title: '提示', content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        ['ycd_token', 'ycd_userId', 'ycd_realName', 'ycd_roleCode',
         'ycd_classNames', 'ycd_managedClasses', 'ycd_boundStudents'
        ].forEach(k => uni.removeStorageSync(k));
        uni.reLaunch({ url: '/pages/login/login' });
      }
    }
  });
};
</script>

<style scoped>
/* ========== 基础 ========== */
.page { min-height: 100vh; background: linear-gradient(180deg, #F0F4FA 0%, #E8EDF5 40%, #F5F7FA 100%); position: relative; }
.scroll { position: relative; z-index: 1; height: 100vh; }

/* ========== 头部 ========== */
.header {
  margin: 0 28rpx;
  background: linear-gradient(160deg, #1E2F50 0%, #2D4A7A 50%, #3B6CB5 100%);
  border-radius: 28rpx;
  padding: 36rpx 32rpx 32rpx;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(30, 47, 80, 0.18);
}
.header-accent {
  position: absolute; top: 0; left: 0; right: 0; height: 4rpx;
  background: linear-gradient(90deg, transparent, #D4A853, #E8C068, #D4A853, transparent);
  opacity: 0.7;
}
.header-top { display: flex; justify-content: space-between; align-items: flex-start; }

/* 问候区 */
.greet-block { flex: 1; display: flex; flex-direction: column; }
.greeting { font-size: 24rpx; color: rgba(255,255,255,0.6); letter-spacing: 1rpx; }
.username { font-size: 44rpx; font-weight: 700; color: #fff; margin-top: 4rpx; letter-spacing: 2rpx; }

/* 身份信息条 */
.identity-strip { display: flex; flex-direction: column; gap: 12rpx; margin-top: 20rpx; }
.role-badge {
  display: inline-flex; align-items: center; gap: 10rpx; align-self: flex-start;
  padding: 8rpx 22rpx;
  background: rgba(255,255,255,0.12);
  border: 1rpx solid rgba(255,255,255,0.18);
  border-radius: 30rpx;
}
.role-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: #E8C068; }
.role-name { font-size: 24rpx; color: #F0D78C; font-weight: 600; letter-spacing: 2rpx; }

.context-tag {
  display: flex; align-items: flex-start; gap: 10rpx;
  padding: 10rpx 20rpx; background: rgba(255,255,255,0.08);
  border-radius: 14rpx;
}
.context-icon { font-size: 24rpx; flex-shrink: 0; }
.context-text { font-size: 23rpx; color: rgba(255,255,255,0.7); line-height: 1.6; }

/* 头像 */
.avatar-wrap { position: relative; width: 96rpx; height: 96rpx; flex-shrink: 0; margin-top: 4rpx; }
.avatar-inner {
  width: 100%; height: 100%; border-radius: 50%;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0.08));
  border: 2rpx solid rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center;
}
.avatar-char { font-size: 40rpx; font-weight: 700; color: #fff; }

/* 数据卡片 */
.stats-card {
  display: flex; align-items: center;
  margin-top: 30rpx; padding: 24rpx 0;
  background: rgba(255,255,255,0.1);
  border-radius: 18rpx;
  backdrop-filter: blur(20rpx);
}
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6rpx; position: relative; }
.stat-hover { background: rgba(255,255,255,0.08); border-radius: 14rpx; }
.stat-num { font-size: 42rpx; font-weight: 700; color: #fff; font-family: 'DIN', 'Courier New', monospace; line-height: 1; }
.time-num { color: #F0D78C; font-size: 36rpx; }
.stat-label { font-size: 22rpx; color: rgba(255,255,255,0.5); letter-spacing: 1rpx; }
.stat-arrow { font-size: 24rpx; color: rgba(255,255,255,0.3); position: absolute; right: 16rpx; top: 50%; transform: translateY(-50%); }
.stat-time { min-width: 160rpx; }
.stat-divider { width: 1rpx; height: 48rpx; background: rgba(255,255,255,0.12); }

/* ========== 功能面板 ========== */
.panel { padding: 36rpx 28rpx 0; }
.panel-header { display: flex; align-items: center; gap: 14rpx; margin-bottom: 24rpx; }
.panel-line {
  width: 5rpx; height: 32rpx;
  background: linear-gradient(180deg, #D4A853, #3B6CB5);
  border-radius: 3rpx;
}
.panel-title { font-size: 30rpx; font-weight: 700; color: #1E2F50; letter-spacing: 2rpx; flex: 1; }
.panel-sub { font-size: 22rpx; color: #94A3B8; }

/* 功能卡片 */
.grid { display: flex; flex-direction: column; gap: 16rpx; }
.card {
  display: flex; align-items: center; gap: 20rpx;
  background: #fff;
  border-radius: 18rpx; padding: 26rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.card-press { background: #F8FAFC; transform: scale(0.985); box-shadow: 0 1rpx 8rpx rgba(0,0,0,0.06); }
.card-icon-box {
  width: 80rpx; height: 80rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.card-emoji { font-size: 40rpx; }
.card-body { flex: 1; min-width: 0; }
.card-title { display: block; font-size: 28rpx; font-weight: 600; color: #1E293B; letter-spacing: 1rpx; }
.card-desc { display: block; font-size: 22rpx; color: #94A3B8; margin-top: 6rpx; }
.card-arrow { font-size: 36rpx; color: #CBD5E1; font-weight: 300; flex-shrink: 0; }

/* ========== 底部 ========== */
.footer { padding: 52rpx 28rpx 0; text-align: center; }
.brand-name { display: block; font-size: 24rpx; font-weight: 600; color: #94A3B8; letter-spacing: 4rpx; }
.brand-slogan { display: block; margin-top: 8rpx; font-size: 20rpx; color: #CBD5E1; letter-spacing: 2rpx; }
.logout-btn {
  margin-top: 36rpx; padding: 22rpx 0; text-align: center;
  background: #fff; border: 1rpx solid #FEE2E2; border-radius: 14rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.logout-press { background: #FFF5F5; }
.logout-text { font-size: 26rpx; color: #EF4444; letter-spacing: 2rpx; }
</style>
