/**
 * 模块: 学生学籍 / PC 后台
 * 功能: 学生档案接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { http } from './http';

export function fetchStudents(params) {
  return http.get('/stu/students', { params });
}
