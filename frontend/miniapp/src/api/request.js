/**
 * 模块: 小程序
 * 功能: 请求封装，自动注入 token，401 跳登录页
 *   - 开发者工具(模拟器) 自动用 localhost
 *   - 真机调试/预览 自动用电脑局域网IP
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */

// ═══════════ 生产发布配置（部署云服务器后启用）═══════════
// 上线步骤见 deploy/DEPLOY.md 第六节：
//   1. 把 PROD_API 改成你的 https 域名（须ICP备案 + 微信后台配置合法域名）
//   2. PROD_MODE 改为 true
//   3. npm run build:mp-weixin 重新编译并上传发布
const PROD_MODE = true;
const PROD_API = 'https://node.alois.vip/api';

// ── 本地开发配置 ──
// 电脑局域网IP（真机调试时手机通过此IP访问后端，需与手机同一WiFi）
const LAN_IP = '192.168.0.40';
const PORT = 8081;

// 根据运行环境自动选择 host
function resolveBaseUrl() {
  if (PROD_MODE) return PROD_API; // 生产：统一走云服务器 https
  let platform = '';
  try {
    // 优先用新版 API
    if (uni.getDeviceInfo) {
      platform = uni.getDeviceInfo().platform || '';
    } else {
      platform = uni.getSystemInfoSync().platform || '';
    }
  } catch (e) {}
  // 开发者工具模拟器：platform === 'devtools'，可直接用 localhost
  if (platform === 'devtools') {
    return `http://localhost:${PORT}/api`;
  }
  // 真机：使用局域网IP
  return `http://${LAN_IP}:${PORT}/api`;
}

const BASE_URL = resolveBaseUrl();

/**
 * 受保护图片访问URL。
 * 人脸/凭证/签字等敏感图片不再公开静态暴露，统一经
 * /api/files/preview?path=...&token=... 鉴权后输出（防人脸数据泄露）。
 * 兼容历史数据中存的绝对URL（含 /uploads/ 的会被改写为鉴权地址）。
 */
export function assetUrl(path) {
  if (!path) return '';
  let p = String(path);
  // 历史绝对URL：提取其中的 /uploads/ 相对部分
  const i = p.indexOf('/uploads/');
  if (i >= 0) p = p.substring(i);
  else if (/^https?:\/\//.test(p)) return p; // 非本系统外链原样返回
  const token = uni.getStorageSync('ycd_token') || '';
  return `${BASE_URL}/files/preview?path=${encodeURIComponent(p)}&token=${encodeURIComponent(token)}`;
}

/** 上传图片文件，返回 { url, absoluteUrl } */
export function uploadFile(filePath, category = 'common') {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('ycd_token') || '';
    uni.uploadFile({
      url: `${BASE_URL}/upload/image`,
      filePath,
      name: 'file',
      formData: { category },
      header: { Authorization: token },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          if (body && body.code === 0) resolve(body.data);
          else reject(new Error((body && body.message) || '上传失败'));
        } catch (e) {
          reject(new Error('上传响应解析失败'));
        }
      },
      fail: (err) => reject(new Error((err && err.errMsg) || '上传失败'))
    });
  });
}

export function request({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('ycd_token') || '';
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      timeout: 15000,
      header: { Authorization: token, 'Content-Type': 'application/json' },
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('ycd_token');
          uni.reLaunch({ url: '/pages/login/login' });
          reject(new Error('登录已过期，请重新登录'));
          return;
        }
        if (res.statusCode >= 500) {
          reject(new Error(`服务器错误(${res.statusCode})`));
          return;
        }
        const body = res.data;
        if (body && body.code === 0) {
          resolve(body.data);
        } else {
          reject(new Error((body && body.message) || `请求失败(${res.statusCode})`));
        }
      },
      fail: (err) => {
        // 把真实失败原因暴露出来，便于排查（域名校验/网络不通等）
        const msg = err && err.errMsg ? err.errMsg : '网络异常';
        let friendly = msg;
        if (msg.indexOf('domain list') >= 0 || msg.indexOf('not in domain') >= 0) {
          friendly = '域名未校验：请在开发者工具「详情→本地设置」勾选「不校验合法域名」';
        } else if (msg.indexOf('timeout') >= 0) {
          friendly = '请求超时：请确认手机与电脑同一WiFi，后端已启动';
        } else if (msg.indexOf('fail') >= 0) {
          friendly = `网络连接失败：${msg}`;
        }
        reject(new Error(friendly));
      }
    });
  });
}
