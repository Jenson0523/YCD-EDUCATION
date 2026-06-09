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
page {
  background: #f6f8fb;
  color: #1f2937;
  font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
}

.section {
  margin: 24rpx;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 16rpx;
  border: 1rpx solid #e5e7eb;
}

.primary-button {
  background: #1b5ea6;
  color: #ffffff;
  border-radius: 12rpx;
}
</style>
