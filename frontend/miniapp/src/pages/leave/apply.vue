<template>
  <view class="page">
    <!-- Hero Header -->
    <view class="hero">
      <view class="hero-bg-orb1"></view>
      <view class="hero-bg-orb2"></view>
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @click="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <view class="title-wrap">
            <text class="hero-title">学生请假申请</text>
            <text class="hero-sub">在线申请 · 班主任审批 · 门卫核验</text>
          </view>
          <view style="width:64rpx;"></view>
        </view>
      </view>
    </view>

    <!-- 表单卡片 -->
    <scroll-view scroll-y class="form-scroll">
      <view class="form-card">

        <!-- 为谁请假 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">为谁请假</text>
            <text class="title-req">*</text>
          </view>
          <view v-if="children.length === 0 && childrenLoaded" class="no-child">
            <text class="no-child-icon">👶</text>
            <text class="no-child-text">暂无关联学生，请联系管理员绑定</text>
          </view>
          <view v-else class="child-list">
            <view
              v-for="c in children"
              :key="c.studentId"
              class="child-chip"
              :class="{ 'chip-active': form.studentId === c.studentId }"
              @click="pickChild(c)"
            >
              <view class="chip-avatar" :class="{ 'ca-active': form.studentId === c.studentId }">
                <text class="ca-initial">{{ c.studentName?.charAt(0) }}</text>
              </view>
              <text class="chip-name">{{ c.studentName }}</text>
              <view v-if="form.studentId === c.studentId" class="chip-check">✓</view>
            </view>
          </view>
        </view>

        <!-- 请假类型 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">请假类型</text>
          </view>
          <view class="type-row">
            <view
              class="type-card"
              :class="{ 'type-active': form.leaveType === 'SICK' }"
              @click="form.leaveType = 'SICK'"
            >
              <text class="type-emoji">🤒</text>
              <text class="type-label">病假</text>
              <view v-if="form.leaveType === 'SICK'" class="type-check">✓</view>
            </view>
            <view
              class="type-card"
              :class="{ 'type-active': form.leaveType === 'PERSONAL' }"
              @click="form.leaveType = 'PERSONAL'"
            >
              <text class="type-emoji">📌</text>
              <text class="type-label">事假</text>
              <view v-if="form.leaveType === 'PERSONAL'" class="type-check">✓</view>
            </view>
          </view>
        </view>

        <!-- 请假原因 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">请假原因</text>
            <text class="title-req">*</text>
          </view>
          <view class="textarea-wrap">
            <textarea
              v-model="form.reason"
              placeholder="请详细说明请假原因，以便老师审批…"
              class="field-textarea"
              :maxlength="300"
            />
            <text class="char-count">{{ form.reason.length }}/300</text>
          </view>
        </view>

        <!-- 离校时间 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">计划离校时间</text>
            <text class="title-req">*</text>
          </view>
          <picker mode="dateTime" :value="form.leaveStart" @change="e => form.leaveStart = e.detail.value">
            <view class="date-btn">
              <view class="date-btn-left">
                <text class="date-icon">📅</text>
                <text class="date-text" :class="{ 'date-placeholder': !form.leaveStart }">
                  {{ form.leaveStart || '点击选择离校日期时间' }}
                </text>
              </view>
              <text class="date-arrow">›</text>
            </view>
          </picker>
        </view>

        <!-- 返校时间 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">预计返校时间</text>
            <text class="title-req">*</text>
          </view>
          <picker mode="dateTime" :value="form.leaveEnd" @change="e => form.leaveEnd = e.detail.value">
            <view class="date-btn">
              <view class="date-btn-left">
                <text class="date-icon">🔙</text>
                <text class="date-text" :class="{ 'date-placeholder': !form.leaveEnd }">
                  {{ form.leaveEnd || '点击选择返校日期时间' }}
                </text>
              </view>
              <text class="date-arrow">›</text>
            </view>
          </picker>
        </view>

        <!-- 凭证照片 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">凭证照片</text>
            <text class="title-opt">（选填）</text>
          </view>
          <view class="photo-area" @click="choosePhoto">
            <image v-if="form.proofPhotoUrl" :src="form.proofPhotoUrl" class="proof-img" mode="aspectFill" />
            <view v-else class="photo-placeholder">
              <view class="pp-icon-wrap">
                <text class="pp-icon">+</text>
              </view>
              <text class="pp-label">上传就医证明</text>
              <text class="pp-sub">照片 · 最大 5MB</text>
            </view>
          </view>
        </view>

      </view>

      <!-- 提交按钮 -->
      <view class="submit-area">
        <view class="submit-btn" :class="{ 'btn-loading': loading }" @click="submit">
          <text class="submit-text">{{ loading ? '提交中…' : '提交申请' }}</text>
        </view>
        <text class="submit-tip">提交后将推送通知给班主任审批</text>
      </view>

      <view style="height:60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { request } from '../../api/request';

const roleCode = uni.getStorageSync('ycd_roleCode') || 'PARENT';
const children = ref([]);
const childrenLoaded = ref(false);
const loading = ref(false);

const form = reactive({
  studentId: null,
  studentName: '',
  studentNo: '',
  classId: null,
  className: '',
  leaveType: 'SICK',
  reason: '',
  leaveStart: '',
  leaveEnd: '',
  proofPhotoUrl: '',
  applicantRole: (roleCode === 'TEACHER' || roleCode === 'HEAD_TEACHER') ? 'TEACHER' : 'PARENT'
});

const pickChild = (c) => {
  form.studentId = c.studentId;
  form.studentName = c.studentName;
  form.studentNo = c.studentNo || '';
};

onMounted(async () => {
  try {
    const list = await request({ url: '/permission/my-students' }) || [];
    children.value = list;
    if (list.length === 1) pickChild(list[0]);
  } catch (e) {
    uni.showToast({ title: e.message || '加载关联学生失败', icon: 'none' });
  } finally {
    childrenLoaded.value = true;
  }
});

const choosePhoto = () => {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
    success: (res) => { form.proofPhotoUrl = res.tempFilePaths[0]; }
  });
};

const submit = async () => {
  if (!form.studentId) { uni.showToast({ title: '请选择请假学生', icon: 'none' }); return; }
  if (!form.reason.trim()) { uni.showToast({ title: '请填写请假原因', icon: 'none' }); return; }
  if (!form.leaveStart) { uni.showToast({ title: '请选择离校时间', icon: 'none' }); return; }
  if (!form.leaveEnd) { uni.showToast({ title: '请选择返校时间', icon: 'none' }); return; }
  loading.value = true;
  try {
    await request({ url: '/leave/applications', method: 'POST', data: form });
    uni.showToast({ title: '✓ 申请已提交，等待班主任审批', icon: 'none', duration: 2500 });
    setTimeout(() => uni.navigateBack(), 2500);
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.page { background: #F0F4FA; min-height: 100vh; }

/* Hero */
.hero {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #06133D 0%, #1225A0 60%, #2B6CFF 100%);
  padding: 60rpx 32rpx 48rpx;
  border-radius: 0 0 40rpx 40rpx;
}
.hero-bg-orb1 { position: absolute; width: 400rpx; height: 400rpx; background: rgba(21,200,224,0.2); filter: blur(60rpx); border-radius: 50%; top: -140rpx; right: -100rpx; }
.hero-bg-orb2 { position: absolute; width: 300rpx; height: 300rpx; background: rgba(232,192,104,0.1); filter: blur(50rpx); border-radius: 50%; bottom: -100rpx; left: -60rpx; }
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; }
.back-btn { width: 64rpx; height: 64rpx; background: rgba(255,255,255,0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 52rpx; color: #fff; }
.title-wrap { flex: 1; text-align: center; }
.hero-title { display: block; font-size: 34rpx; font-weight: 700; color: #fff; }
.hero-sub { display: block; font-size: 22rpx; color: rgba(255,255,255,0.6); margin-top: 6rpx; }

/* Form */
.form-scroll { flex: 1; }
.form-card { background: #fff; margin: -28rpx 24rpx 0; border-radius: 28rpx; padding: 32rpx; box-shadow: 0 8rpx 40rpx rgba(15,40,100,0.08); position: relative; }

.field-section { margin-bottom: 36rpx; }
.section-title { display: flex; align-items: center; gap: 10rpx; margin-bottom: 16rpx; }
.title-dot { width: 6rpx; height: 28rpx; background: linear-gradient(180deg, #1947C8, #2B7FFF); border-radius: 3rpx; }
.title-text { font-size: 28rpx; font-weight: 600; color: #1E293B; }
.title-req { font-size: 26rpx; color: #EF4444; }
.title-opt { font-size: 22rpx; color: #94A3B8; }

/* 孩子列表 */
.no-child { display: flex; align-items: center; gap: 12rpx; background: #FFFBEB; padding: 20rpx; border-radius: 14rpx; border: 1rpx solid #FDE68A; }
.no-child-icon { font-size: 32rpx; }
.no-child-text { font-size: 24rpx; color: #92400E; }
.child-list { display: flex; gap: 16rpx; flex-wrap: wrap; }
.child-chip {
  display: flex; align-items: center; gap: 10rpx; padding: 16rpx 24rpx;
  background: #F8FAFC; border: 1rpx solid #E2E8F0;
  border-radius: 50rpx; transition: all 0.2s;
}
.chip-active { background: linear-gradient(135deg, #EFF6FF, #DBEAFE); border-color: #2B7FFF; }
.chip-avatar { width: 44rpx; height: 44rpx; border-radius: 50%; background: #E2E8F0; display: flex; align-items: center; justify-content: center; }
.ca-active { background: linear-gradient(135deg, #1947C8, #2B7FFF); }
.ca-initial { font-size: 22rpx; font-weight: 700; color: #64748B; }
.ca-active .ca-initial { color: #fff; }
.chip-name { font-size: 26rpx; color: #1E293B; font-weight: 500; }
.chip-active .chip-name { color: #1947C8; font-weight: 600; }
.chip-check { font-size: 22rpx; color: #1947C8; font-weight: 700; }

/* 请假类型 */
.type-row { display: flex; gap: 20rpx; }
.type-card {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 10rpx;
  padding: 24rpx 0; background: #F8FAFC;
  border: 2rpx solid #E2E8F0; border-radius: 20rpx;
  position: relative; transition: all 0.2s;
}
.type-active { background: linear-gradient(135deg, #EFF6FF, #DBEAFE); border-color: #2B7FFF; }
.type-emoji { font-size: 48rpx; }
.type-label { font-size: 26rpx; color: #374151; font-weight: 500; }
.type-active .type-label { color: #1947C8; font-weight: 600; }
.type-check { position: absolute; top: 12rpx; right: 12rpx; width: 36rpx; height: 36rpx; background: #2B7FFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20rpx; color: #fff; }

/* Textarea */
.textarea-wrap { position: relative; }
.field-textarea {
  width: 100%; min-height: 160rpx; padding: 20rpx;
  background: #F8FAFC; border: 1rpx solid #E2E8F0;
  border-radius: 16rpx; font-size: 28rpx; color: #1E293B;
  box-sizing: border-box; line-height: 1.6;
}
.char-count { position: absolute; right: 16rpx; bottom: 14rpx; font-size: 20rpx; color: #CBD5E1; }

/* Date Button */
.date-btn { display: flex; align-items: center; background: #F8FAFC; border: 1rpx solid #E2E8F0; border-radius: 16rpx; padding: 0 24rpx; height: 88rpx; }
.date-btn-left { display: flex; align-items: center; gap: 14rpx; flex: 1; }
.date-icon { font-size: 30rpx; }
.date-text { font-size: 28rpx; color: #1E293B; }
.date-placeholder { color: #CBD5E1; }
.date-arrow { font-size: 44rpx; color: #CBD5E1; }

/* Photo */
.photo-area { width: 200rpx; height: 200rpx; border-radius: 20rpx; overflow: hidden; border: 2rpx dashed #CBD5E1; }
.proof-img { width: 100%; height: 100%; }
.photo-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; }
.pp-icon-wrap { width: 64rpx; height: 64rpx; border-radius: 50%; background: #EEF2F8; display: flex; align-items: center; justify-content: center; }
.pp-icon { font-size: 40rpx; color: #94A3B8; }
.pp-label { font-size: 22rpx; color: #64748B; }
.pp-sub { font-size: 18rpx; color: #CBD5E1; }

/* Submit */
.submit-area { padding: 24rpx; }
.submit-btn {
  height: 96rpx; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1225A0 0%, #1947C8 50%, #2B7FFF 100%);
  border-radius: 24rpx; box-shadow: 0 12rpx 32rpx rgba(25,71,200,0.4);
  transition: opacity 0.2s;
}
.btn-loading { opacity: 0.7; }
.submit-text { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 4rpx; }
.submit-tip { display: block; text-align: center; font-size: 22rpx; color: #94A3B8; margin-top: 14rpx; }
</style>
