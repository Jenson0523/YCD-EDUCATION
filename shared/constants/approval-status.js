/**
 * 模块: 平台级 / shared
 * 功能: 统一审批状态枚举
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

export const APPROVAL_STATUS = Object.freeze({
  DRAFT: 'DRAFT',
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELED: 'CANCELED'
});

export const APPROVAL_STATUS_LABEL = Object.freeze({
  [APPROVAL_STATUS.DRAFT]: '草稿',
  [APPROVAL_STATUS.PENDING]: '待审核',
  [APPROVAL_STATUS.PROCESSING]: '审核中',
  [APPROVAL_STATUS.APPROVED]: '已通过',
  [APPROVAL_STATUS.REJECTED]: '已驳回',
  [APPROVAL_STATUS.CANCELED]: '已撤销'
});
