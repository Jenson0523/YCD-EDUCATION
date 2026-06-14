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
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/DashboardHome.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HR', 'FINANCE', 'HEAD_TEACHER', 'TEACHER', 'GATE', 'PARENT'] } },
      { path: 'family-school/home-reports', name: 'HomeReports', component: () => import('../views/family-school/HomeReports.vue'), meta: { roles: ['ADMIN', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'student/students', name: 'Students', component: () => import('../views/student/StudentList.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'workflow/instances', name: 'WorkflowInstances', component: () => import('../views/workflow/WorkflowInstances.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'sys/users', name: 'SysUsers', component: () => import('../views/system/UserManagement.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'sys/announcements', name: 'Announcements', component: () => import('../views/system/Announcement.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'ACADEMIC'] } },
      { path: 'academic/subjects', name: 'AcademicSubjects', component: () => import('../views/academic/SubjectList.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'academic/classes', name: 'AcademicClasses', component: () => import('../views/academic/ClassList.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL', 'ACADEMIC', 'HEAD_TEACHER'] } },
      { path: 'academic/scores', name: 'AcademicScores', component: () => import('../views/academic/ScoreList.vue'), meta: { roles: ['ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'academic/homeworks', name: 'AcademicHomeworks', component: () => import('../views/academic/HomeworkList.vue'), meta: { roles: ['ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'academic/teaching-progress', name: 'TeachingProgress', component: () => import('../views/academic/TeachingProgress.vue'), meta: { roles: ['ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'academic/student-honors', name: 'StudentHonors', component: () => import('../views/academic/StudentHonorList.vue'), meta: { roles: ['ADMIN', 'ACADEMIC', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'leave/face', name: 'FaceManagement', component: () => import('../views/leave/FaceManagement.vue'), meta: { roles: ['ADMIN', 'HEAD_TEACHER', 'TEACHER'] } },
      { path: 'leave/ledger', name: 'LeaveLedger', component: () => import('../views/leave/LeaveLedger.vue'), meta: { roles: ['ADMIN', 'HEAD_TEACHER', 'TEACHER', 'GATE'] } },
      { path: 'permission/bind', name: 'BindManagement', component: () => import('../views/permission/BindManagement.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'hr', name: 'HrPlaceholder', component: () => import('../views/placeholders/HrPlaceholder.vue'), meta: { roles: ['ADMIN', 'HR'] } },
      { path: 'finance', name: 'FinancePlaceholder', component: () => import('../views/placeholders/FinancePlaceholder.vue'), meta: { roles: ['ADMIN', 'FINANCE'] } },
      { path: 'insurance', name: 'InsurancePlaceholder', component: () => import('../views/placeholders/InsurancePlaceholder.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'logistics', name: 'LogisticsPlaceholder', component: () => import('../views/placeholders/LogisticsPlaceholder.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'psychology-safety', name: 'PsychologyPlaceholder', component: () => import('../views/placeholders/PsychologyPlaceholder.vue'), meta: { roles: ['ADMIN', 'HEAD_TEACHER'] } },
      { path: 'enrollment', name: 'EnrollmentPlaceholder', component: () => import('../views/placeholders/EnrollmentPlaceholder.vue'), meta: { roles: ['ADMIN', 'PRINCIPAL'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 导航守卫：未登录跳转登录页 + 角色权限校验
router.beforeEach((to) => {
  const token = localStorage.getItem('ycd_token');
  const roleCode = localStorage.getItem('ycd_roleCode') || '';

  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' };
  }

  // 角色权限校验：路由 meta.roles 未配置则放行，已配置则校验
  if (to.meta.roles && roleCode) {
    const allowedRoles = to.meta.roles;
    if (!allowedRoles.includes(roleCode)) {
      // 无权限，跳转到 dashboard（403 页后续可完善）
      return { path: '/dashboard' };
    }
  }
});

export default router;
