<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <strong>云辰盾</strong>
        <span>家校共育共同体</span>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon>数据总览</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN', 'HEAD_TEACHER', 'TEACHER')" index="/family-school/home-reports"><el-icon><ChatDotRound /></el-icon>家校互通</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN', 'PRINCIPAL', 'HEAD_TEACHER', 'TEACHER')" index="/student/students"><el-icon><User /></el-icon>学生档案</el-menu-item>
        <el-sub-menu v-if="canAccess('ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic">
          <template #title><el-icon><Reading /></el-icon>教务管理</template>
          <el-menu-item v-if="canAccess('ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic/subjects">学科管理</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER')" index="/academic/classes">班级管理</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic/scores">成绩管理</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic/homeworks">作业管理</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic/teaching-progress">教学进度</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/academic/student-honors">学生评优</el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="canAccess('ADMIN', 'HR')" index="/hr"><el-icon><Briefcase /></el-icon>人事薪资</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN', 'FINANCE')" index="/finance"><el-icon><Money /></el-icon>财务收费</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN')" index="/insurance"><el-icon><FirstAidKit /></el-icon>保险服务</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN')" index="/logistics"><el-icon><Van /></el-icon>后勤服务</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN', 'HEAD_TEACHER')" index="/psychology-safety"><el-icon><Warning /></el-icon>心理安全</el-menu-item>
        <el-menu-item v-if="canAccess('ADMIN', 'PRINCIPAL')" index="/enrollment"><el-icon><Promotion /></el-icon>招生升学</el-menu-item>
        <el-sub-menu v-if="canAccess('ADMIN', 'HEAD_TEACHER', 'TEACHER', 'GATE')" index="/leave">
          <template #title><el-icon><Ticket /></el-icon>请假离校</template>
          <el-menu-item v-if="canAccess('ADMIN', 'HEAD_TEACHER', 'TEACHER')" index="/leave/face">人脸档案管理</el-menu-item>
          <el-menu-item v-if="canAccess('ADMIN', 'HEAD_TEACHER', 'TEACHER', 'GATE')" index="/leave/ledger">请假台账</el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="canAccess('ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER')" index="/workflow/instances"><el-icon><Finished /></el-icon>审批中心</el-menu-item>
        <el-sub-menu v-if="canAccess('ADMIN')" index="/sys">
          <template #title><el-icon><Setting /></el-icon>系统管理</template>
          <el-menu-item index="/sys/users">用户管理</el-menu-item>
          <el-menu-item index="/permission/bind">数据权限绑定</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">综合管理后台</span>
        <div class="header-right">
          <!-- 消息铃铛 -->
          <el-badge :value="authStore.unreadCount || null" class="msg-badge" @click="showMessages = true">
            <el-icon size="20" style="cursor:pointer"><Bell /></el-icon>
          </el-badge>
          <!-- 用户信息 -->
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" style="background:#1b5ea6;font-size:12px">
                {{ authStore.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span>{{ authStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 消息抽屉 -->
    <el-drawer v-model="showMessages" title="消息通知" size="380px" @open="loadMessages">
      <div v-if="messages.length === 0" class="no-msg">暂无消息</div>
      <div v-else>
        <el-button size="small" text @click="readAll" style="margin-bottom:12px">全部已读</el-button>
        <div v-for="msg in messages" :key="msg.id" class="msg-item" :class="{ unread: !msg.isRead }" @click="readMsg(msg)">
          <div class="msg-title">{{ msg.title }}</div>
          <div class="msg-content">{{ msg.content }}</div>
          <div class="msg-time">{{ msg.createdAt }}</div>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  ArrowDown, Bell, Briefcase, ChatDotRound, DataBoard,
  Finished, FirstAidKit, Money, Promotion, Reading,
  Setting, Ticket, User, Van, Warning
} from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
import { fetchUnreadCount, fetchMessages, markMessageRead, markAllRead } from '../api/auth';

const router = useRouter();
const authStore = useAuthStore();
const showMessages = ref(false);
const messages = ref([]);

// 角色菜单权限判断
const roleCode = authStore.roleCode || localStorage.getItem('ycd_roleCode') || '';
const canAccess = (...roles) => roles.length === 0 || roles.includes(roleCode);

onMounted(async () => {
  try {
    const res = await fetchUnreadCount();
    authStore.unreadCount = res?.count || 0;
  } catch {}
});

const loadMessages = async () => {
  try {
    const res = await fetchMessages({ pageNo: 1, pageSize: 30 });
    messages.value = res?.records || [];
    authStore.unreadCount = 0;
  } catch {}
};

const readMsg = async (msg) => {
  if (!msg.isRead) {
    await markMessageRead(msg.id);
    msg.isRead = true;
  }
};

const readAll = async () => {
  await markAllRead();
  messages.value.forEach(m => m.isRead = true);
};

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' });
    await authStore.logout();
    router.replace('/login');
  }
};
</script>

<style scoped>
.admin-layout { min-height: 100vh; }

.aside {
  background: #fff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}

.brand {
  height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
}

.brand strong { color: #1b5ea6; font-size: 18px; }
.brand span { color: #9ca3af; font-size: 11px; }
.menu { border-right: none; }

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 20px;
}

.header-title { font-weight: 600; color: #374151; }

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.msg-badge { cursor: pointer; }

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
}

.main { background: #f6f8fb; }

.no-msg { text-align: center; color: #9ca3af; padding: 40px 0; }

.msg-item {
  padding: 14px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #f9fafb;
  cursor: pointer;
  border-left: 3px solid transparent;
}

.msg-item.unread {
  background: #eff6ff;
  border-left-color: #1b5ea6;
}

.msg-title { font-weight: 600; font-size: 14px; color: #111827; }
.msg-content { margin-top: 4px; font-size: 13px; color: #6b7280; }
.msg-time { margin-top: 6px; font-size: 12px; color: #9ca3af; }
</style>
