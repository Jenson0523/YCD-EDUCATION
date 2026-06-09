/**
 * 模块: PC 后台
 * 功能: 路由配置，按业务板块分组 + 登录守卫
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { createRouter, createWebHistory } from 'vue-router';
import AdminLayout from '../layouts/AdminLayout.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginPage.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/DashboardHome.vue') },
      { path: 'family-school/home-reports', name: 'HomeReports', component: () => import('../views/family-school/HomeReports.vue') },
      { path: 'student/students', name: 'Students', component: () => import('../views/student/StudentList.vue') },
      { path: 'workflow/instances', name: 'WorkflowInstances', component: () => import('../views/workflow/WorkflowInstances.vue') },
      { path: 'sys/users', name: 'SysUsers', component: () => import('../views/system/UserManagement.vue') },
      { path: 'academic/subjects', name: 'AcademicSubjects', component: () => import('../views/academic/SubjectList.vue') },
      { path: 'academic/classes', name: 'AcademicClasses', component: () => import('../views/academic/ClassList.vue') },
      { path: 'academic/scores', name: 'AcademicScores', component: () => import('../views/academic/ScoreList.vue') },
      { path: 'academic/homeworks', name: 'AcademicHomeworks', component: () => import('../views/academic/HomeworkList.vue') },
      { path: 'academic/teaching-progress', name: 'TeachingProgress', component: () => import('../views/academic/TeachingProgress.vue') },
      { path: 'academic/student-honors', name: 'StudentHonors', component: () => import('../views/academic/StudentHonorList.vue') },
      { path: 'leave/face', name: 'FaceManagement', component: () => import('../views/leave/FaceManagement.vue') },
      { path: 'leave/ledger', name: 'LeaveLedger', component: () => import('../views/leave/LeaveLedger.vue') },
      { path: 'permission/bind', name: 'BindManagement', component: () => import('../views/permission/BindManagement.vue') },
      { path: 'hr', name: 'HrPlaceholder', component: () => import('../views/placeholders/HrPlaceholder.vue') },
      { path: 'finance', name: 'FinancePlaceholder', component: () => import('../views/placeholders/FinancePlaceholder.vue') },
      { path: 'insurance', name: 'InsurancePlaceholder', component: () => import('../views/placeholders/InsurancePlaceholder.vue') },
      { path: 'logistics', name: 'LogisticsPlaceholder', component: () => import('../views/placeholders/LogisticsPlaceholder.vue') },
      { path: 'psychology-safety', name: 'PsychologyPlaceholder', component: () => import('../views/placeholders/PsychologyPlaceholder.vue') },
      { path: 'enrollment', name: 'EnrollmentPlaceholder', component: () => import('../views/placeholders/EnrollmentPlaceholder.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 导航守卫：未登录跳转登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('ycd_token');
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' };
  }
});

export default router;
