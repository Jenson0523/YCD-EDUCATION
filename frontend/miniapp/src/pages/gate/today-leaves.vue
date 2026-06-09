<template>
  <view class="page">
    <view v-if="loading" class="center">加载中…</view>
    <view v-else-if="list.length === 0" class="center">今日暂无有效假条</view>
    <view v-else class="list">
      <view v-for="item in list" :key="item.id" class="card">
        <view class="card-header">
          <text class="sname">{{ item.studentName }}</text>
          <text class="class-name">{{ item.className }}</text>
          <view :class="['badge', item.isTemp ? 'badge-orange' : 'badge-green']">
            {{ item.isTemp ? '⚡临时' : '✅已批' }}
          </view>
        </view>
        <view class="row"><text class="lbl">类型</text><text>{{ item.leaveType === 'SICK' ? '病假' : '事假' }}</text></view>
        <view class="row"><text class="lbl">离校</text><text>{{ fmtDt(item.leaveStart) }}</text></view>
        <view class="row"><text class="lbl">返校</text><text>{{ fmtDt(item.leaveEnd) }}</text></view>
        <view class="row"><text class="lbl">状态</text>
          <text :class="item.departAt ? 'txt-gray' : 'txt-blue'">
            {{ item.departAt ? '已离校 ' + fmtDt(item.departAt) : '未离校' }}
          </text>
        </view>
        <view class="row"><text class="lbl">假条号</text><text class="leave-no">{{ item.leaveNo }}</text></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';
const list = ref([]);
const loading = ref(true);
onMounted(async () => {
  try {
    list.value = await request({ url: '/leave/applications/today-valid' }) || [];
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally { loading.value = false; }
});
const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
</script>

<style scoped>
.page { padding: 20rpx 28rpx; background: #f6f8fb; min-height: 100vh; }
.center { text-align: center; padding: 100rpx 0; color: #9ca3af; font-size: 28rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.card-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 16rpx; }
.sname { font-size: 30rpx; font-weight: 700; color: #111827; }
.class-name { font-size: 22rpx; color: #9ca3af; flex: 1; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 50rpx; }
.badge-green { background: #d1fae5; color: #059669; }
.badge-orange { background: #ffedd5; color: #ea580c; }
.row { display: flex; margin-bottom: 10rpx; font-size: 26rpx; }
.lbl { width: 80rpx; color: #9ca3af; }
.txt-blue { color: #2563eb; }
.txt-gray { color: #9ca3af; }
.leave-no { font-size: 22rpx; color: #9ca3af; }
</style>
