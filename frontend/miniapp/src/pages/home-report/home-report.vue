<template>
  <view class="page">
    <view class="section">
      <view class="field">
        <text>学生ID</text>
        <input v-model="form.studentId" type="number" placeholder="请输入学生ID" />
      </view>
      <view class="field">
        <text>家长用户ID</text>
        <input v-model="form.parentUserId" type="number" placeholder="请输入家长用户ID" />
      </view>
      <view class="field">
        <text>报备日期</text>
        <input v-model="form.reportDate" placeholder="YYYY-MM-DD" />
      </view>
      <view class="field">
        <text>作息情况</text>
        <input v-model="form.sleepStatus" placeholder="如：正常 / 熬夜 / 睡眠不足" />
      </view>
      <view class="field">
        <text>情绪状态</text>
        <input v-model="form.emotionStatus" placeholder="如：稳定 / 焦虑 / 低落" />
      </view>
      <view class="field">
        <text>居家学习</text>
        <input v-model="form.studyStatus" placeholder="如：主动 / 需督促 / 未完成" />
      </view>
      <view class="field">
        <text>特殊情况</text>
        <textarea v-model="form.familySpecialSituation" placeholder="家庭特殊情况、需要老师关注的问题" />
      </view>
      <view class="field">
        <text>外出报备</text>
        <textarea v-model="form.outgoingReport" placeholder="外出时间、地点、陪同人" />
      </view>
      <button class="primary-button" @click="submit">提交报备</button>
    </view>
  </view>
</template>

<script setup>
import { reactive } from 'vue';
import { createHomeReport } from '../../api/homeReport';

const today = new Date().toISOString().slice(0, 10);
const form = reactive({
  studentId: '',
  parentUserId: '',
  reportDate: today,
  sleepStatus: '',
  emotionStatus: '',
  studyStatus: '',
  familySpecialSituation: '',
  outgoingReport: ''
});

async function submit() {
  try {
    await createHomeReport({
      ...form,
      studentId: Number(form.studentId),
      parentUserId: Number(form.parentUserId)
    });
    uni.showToast({ title: '已提交', icon: 'success' });
  } catch (error) {
    uni.showToast({ title: error.message || '提交失败', icon: 'none' });
  }
}
</script>

<style scoped>
.field {
  margin-bottom: 24rpx;
}

.field text {
  display: block;
  margin-bottom: 10rpx;
  font-size: 26rpx;
  color: #4b5563;
}

input,
textarea {
  width: 100%;
  min-height: 76rpx;
  padding: 0 18rpx;
  background: #f9fafb;
  border: 1rpx solid #d1d5db;
  border-radius: 12rpx;
  font-size: 28rpx;
}

textarea {
  min-height: 140rpx;
  padding-top: 18rpx;
}
</style>
