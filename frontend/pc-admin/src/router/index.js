/**
 * 模块: PC 后台
 * 功能: 路由配置，按业务板块分组
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { createRouter, createWebHistory } from 'vue-router';
import AdminLayout from '../layouts/AdminLayout.vue';

const routes = [
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/DashboardHome.vue') },
      { path: 'family-school/home-reports', name: 'HomeReports', component: () => import('../views/family-school/HomeReports.vue') },
      { path: 'student/students', name: 'Students', component: () => import('../views/student/StudentList.vue') },
      { path: 'workflow/instances', name: 'WorkflowInstances', component: () => import('../views/workflow/WorkflowInstances.vue') },
      { path: 'academic', name: 'AcademicPlaceholder', component: () => import('../views/placeholders/AcademicPlaceholder.vue') },
      { path: 'hr', name: 'HrPlaceholder', component: () => import('../views/placeholders/HrPlaceholder.vue') },
      { path: 'finance', name: 'FinancePlaceholder', component: () => import('../views/placeholders/FinancePlaceholder.vue') },
      { path: 'insurance', name: 'InsurancePlaceholder', component: () => import('../views/placeholders/InsurancePlaceholder.vue') },
      { path: 'logistics', name: 'LogisticsPlaceholder', component: () => import('../views/placeholders/LogisticsPlaceholder.vue') },
      { path: 'psychology-safety', name: 'PsychologyPlaceholder', component: () => import('../views/placeholders/PsychologyPlaceholder.vue') },
      { path: 'enrollment', name: 'EnrollmentPlaceholder', component: () => import('../views/placeholders/EnrollmentPlaceholder.vue') }
    ]
  }
];

export default createRouter({
  history: createWebHistory(),
  routes
});
