<script>
export default {
  onLaunch() {
    console.log('云辰盾小程序启动');
    const token = uni.getStorageSync('ycd_token');
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' });
    }
  }
};
</script>

<style>
/* ===== 云辰盾 · 全局科技感设计基调 ===== */
page {
  background: #eef2f9;
  color: #1e293b;
  font-family: "PingFang SC", "Microsoft YaHei", -apple-system, Arial, sans-serif;
  --ycd-primary: #2b6cff;
  --ycd-cyan: #15c8e0;
  --ycd-deep: #0b2a6b;
  --ycd-grad: linear-gradient(135deg, #2b6cff 0%, #15c8e0 100%);
  --ycd-grad-deep: linear-gradient(150deg, #0b2a6b 0%, #1746b5 55%, #1f6bd6 100%);
}

.section {
  margin: 24rpx;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 30rpx rgba(15, 40, 100, 0.06);
}

/* 通用渐变主按钮 */
.primary-button {
  background: var(--ycd-grad);
  color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 10rpx 24rpx rgba(43, 108, 255, 0.32);
}

/* 玻璃拟态卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.6);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 40, 100, 0.08);
}
</style>
