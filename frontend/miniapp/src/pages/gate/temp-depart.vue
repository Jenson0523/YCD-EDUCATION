<template>
  <view class="page">
    <!-- Header -->
    <view class="hero">
      <view class="hero-content">
        <view class="hero-top">
          <view class="back-btn" @click="uni.navigateBack()">
            <text class="back-icon">‹</text>
          </view>
          <view class="title-wrap">
            <text class="hero-title">临时紧急放行</text>
            <text class="hero-sub">需在 24 小时内补批审核</text>
          </view>
          <view style="width:60rpx;"></view>
        </view>
      </view>
      <!-- 警示条 -->
      <view class="warn-strip">
        <text class="warn-icon">⚠</text>
        <text class="warn-text">此操作将生成补批工单，班主任须在截止时间前审批</text>
      </view>
    </view>

    <!-- 表单 -->
    <scroll-view scroll-y>
      <view class="form-wrap">

        <!-- 学籍号 -->
        <view class="field-group">
          <text class="field-label">学籍号 <text class="req">*</text></text>
          <view class="input-wrap">
            <text class="input-prefix-icon">🎓</text>
            <input
              v-model="form.studentNo"
              placeholder="输入学生学籍号"
              class="field-input"
              @blur="autoFill"
            />
            <view v-if="filling" class="input-loading">...</view>
          </view>
        </view>

        <!-- 学生信息回显 -->
        <view v-if="form.studentName" class="student-hint">
          <view class="sh-avatar">
            <text class="sh-initial">{{ form.studentName?.charAt(0) }}</text>
          </view>
          <view class="sh-info">
            <text class="sh-name">{{ form.studentName }}</text>
            <text class="sh-class">{{ form.className }}</text>
          </view>
          <view class="sh-check">✓ 已确认</view>
        </view>

        <!-- 请假原因 -->
        <view class="field-group">
          <text class="field-label">离校原因 <text class="req">*</text></text>
          <textarea
            v-model="form.reason"
            placeholder="请填写临时紧急离校原因…"
            class="field-textarea"
            :maxlength="200"
          />
          <text class="char-count">{{ form.reason.length }}/200</text>
        </view>

        <!-- 返校时间 -->
        <view class="field-group">
          <text class="field-label">预计返校时间</text>
          <view class="datetime-row">
            <picker mode="date" :value="endDate" :start="todayStr" @change="e => endDate = e.detail.value">
              <view class="date-picker-btn">
                <text class="dp-icon">📅</text>
                <text class="dp-text">{{ endDate || '选择日期' }}</text>
              </view>
            </picker>
            <picker mode="time" :value="endTime" @change="e => endTime = e.detail.value">
              <view class="date-picker-btn">
                <text class="dp-icon">🕐</text>
                <text class="dp-text">{{ endTime || '选择时间' }}</text>
              </view>
            </picker>
          </view>
          <text class="field-hint">选填，不填默认离校后8小时</text>
        </view>

        <!-- 备注 -->
        <view class="field-group">
          <text class="field-label">门卫备注</text>
          <view class="input-wrap">
            <text class="input-prefix-icon">📝</text>
            <input v-model="form.approveRemark" placeholder="门卫说明（选填）" class="field-input" />
          </view>
        </view>

        <!-- 提交按钮 -->
        <view class="submit-area">
          <view class="submit-btn" :class="{ 'btn-loading': loading }" @click="submit">
            <text v-if="!loading" class="submit-icon">⚡</text>
            <text class="submit-text">{{ loading ? '登记中…' : '确认临时放行' }}</text>
          </view>
          <text class="submit-tip">放行后将自动生成补批工单通知班主任</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, ref, watch, onMounted } from 'vue';
import { request } from '../../api/request';

// 角色守卫：仅门卫可进入
onMounted(() => {
  const roleCode = uni.getStorageSync('ycd_roleCode') || '';
  if (roleCode !== 'GATE') {
    uni.showToast({ title: '仅门卫可操作', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 1500);
  }
});

const loading = ref(false);
const filling = ref(false);

function formatDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
const todayStr = formatDate(new Date());
const endDate = ref('');
const endTime = ref('');

const form = reactive({
  studentNo: '', studentName: '', className: '', classId: null,
  leaveType: 'PERSONAL', reason: '', leaveEnd: '', approveRemark: ''
});

watch([endDate, endTime], () => {
  form.leaveEnd = (endDate.value && endTime.value)
    ? `${endDate.value} ${endTime.value}:00` : '';
});

const autoFill = async () => {
  if (!form.studentNo || form.studentName) return;
  filling.value = true;
  try {
    const stuData = await request({ url: `/stu/students?keyword=${form.studentNo}&pageSize=5` });
    const stu = stuData?.records?.find(s => s.studentNo === form.studentNo) || stuData?.records?.[0];
    if (stu) {
      form.studentName = stu.name;
      form.className = stu.className || '';
      form.classId = stu.classId;
    }
  } catch {}
  filling.value = false;
};

const submit = async () => {
  if (!form.studentNo.trim()) { uni.showToast({ title: '请填写学籍号', icon: 'none' }); return; }
  if (!form.reason.trim()) { uni.showToast({ title: '请填写离校原因', icon: 'none' }); return; }
  loading.value = true;
  try {
    await request({ url: '/leave/applications/temp-depart', method: 'POST', data: form });
    uni.showToast({ title: '已临时放行 ✓ 补批工单已生成', icon: 'none', duration: 2500 });
    setTimeout(() => uni.navigateBack(), 2500);
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' });
  } finally { loading.value = false; }
};
</script>

<style scoped>
.page { background: #F5F7FB; min-height: 100vh; }

.hero {
  background: linear-gradient(145deg, #7C1B00, #C04A00, #E07200);
  padding: 56rpx 32rpx 0;
  border-radius: 0 0 36rpx 36rpx;
}
.hero-content { position: relative; }
.hero-top { display: flex; align-items: center; margin-bottom: 24rpx; }
.back-btn { width: 64rpx; height: 64rpx; background: rgba(255,255,255,0.15); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.back-icon { font-size: 52rpx; color: #fff; }
.title-wrap { flex: 1; text-align: center; }
.hero-title { display: block; font-size: 34rpx; font-weight: 700; color: #fff; }
.hero-sub { display: block; font-size: 22rpx; color: rgba(255,255,255,0.65); margin-top: 4rpx; }

.warn-strip {
  display: flex; align-items: center; gap: 12rpx;
  background: rgba(0,0,0,0.2);
  padding: 18rpx 28rpx;
  margin: 0 -32rpx;
  border-radius: 0 0 36rpx 36rpx;
}
.warn-icon { font-size: 26rpx; color: #FFD700; flex-shrink: 0; }
.warn-text { font-size: 22rpx; color: rgba(255,255,255,0.85); }

.form-wrap { padding: 28rpx; }

.field-group { margin-bottom: 28rpx; }
.field-label { display: block; font-size: 28rpx; font-weight: 600; color: #1E293B; margin-bottom: 12rpx; }
.req { color: #EF4444; }

.input-wrap { display: flex; align-items: center; background: #fff; border: 1rpx solid #E2E8F0; border-radius: 16rpx; padding: 0 20rpx; height: 88rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03); }
.input-prefix-icon { font-size: 30rpx; margin-right: 14rpx; flex-shrink: 0; }
.field-input { flex: 1; height: 88rpx; font-size: 28rpx; color: #1E293B; background: transparent; }
.input-loading { font-size: 26rpx; color: #94A3B8; }

.student-hint { display: flex; align-items: center; gap: 20rpx; background: #EFF6FF; border: 1rpx solid #BFDBFE; border-radius: 16rpx; padding: 20rpx; margin-top: 12rpx; margin-bottom: 8rpx; }
.sh-avatar { width: 72rpx; height: 72rpx; border-radius: 18rpx; background: linear-gradient(135deg, #1D4ED8, #2B7FFF); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sh-initial { font-size: 32rpx; font-weight: 700; color: #fff; }
.sh-info { flex: 1; }
.sh-name { display: block; font-size: 30rpx; font-weight: 700; color: #1E293B; }
.sh-class { display: block; font-size: 22rpx; color: #64748B; margin-top: 4rpx; }
.sh-check { font-size: 22rpx; color: #059669; font-weight: 600; }

.field-textarea {
  width: 100%; min-height: 160rpx; padding: 20rpx;
  background: #fff; border: 1rpx solid #E2E8F0;
  border-radius: 16rpx; font-size: 28rpx; color: #1E293B;
  box-sizing: border-box;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.char-count { display: block; text-align: right; font-size: 20rpx; color: #CBD5E1; margin-top: 8rpx; }

.datetime-row { display: flex; gap: 16rpx; }
.datetime-row picker { flex: 1; }
.date-picker-btn { display: flex; align-items: center; background: #fff; border: 1rpx solid #E2E8F0; border-radius: 16rpx; padding: 0 20rpx; height: 88rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03); }
.dp-icon { font-size: 28rpx; margin-right: 12rpx; }
.dp-text { flex: 1; font-size: 26rpx; color: #374151; }
.dp-arrow { font-size: 40rpx; color: #CBD5E1; }
.field-hint { display: block; font-size: 20rpx; color: #94A3B8; margin-top: 10rpx; }

.submit-area { margin-top: 16rpx; }
.submit-btn {
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #B45309, #D97706, #FBBF24);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(180,83,9,0.4);
}
.submit-btn.btn-loading { opacity: 0.7; }
.submit-icon { font-size: 36rpx; }
.submit-text { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.submit-tip { display: block; text-align: center; font-size: 22rpx; color: #94A3B8; margin-top: 16rpx; }
</style>
