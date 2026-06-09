/**
 * 模块: 平台级 / shared
 * 功能: 通用格式化工具
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

export function maskPhone(phone) {
  if (!phone) return '';
  return String(phone).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2');
}

export function formatApprovalStatus(status, labels) {
  return labels?.[status] || status || '-';
}

export function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return `${(Number(value) * 100).toFixed(1)}%`;
}
