/**
 * 模块: 统一审批流 / PC 后台
 * 功能: 审批实例接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { http } from './http';

export function fetchFlowInstances(params) {
  return http.get('/flow/instances', { params });
}
