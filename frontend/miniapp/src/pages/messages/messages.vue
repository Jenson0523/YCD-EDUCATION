<template>
  <view class="page">
    <!-- Header -->
    <view class="hero">
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @tap="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <text class="hero-title">消息中心</text>
          <view class="mark-all" v-if="list.length > 0" @tap="readAll">
            <text class="mark-all-text">全部已读</text>
          </view>
          <view style="width:60rpx;" v-else></view>
        </view>
        <text class="hero-sub">仅显示与您相关的消息</text>
      </view>
    </view>

    <!-- 内容 -->
    <view class="content">
      <view v-if="loading" class="skeleton">
        <view class="sk-item" v-for="i in 3" :key="i">
          <view class="sk-icon"></view>
          <view class="sk-body">
            <view class="sk-line sk-w60"></view>
            <view class="sk-line sk-w80 sk-h14"></view>
          </view>
        </view>
      </view>

      <view v-else-if="list.length === 0" class="empty">
        <text class="empty-icon">📭</text>
        <text class="empty-title">暂无消息</text>
        <text class="empty-sub">请假审批通知、系统消息会出现在这里</text>
      </view>

      <view v-else class="list">
        <view
          v-for="msg in list" :key="msg.id"
          class="msg-card"
          :class="{ unread: msg.isRead === 0, urgent: msg.priority === 1 }"
          hover-class="msg-hover"
          @tap="openMsg(msg)"
        >
          <!-- 紧急标记条 -->
          <view v-if="msg.priority === 1" class="urgent-strip"></view>
          <view class="msg-left">
            <view class="msg-icon-box" :class="'biz-' + (msg.bizType || 'system')">
              <text class="msg-icon">{{ bizIcon(msg.bizType) }}</text>
            </view>
          </view>
          <view class="msg-body">
            <view class="msg-top">
              <text class="msg-title">
                <text v-if="msg.priority === 1" class="prio-tag prio-urgent">紧急</text>
                <text v-else-if="msg.priority === 2" class="prio-tag prio-important">重要</text>
                {{ msg.title }}
              </text>
              <text class="msg-time">{{ fmtTime(msg.createdAt) }}</text>
            </view>
            <text class="msg-content">{{ msg.content }}</text>
            <view class="msg-meta">
              <text class="msg-type">{{ bizLabel(msg.bizType) }}</text>
              <view v-if="msg.isRead === 0" class="msg-dot"></view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="hasMore" class="load-more" @tap="loadMore">
        <text class="load-text">加载更多</text>
      </view>

      <view style="height: 40rpx;"></view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { request } from '../../api/request';

const list = ref([]);
const loading = ref(true);
const pageNo = ref(1);
const hasMore = ref(false);

const load = async (append = false) => {
  if (!append) loading.value = true;
  try {
    const data = await request({
      url: `/sys/messages?pageNo=${pageNo.value}&pageSize=20`
    });
    const records = data?.records || [];
    if (append) {
      list.value = [...list.value, ...records];
    } else {
      list.value = records;
    }
    hasMore.value = records.length >= 20;
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const loadMore = () => {
  pageNo.value++;
  load(true);
};

const readAll = async () => {
  try {
    await request({ url: '/sys/messages/read-all', method: 'PUT' });
    list.value.forEach(m => m.isRead = 1);
    uni.showToast({ title: '已全部标为已读', icon: 'none' });
  } catch {}
};

const openMsg = async (msg) => {
  if (msg.isRead === 0) {
    try {
      await request({ url: `/sys/messages/${msg.id}/read`, method: 'PUT' });
      msg.isRead = 1;
    } catch {}
  }
  // 点击跳转到关联业务页面
  if (msg.bizType === 'LEAVE_APPLY' || msg.bizType === 'LEAVE_APPROVED' ||
      msg.bizType === 'LEAVE_REJECTED' || msg.bizType === 'TEMP_SUPPLEMENT') {
    // 根据 bizType 智能跳转：待审批→审批页，已审批→详情页
    if (msg.bizType === 'LEAVE_APPLY' || msg.bizType === 'TEMP_SUPPLEMENT') {
      uni.navigateTo({ url: '/pages/leave/approve' });
    } else {
      uni.navigateTo({ url: `/pages/leave/detail?id=${msg.bizId}` });
    }
  }
};

const bizIcon = (type) => {
  const map = { LEAVE_APPLY: '📝', LEAVE_APPROVED: '✅', LEAVE_REJECTED: '❌', TEMP_SUPPLEMENT: '⚡' };
  return map[type] || '📬';
};

const bizLabel = (type) => {
  const map = { LEAVE_APPLY: '待审批', LEAVE_APPROVED: '已批准', LEAVE_REJECTED: '已驳回', TEMP_SUPPLEMENT: '临时补批' };
  return map[type] || '系统消息';
};

const fmtTime = (dt) => {
  if (!dt) return '';
  return dt.replace('T', ' ').slice(0, 16);
};

onMounted(() => load());
</script>

<style scoped>
.page { min-height: 100vh; background: #F0F4FA; }

/* Hero */
.hero {
  background: linear-gradient(160deg, #1E2F50 0%, #2D4A7A 60%, #3B6CB5 100%);
  padding: 56rpx 32rpx 48rpx;
  border-radius: 0 0 32rpx 32rpx;
}
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; }
.back-btn { width: 64rpx; height: 64rpx; background: rgba(255,255,255,0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 52rpx; color: #fff; }
.hero-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 700; color: #fff; }
.mark-all { padding: 8rpx 20rpx; }
.mark-all-text { font-size: 24rpx; color: rgba(255,255,255,0.65); }
.hero-sub { display: block; text-align: center; font-size: 22rpx; color: rgba(255,255,255,0.35); margin-top: 10rpx; }

/* Content */
.content { padding: 24rpx 28rpx; }

/* Skeleton */
.sk-item { display: flex; gap: 18rpx; padding: 24rpx; background: #fff; border-radius: 16rpx; margin-bottom: 14rpx; }
.sk-icon { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #EEF2F8; flex-shrink: 0; }
.sk-body { flex: 1; display: flex; flex-direction: column; gap: 10rpx; }
.sk-line { height: 20rpx; background: #EEF2F8; border-radius: 6rpx; }
.sk-w60 { width: 60%; }
.sk-w80 { width: 80%; }
.sk-h14 { height: 14rpx; }

/* Empty */
.empty { text-align: center; padding: 100rpx 0; }
.empty-icon { display: block; font-size: 72rpx; margin-bottom: 18rpx; }
.empty-title { display: block; font-size: 28rpx; font-weight: 600; color: #64748B; }
.empty-sub { display: block; font-size: 24rpx; color: #94A3B8; margin-top: 8rpx; }

/* Message Card */
.msg-card {
  position: relative; display: flex; gap: 18rpx; padding: 24rpx;
  background: #fff; border-radius: 16rpx; margin-bottom: 14rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
  overflow: hidden;
}
.msg-card.unread { background: #F8FAFF; }
.msg-card.urgent { border-left: none; }
.msg-hover { background: #F8FAFC; }
.urgent-strip {
  position: absolute; left: 0; top: 0; bottom: 0; width: 6rpx;
  background: linear-gradient(180deg, #DC2626, #EF4444); border-radius: 0 3rpx 3rpx 0;
}
.msg-left { flex-shrink: 0; }
.msg-icon-box {
  width: 72rpx; height: 72rpx; border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center;
  background: #EEF2FF;
}
.msg-icon-box.biz-LEAVE_APPLY { background: #FFF7ED; }
.msg-icon-box.biz-LEAVE_APPROVED { background: #ECFDF5; }
.msg-icon-box.biz-LEAVE_REJECTED { background: #FEF2F2; }
.msg-icon-box.biz-TEMP_SUPPLEMENT { background: #FFFBEB; }
.msg-icon { font-size: 34rpx; }
.msg-body { flex: 1; min-width: 0; }
.msg-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12rpx; }
.msg-title { font-size: 28rpx; font-weight: 600; color: #1E293B; flex: 1; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.msg-time { font-size: 20rpx; color: #CBD5E1; flex-shrink: 0; margin-top: 4rpx; }
.msg-content { display: block; font-size: 24rpx; color: #64748B; margin-top: 8rpx; line-height: 1.5; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.msg-meta { display: flex; align-items: center; gap: 12rpx; margin-top: 12rpx; }
.msg-type { font-size: 20rpx; color: #94A3B8; background: #F1F5F9; padding: 2rpx 12rpx; border-radius: 20rpx; }
.msg-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #3B6CB5; }

/* Priority Tags */
.prio-tag { display: inline-block; font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; margin-right: 8rpx; vertical-align: middle; font-weight: 600; }
.prio-urgent { background: #FEF2F2; color: #DC2626; }
.prio-important { background: #FFFBEB; color: #D97706; }

.load-more { padding: 28rpx; text-align: center; }
.load-text { font-size: 26rpx; color: #3B6CB5; }
</style>
