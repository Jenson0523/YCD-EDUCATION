<template>
  <view class="page">
    <view class="info-bar">
      <text class="info-text">📋 {{ studentName }} · 请假申请</text>
    </view>

    <view class="section">
      <!-- 请假类型 -->
      <view class="field-group">
        <view class="field-label">请假类型</view>
        <view class="options-row">
          <view class="opt-chip" :class="{ active: form.leaveType === 'SICK' }" @click="form.leaveType='SICK'">🤒 病假</view>
          <view class="opt-chip" :class="{ active: form.leaveType === 'PERSONAL' }" @click="form.leaveType='PERSONAL'">📌 事假</view>
        </view>
      </view>

      <!-- 请假原因 -->
      <view class="field-group">
        <view class="field-label">请假原因 <text class="required">*</text></view>
        <textarea v-model="form.reason" placeholder="请详细填写请假原因…" class="textarea" />
      </view>

      <!-- 离校时间 -->
      <view class="field-group">
        <view class="field-label">计划离校时间 <text class="required">*</text></view>
        <picker mode="dateTime" :value="form.leaveStart" @change="e => form.leaveStart = e.detail.value">
          <view class="date-picker">{{ form.leaveStart || '点击选择日期时间' }}</view>
        </picker>
      </view>

      <!-- 返校时间 -->
      <view class="field-group">
        <view class="field-label">预计返校时间 <text class="required">*</text></view>
        <picker mode="dateTime" :value="form.leaveEnd" @change="e => form.leaveEnd = e.detail.value">
          <view class="date-picker">{{ form.leaveEnd || '点击选择日期时间' }}</view>
        </picker>
      </view>

      <!-- 凭证照片（选填） -->
      <view class="field-group">
        <view class="field-label">凭证照片 <text class="optional">（选填，如就医证明）</text></view>
        <view class="photo-area" @click="choosePhoto">
          <image v-if="form.proofPhotoUrl" :src="form.proofPhotoUrl" class="proof-img" mode="aspectFill" />
          <view v-else class="photo-placeholder">
            <text class="add-icon">+</text>
            <text class="add-tip">添加图片</text>
          </view>
        </view>
      </view>

      <button class="submit-btn" :disabled="loading" @click="submit">
        {{ loading ? '提交中…' : '提交申请' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { request } from '../../api/request';

const studentName = uni.getStorageSync('ycd_realName') || '学生';
const studentNo = uni.getStorageSync('ycd_studentNo') || '';
const classId = uni.getStorageSync('ycd_classId') || null;
const className = uni.getStorageSync('ycd_className') || '';

const loading = ref(false);
const form = reactive({
  leaveType: 'SICK',
  reason: '',
  leaveStart: '',
  leaveEnd: '',
  proofPhotoUrl: '',
  studentName,
  studentNo,
  classId,
  className,
  applicantRole: 'PARENT'
});

const choosePhoto = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempPath = res.tempFilePaths[0];
      // 实际项目需上传到服务器，这里用临时路径演示
      form.proofPhotoUrl = tempPath;
    }
  });
};

const submit = async () => {
  if (!form.reason.trim()) { uni.showToast({ title: '请填写请假原因', icon: 'none' }); return; }
  if (!form.leaveStart) { uni.showToast({ title: '请选择离校时间', icon: 'none' }); return; }
  if (!form.leaveEnd) { uni.showToast({ title: '请选择返校时间', icon: 'none' }); return; }
  loading.value = true;
  try {
    await request({ url: '/leave/applications', method: 'POST', data: form });
    uni.showToast({ title: '申请已提交，等待班主任审批 ✓', icon: 'none', duration: 2500 });
    setTimeout(() => uni.navigateBack(), 2500);
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.info-bar { background: #1b5ea6; padding: 20rpx 32rpx; }
.info-text { font-size: 24rpx; color: rgba(255,255,255,0.85); }
.section { padding: 24rpx 32rpx; }
.field-group { margin-bottom: 32rpx; }
.field-label { font-size: 28rpx; font-weight: 600; color: #374151; margin-bottom: 14rpx; }
.required { color: #ef4444; }
.optional { font-size: 22rpx; color: #9ca3af; font-weight: 400; }
.options-row { display: flex; gap: 20rpx; }
.opt-chip { padding: 12rpx 32rpx; background: #f3f4f6; border: 1rpx solid #e5e7eb; border-radius: 50rpx; font-size: 26rpx; color: #6b7280; }
.opt-chip.active { background: #1b5ea6; border-color: #1b5ea6; color: #fff; }
.textarea { width: 100%; min-height: 140rpx; padding: 16rpx 20rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.date-picker { padding: 20rpx 24rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; color: #374151; }
.photo-area { width: 160rpx; height: 160rpx; border: 2rpx dashed #d1d5db; border-radius: 12rpx; overflow: hidden; }
.proof-img { width: 100%; height: 100%; }
.photo-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; }
.add-icon { font-size: 48rpx; color: #9ca3af; }
.add-tip { font-size: 22rpx; color: #9ca3af; }
.submit-btn { width: 100%; height: 92rpx; background: #1b5ea6; color: #fff; font-size: 32rpx; font-weight: 600; border-radius: 12rpx; border: none; margin-top: 16rpx; }
.submit-btn[disabled] { opacity: 0.55; }
</style>
