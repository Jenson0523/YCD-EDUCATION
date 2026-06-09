<template>
  <view class="page">
    <view class="warn-banner">
      <text class="warn-text">⚡ 临时紧急放行 - 需在24小时内补批</text>
    </view>
    <view class="section">
      <view class="field-group">
        <view class="field-label">学籍号 <text class="required">*</text></view>
        <input v-model="form.studentNo" placeholder="请输入学籍号" class="field-input" @blur="autoFill" />
      </view>
      <view v-if="form.studentName" class="student-hint">👤 {{ form.studentName }} · {{ form.className }}</view>

      <view class="field-group">
        <view class="field-label">请假原因 <text class="required">*</text></view>
        <textarea v-model="form.reason" placeholder="临时紧急离校原因…" class="textarea" />
      </view>
      <view class="field-group">
        <view class="field-label">预计返校时间</view>
        <picker mode="dateTime" :value="form.leaveEnd" @change="e => form.leaveEnd = e.detail.value">
          <view class="date-picker">{{ form.leaveEnd || '点击选择（选填）' }}</view>
        </picker>
      </view>
      <view class="field-group">
        <view class="field-label">备注</view>
        <input v-model="form.approveRemark" placeholder="门卫备注（选填）" class="field-input" />
      </view>

      <button class="submit-btn" :disabled="loading" @click="submit">
        {{ loading ? '登记中…' : '⚡ 确认临时放行' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { request } from '../../api/request';

const loading = ref(false);
const form = reactive({
  studentNo: '', studentName: '', className: '', classId: null,
  leaveType: 'PERSONAL', reason: '', leaveEnd: '', approveRemark: ''
});

const autoFill = async () => {
  if (!form.studentNo) return;
  try {
    const data = await request({ url: `/leave/face/by-student/${form.studentNo}` });
    if (data) { form.studentName = data.realName; form.className = data.className; form.classId = data.classId; }
  } catch {}
};

const submit = async () => {
  if (!form.studentNo || !form.reason) {
    uni.showToast({ title: '学籍号和原因必填', icon: 'none' }); return;
  }
  loading.value = true;
  try {
    await request({ url: '/leave/applications/temp-depart', method: 'POST', data: form });
    uni.showToast({ title: '已临时放行，补批工单已生成 ✓', icon: 'none', duration: 2500 });
    setTimeout(() => uni.navigateBack(), 2500);
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' });
  } finally { loading.value = false; }
};
</script>

<style scoped>
.warn-banner { background: #fff7ed; padding: 20rpx 32rpx; border-bottom: 1rpx solid #fed7aa; }
.warn-text { font-size: 26rpx; color: #ea580c; font-weight: 600; }
.section { padding: 24rpx 32rpx; }
.field-group { margin-bottom: 28rpx; }
.field-label { font-size: 28rpx; font-weight: 600; color: #374151; margin-bottom: 12rpx; }
.required { color: #ef4444; }
.field-input { width: 100%; height: 80rpx; padding: 0 20rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.student-hint { background: #eff6ff; padding: 16rpx 20rpx; border-radius: 10rpx; font-size: 26rpx; color: #1b5ea6; margin-bottom: 24rpx; }
.textarea { width: 100%; min-height: 120rpx; padding: 16rpx 20rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.date-picker { padding: 20rpx 24rpx; background: #f9fafb; border: 1rpx solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; color: #6b7280; }
.submit-btn { width: 100%; height: 92rpx; background: #ea580c; color: #fff; font-size: 32rpx; font-weight: 600; border-radius: 12rpx; border: none; margin-top: 8rpx; }
.submit-btn[disabled] { opacity: 0.55; }
</style>
