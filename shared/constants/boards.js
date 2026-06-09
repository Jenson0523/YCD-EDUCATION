/**
 * 模块: 平台级 / shared
 * 功能: 十大业务板块常量
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

export const BUSINESS_MODULE = Object.freeze({
  FAMILY_SCHOOL: 'family-school',
  ACADEMIC: 'academic',
  HR: 'hr',
  STUDENT: 'student',
  FINANCE: 'finance',
  INSURANCE: 'insurance',
  LOGISTICS: 'logistics',
  PSYCHOLOGY_SAFETY: 'psychology-safety',
  ENROLLMENT: 'enrollment',
  DASHBOARD: 'dashboard'
});

export const BUSINESS_MODULE_META = Object.freeze({
  [BUSINESS_MODULE.FAMILY_SCHOOL]: { code: 'FS', apiPrefix: '/api/fs', tablePrefix: 'fs_', label: '家校共同体' },
  [BUSINESS_MODULE.ACADEMIC]: { code: 'EDU', apiPrefix: '/api/edu', tablePrefix: 'edu_', label: '教务管理' },
  [BUSINESS_MODULE.HR]: { code: 'HR', apiPrefix: '/api/hr', tablePrefix: 'hr_', label: '人事薪资' },
  [BUSINESS_MODULE.STUDENT]: { code: 'STU', apiPrefix: '/api/stu', tablePrefix: 'stu_', label: '学生档案' },
  [BUSINESS_MODULE.FINANCE]: { code: 'FIN', apiPrefix: '/api/fin', tablePrefix: 'fin_', label: '财务收费' },
  [BUSINESS_MODULE.INSURANCE]: { code: 'INS', apiPrefix: '/api/ins', tablePrefix: 'ins_', label: '保险服务' },
  [BUSINESS_MODULE.LOGISTICS]: { code: 'LOGI', apiPrefix: '/api/logi', tablePrefix: 'logi_', label: '后勤服务' },
  [BUSINESS_MODULE.PSYCHOLOGY_SAFETY]: { code: 'PSY', apiPrefix: '/api/psy', tablePrefix: 'psy_', label: '心理安全' },
  [BUSINESS_MODULE.ENROLLMENT]: { code: 'ENR', apiPrefix: '/api/enr', tablePrefix: 'enr_', label: '招生升学' },
  [BUSINESS_MODULE.DASHBOARD]: { code: 'DASH', apiPrefix: '/api/dash', tablePrefix: 'dash_', label: '数据驾驶舱' }
});
