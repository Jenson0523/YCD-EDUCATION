<template>
  <view class="page">
    <!-- 顶部提示 -->
    <view class="info-bar">
      <text class="info-text">📅 {{ today }} · {{ realName }}的报备</text>
    </view>

    <view class="section">
      <!-- 作息情况 -->
      <view class="field-group">
        <view class="field-label">作息情况</view>
        <view class="options-row">
          <view
            v-for="opt in sleepOptions"
            :key="opt"
            class="opt-chip"
            :class="{ active: form.sleepStatus === opt }"
            @click="form.sleepStatus = opt"
          >{{ opt }}</view>
        </view>
      </view>

      <!-- 情绪状态 -->
      <view class="field-group">
        <view class="field-label">情绪状态</view>
        <view class="options-row">
          <view
            v-for="opt in emotionOptions"
            :key="opt"
            class="opt-chip"
            :class="{ active: form.emotionStatus === opt }"
            @click="form.emotionStatus = opt"
          >{{ opt }}</view>
        </view>
      </view>

      <!-- 居家学习 -->
      <view class="field-group">
        <view class="field-label">居家学习</view>
        <view class="options-row">
          <view
            v-for="opt in studyOptions"
            :key="opt"
            class="opt-chip"
            :class="{ active: form.studyStatus === opt }"
            @click="form.studyStatus = opt"
          >{{ opt }}</view>
        </view>
      </view>

      <!-- 特殊情况 -->
      <view class="field-group">
        <view class="field-label">特殊情况 <text class="optional">（选填）</text></view>
        <textarea
          v-model="form.familySpecialSituation"
          placeholder="家庭特殊情况、需要老师特别关注的问题…"
          class="textarea"
        />
      </view>

      <!-- 外出报备 -->
      <view class="field-group">
        <view class="field-label">外出报备 <text class="optional">（选填）</text></view>
        <textarea
          v-model="form.outgoingReport"
          placeholder="如需外出，请填写时间、地点及陪同人员"
          class="textarea"
        />
      </view>

      <button class="submit-btn" :disabled="loading" @click="submit">
        {{ loading ? '提交中…' : '提交报备' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { request } from '../../api/request';

const today = new Date().toISOString().slice(0, 10);
const realName = uni.getStorageSync('ycd_realName') || '家长';
const userId = uni.getStorageSync('ycd_userId');

const sleepOptions = ['正常', '熬夜', '睡眠不足', '生病休息'];
const emotionOptions = ['稳定', '愉快', '焦虑', '低落', '烦躁'];
const studyOptions = ['主动自觉', '需督促', '未完成', '因病未学'];

const loading = ref(false);
const form = reactive({
  reportDate: today,
  sleepStatus: '',
  emotionStatus: '',
  studyStatus: '',
  familySpecialSituation: '',
  outgoingReport: ''
});

async function submit() {
  if (!form.sleepStatus || !form.emotionStatus || !form.studyStatus) {
    uni.showToast({ title: '请选择作息、情绪和学习状态', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    await request({ url: '/fs/home-reports', method: 'POST', data: { ...form } });
    uni.showToast({ title: '报备已提交 ✓', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 1500);
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.info-bar {
  background: #1b5ea6;
  padding: 20rpx 32rpx;
}
.info-text { font-size: 24rpx; color: rgba(255,255,255,0.85); }

.section { padding: 24rpx 32rpx; }

.field-group { margin-bottom: 32rpx; }

.field-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #374151;
  margin-bottom: 14rpx;
}

.optional { font-size: 22rpx; color: #9ca3af; font-weight: 400; }

.options-row { display: flex; flex-wrap: wrap; gap: 16rpx; }

.opt-chip {
  padding: 12rpx 28rpx;
  background: #f3f4f6;
  border: 1rpx solid #e5e7eb;
  border-radius: 50rpx;
  font-size: 26rpx;
  color: #6b7280;
}

.opt-chip.active {
  background: #1b5ea6;
  border-color: #1b5ea6;
  color: #fff;
}

.textarea {
  width: 100%;
  min-height: 140rpx;
  padding: 16rpx 20rpx;
  background: #f9fafb;
  border: 1rpx solid #d1d5db;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #111827;
  box-sizing: border-box;
}

.submit-btn {
  width: 100%;
  height: 92rpx;
  background: #1b5ea6;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
  margin-top: 16rpx;
}

.submit-btn[disabled] { opacity: 0.55; }
</style>
