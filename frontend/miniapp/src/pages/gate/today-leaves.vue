<template>
  <view class="page">
    <!-- Header -->
    <view class="hero">
      <view class="hero-bg"></view>
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @click="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <text class="hero-title">今日假条</text>
          <view style="width:60rpx;"></view>
        </view>
        <view class="hero-stats">
          <view class="stat-item" :class="{ 'stat-active': filter === 'all' }" @click="filter = 'all'">
            <text class="stat-num">{{ list.length }}</text>
            <text class="stat-label">今日总计</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item" :class="{ 'stat-active': filter === 'departed' }" @click="filter = 'departed'">
            <text class="stat-num">{{ departed }}</text>
            <text class="stat-label">已离校</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item" :class="{ 'stat-active': filter === 'pending' }" @click="filter = 'pending'">
            <text class="stat-num stat-warn">{{ pending }}</text>
            <text class="stat-label">待离校</text>
          </view>
        </view>
        <text class="filter-hint">点击上方统计可筛选 · 待离校项可直接刷脸放行</text>
      </view>
    </view>

    <!-- 内容 -->
    <view class="body">
      <view v-if="loading" class="skeleton-list">
        <view class="skeleton-card" v-for="i in 3" :key="i">
          <view class="sk-line sk-w60"></view>
          <view class="sk-line sk-w40 sk-short"></view>
        </view>
      </view>
      <view v-else-if="filteredList.length === 0" class="empty-state">
        <text class="empty-icon">📋</text>
        <text class="empty-text">{{ filter === 'pending' ? '没有待离校学生' : filter === 'departed' ? '暂无已离校记录' : '今日暂无有效假条' }}</text>
        <text class="empty-sub">{{ filter === 'pending' ? '待离校学生均已核验放行' : '所有学生均在校' }}</text>
      </view>
      <view v-else class="leave-list">
        <view
          v-for="item in filteredList"
          :key="item.id"
          class="leave-card"
          :class="item.departAt ? 'card-departed' : 'card-pending'"
          @click="goDetail(item.id)"
        >
          <view class="card-left-bar" :class="item.isTemp ? 'bar-amber' : (item.departAt ? 'bar-gray' : 'bar-blue')"></view>
          <view class="card-body">
            <view class="card-top">
              <view class="student-info">
                <text class="student-name">{{ item.studentName }}</text>
                <text class="class-tag">{{ item.className }}</text>
              </view>
              <view class="badges">
                <view class="badge" :class="item.isTemp ? 'badge-amber' : 'badge-green'">
                  {{ item.isTemp ? '⚡ 临时' : '✓ 已批' }}
                </view>
                <view class="badge" :class="item.departAt ? 'badge-gray' : 'badge-blue'">
                  {{ item.departAt ? '已离校' : '未离校' }}
                </view>
              </view>
            </view>

            <view class="card-info">
              <view class="info-item">
                <text class="info-icon">🏥</text>
                <text class="info-val">{{ item.leaveType === 'SICK' ? '病假' : '事假' }}</text>
              </view>
              <view class="info-item">
                <text class="info-icon">⏰</text>
                <text class="info-val">{{ fmtDt(item.leaveStart) }}</text>
              </view>
              <view class="info-item">
                <text class="info-icon">🔙</text>
                <text class="info-val">{{ fmtDt(item.leaveEnd) }}</text>
              </view>
            </view>

            <view class="card-footer">
              <text class="leave-no">{{ item.leaveNo }}</text>
              <view class="ft-right">
                <text v-if="item.approveSignatureUrl" class="sig-tag">✍ 已签字</text>
                <text v-if="item.departAt" class="depart-time">离校 {{ fmtDt(item.departAt) }}</text>
              </view>
            </view>

            <!-- 待离校：可直接刷脸放行 -->
            <view v-if="!item.departAt" class="card-action" @click.stop="goVerify(item)">
              <text class="ca-icon">🛂</text>
              <text class="ca-text">刷脸放行</text>
              <text class="ca-arrow">›</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { request } from '../../api/request';

const list = ref([]);
const loading = ref(true);
const filter = ref('all'); // all / departed / pending

const departed = computed(() => list.value.filter(i => i.departAt).length);
const pending = computed(() => list.value.filter(i => !i.departAt).length);
const filteredList = computed(() => {
  if (filter.value === 'departed') return list.value.filter(i => i.departAt);
  if (filter.value === 'pending') return list.value.filter(i => !i.departAt);
  return list.value;
});

onMounted(async () => {
  // 角色守卫：仅门卫可查看今日假条
  const roleCode = uni.getStorageSync('ycd_roleCode') || '';
  if (roleCode !== 'GATE') {
    uni.showToast({ title: '仅门卫可查看', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 1500);
    return;
  }
  try {
    list.value = await request({ url: '/leave/applications/today-valid' }) || [];
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' });
  } finally { loading.value = false; }
});

const goDetail = (id) => uni.navigateTo({ url: `/pages/leave/detail?id=${id}` });
// 待离校学生 → 跳到刷脸核验页（门卫直接给该生刷脸放行）
const goVerify = (item) => uni.navigateTo({ url: `/pages/gate/verify?studentNo=${item.studentNo || ''}` });
const fmtDt = (dt) => {
  if (!dt) return '—';
  const s = dt.replace('T', ' ');
  return s.slice(5, 16); // MM-DD HH:mm
};
</script>

<style scoped>
.page { background: #F0F4FA; min-height: 100vh; }

/* Hero */
.hero {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #06133D 0%, #0D2A8F 55%, #1947C8 100%);
  padding: 60rpx 32rpx 40rpx;
  border-radius: 0 0 40rpx 40rpx;
}
.hero-bg {
  position: absolute; inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; margin-bottom: 32rpx; }
.back-btn { width: 60rpx; height: 60rpx; background: rgba(255,255,255,0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 48rpx; color: #fff; }
.hero-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }

.hero-stats { display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.08); border-radius: 20rpx; padding: 24rpx 0; }
.stat-item { flex: 1; text-align: center; padding: 8rpx 0; border-radius: 14rpx; transition: background 0.2s; }
.stat-active { background: rgba(255,255,255,0.14); }
.stat-num { display: block; font-size: 44rpx; font-weight: 800; color: #fff; line-height: 1; }
.stat-warn { color: #FFD66B; }
.stat-label { display: block; font-size: 22rpx; color: rgba(200,210,255,0.6); margin-top: 8rpx; }
.stat-divider { width: 1rpx; height: 60rpx; background: rgba(255,255,255,0.15); }
.filter-hint { display: block; text-align: center; font-size: 20rpx; color: rgba(200,210,255,0.5); margin-top: 16rpx; }

/* Body */
.body { padding: 24rpx 28rpx; }

/* Skeleton */
.skeleton-list {}
.skeleton-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.sk-line { height: 24rpx; background: #e9eef5; border-radius: 8rpx; margin-bottom: 14rpx; }
.sk-w60 { width: 60%; }
.sk-w40 { width: 40%; }
.sk-short { height: 18rpx; }

/* Empty */
.empty-state { text-align: center; padding: 100rpx 0; }
.empty-icon { display: block; font-size: 100rpx; margin-bottom: 24rpx; }
.empty-text { display: block; font-size: 32rpx; font-weight: 600; color: #374151; }
.empty-sub { display: block; font-size: 24rpx; color: #9CA3AF; margin-top: 8rpx; }

/* Leave Cards */
.leave-card { background: #fff; border-radius: 20rpx; margin-bottom: 20rpx; overflow: hidden; display: flex; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05); }
.card-departed { opacity: 0.8; }
.card-left-bar { width: 8rpx; flex-shrink: 0; }
.bar-blue { background: linear-gradient(180deg, #2B7FFF, #00B8D9); }
.bar-amber { background: linear-gradient(180deg, #F59F00, #E8C068); }
.bar-gray { background: #D1D5DB; }

.card-body { flex: 1; padding: 24rpx; }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16rpx; }
.student-name { display: block; font-size: 30rpx; font-weight: 700; color: #111827; }
.class-tag { display: block; font-size: 20rpx; color: #9CA3AF; margin-top: 4rpx; }
.badges { display: flex; flex-direction: column; align-items: flex-end; gap: 8rpx; }
.badge { font-size: 20rpx; padding: 5rpx 16rpx; border-radius: 30rpx; white-space: nowrap; }
.badge-green { background: #D1FAE5; color: #059669; }
.badge-amber { background: #FEF3C7; color: #D97706; }
.badge-blue { background: #DBEAFE; color: #2563EB; }
.badge-gray { background: #F3F4F6; color: #9CA3AF; }

.card-info { display: flex; gap: 0; flex-direction: column; gap: 8rpx; margin-bottom: 16rpx; }
.info-item { display: flex; align-items: center; gap: 12rpx; }
.info-icon { font-size: 24rpx; }
.info-val { font-size: 24rpx; color: #4B5563; }

.card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 16rpx; border-top: 1rpx solid #F3F4F6; }
.leave-no { font-size: 20rpx; color: #CBD5E1; }
.ft-right { display: flex; align-items: center; gap: 12rpx; }
.sig-tag { font-size: 20rpx; color: #6366f1; background: #EEF2FF; padding: 4rpx 12rpx; border-radius: 20rpx; }
.depart-time { font-size: 20rpx; color: #9CA3AF; }

/* 刷脸放行按钮 */
.card-action { display: flex; align-items: center; gap: 10rpx; margin-top: 16rpx; padding: 18rpx 24rpx;
  background: linear-gradient(135deg, #1947C8, #2B7FFF); border-radius: 14rpx; }
.ca-icon { font-size: 30rpx; }
.ca-text { flex: 1; font-size: 28rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.ca-arrow { font-size: 36rpx; color: rgba(255,255,255,0.7); }
</style>
