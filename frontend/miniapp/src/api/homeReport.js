/**
 * 模块: 家校共同体 / 小程序
 * 功能: 居家状态报备接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { request } from './request';

export const createHomeReport = (data) =>
  request({ url: '/fs/home-reports', method: 'POST', data });

export const fetchMyReports = ({ pageNo = 1, pageSize = 10 } = {}) =>
  request({ url: `/fs/home-reports?pageNo=${pageNo}&pageSize=${pageSize}` });
