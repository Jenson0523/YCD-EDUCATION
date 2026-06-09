/**
 * 模块: PC 后台
 * 功能: 应用入口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './styles/theme.css';
import App from './App.vue';
import router from './router';

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app');
