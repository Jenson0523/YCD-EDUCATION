/**
 * 模块: 家校共同体 / PC 后台
 * 功能: 家校报备接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { http } from './http';

export function fetchHomeReports(params) {
  return http.get('/fs/home-reports', { params });
}
