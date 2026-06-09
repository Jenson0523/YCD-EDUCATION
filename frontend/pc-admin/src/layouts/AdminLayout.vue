<template>
  <el-container class="admin-layout">
    <el-aside width="248px" class="aside">
      <div class="brand">
        <strong>云辰盾</strong>
        <span>家校共育共同体</span>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon>数据总览</el-menu-item>
        <el-menu-item index="/family-school/home-reports"><el-icon><ChatDotRound /></el-icon>家校互通</el-menu-item>
        <el-menu-item index="/student/students"><el-icon><User /></el-icon>学生档案</el-menu-item>
        <el-menu-item index="/academic"><el-icon><Reading /></el-icon>教务管理</el-menu-item>
        <el-menu-item index="/hr"><el-icon><Briefcase /></el-icon>人事薪资</el-menu-item>
        <el-menu-item index="/finance"><el-icon><Money /></el-icon>财务收费</el-menu-item>
        <el-menu-item index="/insurance"><el-icon><FirstAidKit /></el-icon>保险服务</el-menu-item>
        <el-menu-item index="/logistics"><el-icon><Van /></el-icon>后勤服务</el-menu-item>
        <el-menu-item index="/psychology-safety"><el-icon><Warning /></el-icon>心理安全</el-menu-item>
        <el-menu-item index="/enrollment"><el-icon><Promotion /></el-icon>招生升学</el-menu-item>
        <el-menu-item index="/workflow/instances"><el-icon><Finished /></el-icon>审批中心</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>综合管理后台</span>
        <el-tag type="success">develop</el-tag>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {
  Briefcase,
  ChatDotRound,
  DataBoard,
  Finished,
  FirstAidKit,
  Money,
  Promotion,
  Reading,
  User,
  Van,
  Warning
} from '@element-plus/icons-vue';
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.aside {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.brand {
  height: 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
}

.brand strong {
  color: var(--ycd-primary);
  font-size: 20px;
}

.brand span {
  color: var(--ycd-text-secondary);
  font-size: 12px;
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.main {
  background: var(--ycd-bg);
}
</style>
