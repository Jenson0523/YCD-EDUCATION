/**
 * 模块: 统一审批流 / PC 后台
 * 功能: 审批实例接口 + 请假审批接口
 * 创建: 2026-06 / 扩展: 2026-06-12
 * 作者: 云辰盾项目组
 */

import { http } from './http';

/** 获取审批流实例列表（flow_instance 表，未来扩展用） */
export function fetchFlowInstances(params) {
  return http.get('/flow/instances', { params });
}

/** 获取请假审批列表（leave_application 表，当前审批中心数据源） */
export function fetchLeaveApplications(params) {
  return http.get('/leave/applications', { params });
}

/** 获取单条请假审批详情 */
export function fetchLeaveApplicationDetail(id) {
  return http.get(`/leave/applications/${id}`);
}

/** 审批操作（通过/驳回） */
export function approveLeaveApplication(id, data) {
  return http.put(`/leave/applications/${id}/approve`, data);
}

/** 获取补批工单列表 */
export function fetchSupplements(params) {
  return http.get('/leave/applications/supplements', { params });
}

/** 获取核验通行台账 */
export function fetchGateLedger(params) {
  return http.get('/leave/gate/ledger', { params });
}
