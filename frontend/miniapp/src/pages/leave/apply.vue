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
            <text class="title-text">{{ isParent ? '为哪个孩子请假' : '为哪位学生请假' }}</text>
            <text class="title-req">*</text>
            <text v-if="!isParent" class="title-mode">代请假</text>
          </view>

          <!-- 家长/学生：仅可选已绑定孩子 -->
          <template v-if="isParent">
            <view v-if="children.length === 0 && childrenLoaded" class="no-child">
              <text class="no-child-icon">👶</text>
              <text class="no-child-text">暂无关联孩子，请联系管理员绑定</text>
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
          </template>

          <!-- 教师/管理员：可搜索选择学生（代请假） -->
          <template v-else>
            <!-- 已选学生展示 -->
            <view v-if="form.studentId" class="picked-student" @click="clearPicked">
              <view class="ps-avatar"><text class="ps-initial">{{ form.studentName?.charAt(0) }}</text></view>
              <view class="ps-info">
                <text class="ps-name">{{ form.studentName }}</text>
                <text class="ps-meta">{{ form.studentNo }} · {{ form.className || '—' }}</text>
              </view>
              <text class="ps-change">更换</text>
            </view>
            <!-- 搜索框 -->
            <view v-else class="search-box">
              <view class="search-input-wrap">
                <text class="search-icon">🔍</text>
                <input
                  v-model="searchKw"
                  placeholder="输入学生姓名或学籍号搜索"
                  class="search-input"
                  @input="onSearchInput"
                />
                <text v-if="searching" class="search-loading">…</text>
              </view>
              <!-- 搜索结果 -->
              <view v-if="searchResults.length > 0" class="search-results">
                <view
                  v-for="s in searchResults"
                  :key="s.studentId"
                  class="result-item"
                  @click="pickStudent(s)"
                >
                  <view class="ri-avatar"><text class="ri-initial">{{ s.studentName?.charAt(0) }}</text></view>
                  <view class="ri-info">
                    <text class="ri-name">{{ s.studentName }}</text>
                    <text class="ri-meta">{{ s.studentNo }} · {{ s.className || '—' }}</text>
                  </view>
                </view>
              </view>
              <view v-else-if="searchKw && !searching" class="search-empty">
                未找到匹配的学生
              </view>
            </view>
          </template>
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
          <view class="datetime-row">
            <picker mode="date" :value="startDate" :start="todayStr" @change="e => startDate = e.detail.value">
              <view class="dt-btn">
                <text class="dt-icon">📅</text>
                <text class="dt-text" :class="{ 'dt-ph': !startDate }">{{ startDate || '选择日期' }}</text>
              </view>
            </picker>
            <picker mode="time" :value="startTime" @change="e => startTime = e.detail.value">
              <view class="dt-btn">
                <text class="dt-icon">🕐</text>
                <text class="dt-text" :class="{ 'dt-ph': !startTime }">{{ startTime || '选择时间' }}</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 返校时间 -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">预计返校时间</text>
            <text class="title-req">*</text>
          </view>
          <view class="datetime-row">
            <picker mode="date" :value="endDate" :start="todayStr" @change="e => endDate = e.detail.value">
              <view class="dt-btn">
                <text class="dt-icon">📅</text>
                <text class="dt-text" :class="{ 'dt-ph': !endDate }">{{ endDate || '选择日期' }}</text>
              </view>
            </picker>
            <picker mode="time" :value="endTime" @change="e => endTime = e.detail.value">
              <view class="dt-btn">
                <text class="dt-icon">🕐</text>
                <text class="dt-text" :class="{ 'dt-ph': !endTime }">{{ endTime || '选择时间' }}</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 凭证照片（病例，选填） -->
        <view class="field-section">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">凭证照片 / 病例</text>
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

        <!-- 人脸录入（必须，门卫核验离校用） -->
        <view class="field-section" v-if="form.studentId">
          <view class="section-title">
            <view class="title-dot"></view>
            <text class="title-text">人脸录入</text>
            <text class="title-req">*</text>
            <text class="title-hint">门卫核验离校必需</text>
          </view>

          <!-- 已有人脸档案 -->
          <view v-if="faceStatus === 'has'" class="face-enrolled">
            <image :src="facePhotoUrl" class="fe-photo" mode="aspectFill" />
            <view class="fe-info">
              <text class="fe-ok">✓ 已录入人脸档案</text>
              <text class="fe-sub">可正常通过门卫核验离校</text>
            </view>
            <view class="fe-reupload" @click="enrollFace">重新录入</view>
          </view>

          <!-- 未录入：需上传 -->
          <view v-else-if="faceStatus === 'none'" class="face-enroll-area">
            <view class="face-upload-box" @click="enrollFace">
              <image v-if="form.facePhotoUrl" :src="form.facePhotoUrl" class="fe-preview" mode="aspectFill" />
              <view v-else class="fe-placeholder">
                <text class="fe-icon">📷</text>
                <text class="fe-text">拍摄 / 上传人脸照片</text>
                <text class="fe-tip">清晰正脸 · 用于AI核验</text>
              </view>
            </view>
            <view class="face-warn">⚠️ 该学生尚未录入人脸，请拍照录入后方可离校核验</view>
          </view>

          <view v-else class="face-loading">检查人脸档案中…</view>
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
import { reactive, ref, computed, watch, onMounted } from 'vue';
import { request, uploadFile, assetUrl } from '../../api/request';

const roleCode = uni.getStorageSync('ycd_roleCode') || 'PARENT';
// 家长/学生 → 仅可选绑定孩子；教师/管理员 → 代请假可搜索选人
const isParent = computed(() => roleCode === 'PARENT' || roleCode === 'STUDENT');

const children = ref([]);
const childrenLoaded = ref(false);
const loading = ref(false);

// 教师代请假搜索
const searchKw = ref('');
const searchResults = ref([]);
const searching = ref(false);
let searchTimer = null;

// 日期/时间拆分选择（微信 picker 不支持 dateTime，需 date + time 组合）
const todayStr = formatDate(new Date());
const startDate = ref('');
const startTime = ref('');
const endDate = ref('');
const endTime = ref('');

// 人脸档案状态：checking / has / none
const faceStatus = ref('');
const facePhotoUrl = ref('');

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
  facePhotoUrl: '',
  applicantRole: (roleCode === 'TEACHER' || roleCode === 'HEAD_TEACHER') ? 'TEACHER' : 'PARENT'
});

function formatDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// 组合日期+时间 → form.leaveStart / leaveEnd（yyyy-MM-dd HH:mm:ss）
watch([startDate, startTime], () => {
  form.leaveStart = (startDate.value && startTime.value)
    ? `${startDate.value} ${startTime.value}:00` : '';
});
watch([endDate, endTime], () => {
  form.leaveEnd = (endDate.value && endTime.value)
    ? `${endDate.value} ${endTime.value}:00` : '';
});

// 选中学生后检查人脸档案
const checkFace = async (studentNo) => {
  if (!studentNo) { faceStatus.value = ''; return; }
  faceStatus.value = 'checking';
  facePhotoUrl.value = '';
  form.facePhotoUrl = '';
  try {
    const rec = await request({ url: `/leave/face/by-no/${studentNo}` });
    if (rec && rec.facePhotoUrl) {
      faceStatus.value = 'has';
      facePhotoUrl.value = assetUrl(rec.facePhotoUrl);
      form.facePhotoUrl = rec.facePhotoUrl;
    } else {
      faceStatus.value = 'none';
    }
  } catch {
    faceStatus.value = 'none';
  }
};

const pickChild = (c) => {
  form.studentId = c.studentId;
  form.studentName = c.studentName;
  form.studentNo = c.studentNo || '';
  form.classId = c.classId || null;
  form.className = c.className || '';
  checkFace(form.studentNo);
};

const pickStudent = (s) => {
  form.studentId = s.studentId;
  form.studentName = s.studentName;
  form.studentNo = s.studentNo || '';
  form.className = s.className || '';
  searchResults.value = [];
  searchKw.value = '';
  checkFace(form.studentNo);
};

const clearPicked = () => {
  form.studentId = null;
  form.studentName = '';
  form.studentNo = '';
  form.className = '';
  faceStatus.value = '';
};

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer);
  const kw = searchKw.value.trim();
  if (!kw) { searchResults.value = []; return; }
  searchTimer = setTimeout(async () => {
    searching.value = true;
    try {
      searchResults.value = await request({
        url: `/permission/selectable-students?keyword=${encodeURIComponent(kw)}`
      }) || [];
    } catch (e) {
      searchResults.value = [];
    } finally {
      searching.value = false;
    }
  }, 350);
};

onMounted(async () => {
  if (isParent.value) {
    try {
      const list = await request({ url: '/permission/selectable-students' }) || [];
      children.value = list;
      if (list.length === 1) pickChild(list[0]);
    } catch (e) {
      uni.showToast({ title: e.message || '加载关联孩子失败', icon: 'none' });
    } finally {
      childrenLoaded.value = true;
    }
  } else {
    childrenLoaded.value = true;
  }
});

const choosePhoto = () => {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempPath = res.tempFilePaths[0];
      uni.showLoading({ title: '上传中…' });
      try {
        const data = await uploadFile(tempPath, 'proof');
        form.proofPhotoUrl = assetUrl(data.url);
      } catch (e) {
        form.proofPhotoUrl = tempPath; // 兜底用本地预览
      } finally {
        uni.hideLoading();
      }
    }
  });
};

// 录入人脸（拍照上传 + 写入档案）
const enrollFace = () => {
  if (!form.studentId) return;
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['camera', 'album'],
    success: async (res) => {
      const tempPath = res.tempFilePaths[0];
      uni.showLoading({ title: '录入人脸中…' });
      try {
        const up = await uploadFile(tempPath, 'face');
        // 写入/更新人脸档案
        await request({
          url: '/leave/face', method: 'POST',
          data: {
            studentId: form.studentId,
            studentNo: form.studentNo,
            realName: form.studentName,
            classId: form.classId,
            className: form.className,
            facePhotoUrl: up.url
          }
        });
        form.facePhotoUrl = up.url;
        facePhotoUrl.value = assetUrl(up.url);
        faceStatus.value = 'has';
        uni.showToast({ title: '✓ 人脸录入成功', icon: 'none' });
      } catch (e) {
        uni.showToast({ title: e.message || '人脸录入失败', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    }
  });
};

const submit = async () => {
  if (!form.studentId) { uni.showToast({ title: '请选择请假学生', icon: 'none' }); return; }
  if (!form.reason.trim()) { uni.showToast({ title: '请填写请假原因', icon: 'none' }); return; }
  if (!form.leaveStart) { uni.showToast({ title: '请选择完整的离校日期和时间', icon: 'none' }); return; }
  if (!form.leaveEnd) { uni.showToast({ title: '请选择完整的返校日期和时间', icon: 'none' }); return; }
  if (faceStatus.value !== 'has' || !form.facePhotoUrl) {
    uni.showToast({ title: '请先为学生录入人脸（门卫核验离校必需）', icon: 'none', duration: 2500 });
    return;
  }
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
.title-mode { font-size: 20rpx; color: #1947C8; background: #EFF6FF; padding: 4rpx 14rpx; border-radius: 20rpx; margin-left: auto; }

/* 教师代请假：搜索选人 */
.picked-student { display: flex; align-items: center; gap: 18rpx; background: linear-gradient(135deg, #EFF6FF, #DBEAFE); border: 1rpx solid #2B7FFF; border-radius: 18rpx; padding: 20rpx; }
.ps-avatar { width: 72rpx; height: 72rpx; border-radius: 18rpx; background: linear-gradient(135deg, #1947C8, #2B7FFF); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ps-initial { font-size: 32rpx; font-weight: 700; color: #fff; }
.ps-info { flex: 1; }
.ps-name { display: block; font-size: 30rpx; font-weight: 700; color: #1947C8; }
.ps-meta { display: block; font-size: 22rpx; color: #64748B; margin-top: 4rpx; }
.ps-change { font-size: 24rpx; color: #2B7FFF; }

.search-box { position: relative; }
.search-input-wrap { display: flex; align-items: center; background: #F8FAFC; border: 1rpx solid #E2E8F0; border-radius: 16rpx; padding: 0 20rpx; height: 88rpx; }
.search-icon { font-size: 28rpx; margin-right: 12rpx; }
.search-input { flex: 1; height: 88rpx; font-size: 28rpx; color: #1E293B; }
.search-loading { font-size: 26rpx; color: #94A3B8; }
.search-results { margin-top: 12rpx; background: #fff; border: 1rpx solid #E2E8F0; border-radius: 16rpx; overflow: hidden; max-height: 480rpx; }
.result-item { display: flex; align-items: center; gap: 16rpx; padding: 20rpx; border-bottom: 1rpx solid #F1F5F9; }
.result-item:last-child { border-bottom: none; }
.result-item:active { background: #F8FAFC; }
.ri-avatar { width: 60rpx; height: 60rpx; border-radius: 16rpx; background: #E2E8F0; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ri-initial { font-size: 26rpx; font-weight: 700; color: #64748B; }
.ri-info { flex: 1; }
.ri-name { display: block; font-size: 28rpx; font-weight: 600; color: #1E293B; }
.ri-meta { display: block; font-size: 22rpx; color: #94A3B8; margin-top: 4rpx; }
.search-empty { margin-top: 12rpx; padding: 32rpx; text-align: center; font-size: 24rpx; color: #94A3B8; background: #F8FAFC; border-radius: 16rpx; }

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

/* Date+Time 拆分选择 */
.datetime-row { display: flex; gap: 16rpx; }
.datetime-row picker { flex: 1; }
.dt-btn { display: flex; align-items: center; gap: 12rpx; background: #F8FAFC; border: 1rpx solid #E2E8F0; border-radius: 16rpx; padding: 0 20rpx; height: 88rpx; }
.dt-icon { font-size: 28rpx; }
.dt-text { font-size: 26rpx; color: #1E293B; }
.dt-ph { color: #CBD5E1; }

/* Face enrollment */
.title-hint { font-size: 20rpx; color: #EA580C; background: #FFF7ED; padding: 4rpx 12rpx; border-radius: 20rpx; margin-left: auto; }
.face-enrolled { display: flex; align-items: center; gap: 18rpx; background: linear-gradient(135deg, #ECFDF5, #D1FAE5); border: 1rpx solid #6EE7B7; border-radius: 18rpx; padding: 20rpx; }
.fe-photo { width: 96rpx; height: 96rpx; border-radius: 16rpx; background: #fff; flex-shrink: 0; }
.fe-info { flex: 1; }
.fe-ok { display: block; font-size: 28rpx; font-weight: 700; color: #059669; }
.fe-sub { display: block; font-size: 22rpx; color: #10B981; margin-top: 4rpx; }
.fe-reupload { font-size: 24rpx; color: #059669; }
.face-enroll-area {}
.face-upload-box { width: 220rpx; height: 220rpx; border-radius: 20rpx; overflow: hidden; border: 2rpx dashed #FB923C; background: #FFF7ED; }
.fe-preview { width: 100%; height: 100%; }
.fe-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; }
.fe-icon { font-size: 56rpx; }
.fe-text { font-size: 24rpx; color: #EA580C; font-weight: 600; }
.fe-tip { font-size: 18rpx; color: #FB923C; }
.face-warn { margin-top: 14rpx; font-size: 22rpx; color: #EA580C; background: #FFF7ED; padding: 14rpx 18rpx; border-radius: 12rpx; }
.face-loading { padding: 24rpx; text-align: center; font-size: 24rpx; color: #94A3B8; background: #F8FAFC; border-radius: 16rpx; }

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
