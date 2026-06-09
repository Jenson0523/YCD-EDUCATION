/**
 * 模块: 小程序
 * 功能: uni-app 入口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { createSSRApp } from 'vue';
import App from './App.vue';

export function createApp() {
  const app = createSSRApp(App);
  return { app };
}
