<template>
  <view class="page">
    <view class="bg-layer"><view class="bg-grid"></view><view class="bg-glow"></view></view>
    <view :style="{ height: statusBarH + 'px' }"></view>

    <!-- Header -->
    <view class="header">
      <view class="hd-back" hover-class="hd-hover" @click="goBack">‹</view>
      <view class="hd-center">
        <text class="hd-title">人脸信息录入</text>
        <text class="hd-sub">{{ stepTip }}</text>
      </view>
      <view style="width:64rpx"></view>
    </view>

    <!-- 步骤指示 -->
    <view class="steps">
      <view class="step" :class="{ active: step >= 1, done: step > 1 }">
        <view class="step-dot">{{ step > 1 ? '✓' : '1' }}</view>
        <text class="step-label">阅读同意书</text>
      </view>
      <view class="step-line" :class="{ done: step > 1 }"></view>
      <view class="step" :class="{ active: step >= 2, done: step > 2 }">
        <view class="step-dot">{{ step > 2 ? '✓' : '2' }}</view>
        <text class="step-label">选择学生</text>
      </view>
      <view class="step-line" :class="{ done: step > 2 }"></view>
      <view class="step" :class="{ active: step >= 3 }">
        <view class="step-dot">3</view>
        <text class="step-label">采集人脸</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll">

      <!-- ════ 第1步：告知同意书 ════ -->
      <view v-if="step === 1" class="consent-section">
        <view class="doc-card">
          <text class="doc-title">{{ doc.title || '加载中…' }}</text>
          <text class="doc-version mono">版本 {{ doc.version }}</text>
          <view class="doc-divider"></view>
          <scroll-view scroll-y class="doc-body" @scrolltolower="readToEnd = true">
            <view v-for="(s, i) in doc.sections" :key="i" class="doc-section">
              <text class="doc-text">{{ s }}</text>
            </view>
            <view class="doc-footer">
              <text class="doc-footer-text">{{ doc.footer }}</text>
            </view>
          </scroll-view>
        </view>

        <view class="consent-check" @click="agreed = !agreed">
          <view class="checkbox" :class="{ checked: agreed }">
            <text v-if="agreed" class="check-mark">✓</text>
          </view>
          <text class="check-text">我已完整阅读并同意《{{ doc.title }}》，自愿为孩子/学生采集人脸信息用于离校安全核验</text>
        </view>

        <view class="primary-btn" :class="{ disabled: !agreed }" hover-class="btn-hover" @click="confirmConsent">
          <text class="btn-text">我已阅读并同意，下一步</text>
        </view>
        <view class="refuse-link" @click="goBack">不同意，返回</view>
      </view>

      <!-- ════ 第2步：选择学生 ════ -->
      <view v-if="step === 2" class="select-section">
        <!-- 家长：绑定孩子chips -->
        <template v-if="isParent">
          <view class="sec-label"><view class="sec-bar"></view><text class="sec-text">选择孩子</text></view>
          <view v-if="children.length === 0" class="empty-tip">暂无绑定孩子，请联系学校管理员</view>
          <view v-else class="child-list">
            <view
              v-for="c in children" :key="c.studentId"
              class="child-card" :class="{ picked: form.studentId === c.studentId }"
              hover-class="cc-hover" @click="pickStudent(c)"
            >
              <view class="cc-avatar"><text class="cc-initial">{{ c.studentName?.charAt(0) }}</text></view>
              <view class="cc-info">
                <text class="cc-name">{{ c.studentName }}</text>
                <text class="cc-meta mono">{{ c.studentNo }} · {{ c.className || '—' }}</text>
              </view>
              <view v-if="form.studentId === c.studentId" class="cc-check">✓</view>
            </view>
          </view>
        </template>

        <!-- 教师：搜索选人 -->
        <template v-else>
          <view class="sec-label"><view class="sec-bar"></view><text class="sec-text">搜索学生（限所辖班级）</text></view>
          <view class="search-wrap">
            <text class="search-icon">🔍</text>
            <input v-model="searchKw" placeholder="输入姓名或学籍号" class="search-input" @input="onSearch" />
          </view>
          <view v-if="searchResults.length" class="result-list">
            <view
              v-for="s in searchResults" :key="s.studentId"
              class="child-card" :class="{ picked: form.studentId === s.studentId }"
              hover-class="cc-hover" @click="pickStudent(s)"
            >
              <view class="cc-avatar"><text class="cc-initial">{{ s.studentName?.charAt(0) }}</text></view>
              <view class="cc-info">
                <text class="cc-name">{{ s.studentName }}</text>
                <text class="cc-meta mono">{{ s.studentNo }} · {{ s.className || '—' }}</text>
              </view>
              <view v-if="form.studentId === s.studentId" class="cc-check">✓</view>
            </view>
          </view>
        </template>

        <!-- 已有档案提示 -->
        <view v-if="form.studentId && existStatus === 'has'" class="exist-tip">
          ℹ️ 该学生已有人脸档案，继续操作将<text class="warn">更新覆盖</text>原照片
        </view>

        <view class="primary-btn" :class="{ disabled: !form.studentId }" hover-class="btn-hover" @click="step = 3">
          <text class="btn-text">下一步：采集人脸</text>
        </view>
      </view>

      <!-- ════ 第3步：采集人脸 ════ -->
      <view v-if="step === 3" class="capture-section">
        <view class="picked-banner">
          <text class="pb-name">{{ form.studentName }}</text>
          <text class="pb-meta mono">{{ form.studentNo }} · {{ form.className || '—' }}</text>
        </view>

        <view class="photo-frame" @click="takePhoto">
          <image v-if="photoLocal" :src="photoLocal" class="photo-img" mode="aspectFill" />
          <view v-else class="photo-empty">
            <text class="pe-icon">📷</text>
            <text class="pe-text">点击拍摄 / 选择正脸照片</text>
            <text class="pe-tip">光线充足 · 正面免冠 · 单人</text>
          </view>
          <view class="corner c-tl"></view><view class="corner c-tr"></view>
          <view class="corner c-bl"></view><view class="corner c-br"></view>
        </view>

        <view class="privacy-note">
          🔒 照片仅存学校服务器，访问受权限控制，比对在本地离线完成
        </view>

        <view class="primary-btn" :class="{ disabled: !photoLocal || submitting }" hover-class="btn-hover" @click="submit">
          <text class="btn-text">{{ submitting ? '提交中…' : '确认提交录入' }}</text>
        </view>
      </view>

      <!-- ════ 完成 ════ -->
      <view v-if="step === 4" class="done-section">
        <view class="done-icon">✓</view>
        <text class="done-title">人脸录入成功</text>
        <text class="done-sub">已同步至学校人脸库，门卫端即刻可用</text>
        <view class="done-info">
          <view class="di-row"><text class="di-k">学生</text><text class="di-v">{{ form.studentName }}</text></view>
          <view class="di-row"><text class="di-k">授权版本</text><text class="di-v mono">{{ doc.version }}</text></view>
          <view class="di-row"><text class="di-k">授权时间</text><text class="di-v mono">{{ doneTime }}</text></view>
        </view>
        <view class="primary-btn" hover-class="btn-hover" @click="goBack">
          <text class="btn-text">完 成</text>
        </view>
      </view>

      <view style="height:60rpx"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { request, uploadFile } from '../../api/request';

const statusBarH = ref(20);
const step = ref(1);
const doc = ref({ sections: [] });
const agreed = ref(false);
const readToEnd = ref(false);

const roleCode = uni.getStorageSync('ycd_roleCode') || 'PARENT';
const isParent = computed(() => roleCode === 'PARENT' || roleCode === 'STUDENT');

const children = ref([]);
const searchKw = ref('');
const searchResults = ref([]);
let searchTimer = null;

const existStatus = ref(''); // has / none
const photoLocal = ref('');
const submitting = ref(false);
const doneTime = ref('');
const backToApply = ref(false);

const form = reactive({ studentId: null, studentNo: '', studentName: '', classId: null, className: '' });

const stepTip = computed(() => ({
  1: '请先阅读《告知同意书》', 2: '选择要录入的学生', 3: '拍摄清晰正脸照片', 4: '录入完成'
}[step.value]));

onMounted(async () => {
  try {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : uni.getSystemInfoSync();
    statusBarH.value = info.statusBarHeight || 20;
  } catch {}
  // 同意书
  try { doc.value = await request({ url: '/leave/face/consent-doc' }); } catch {}
  // 家长预载孩子
  if (isParent.value) {
    try { children.value = await request({ url: '/permission/selectable-students' }) || []; } catch {}
  }
  // 从请假页跳入：预选学生
  const pages = getCurrentPages();
  const opts = pages[pages.length - 1]?.options || {};
  if (opts.studentId) {
    form.studentId = opts.studentId;
    form.studentNo = decodeURIComponent(opts.studentNo || '');
    form.studentName = decodeURIComponent(opts.studentName || '');
    form.classId = opts.classId || null;
    form.className = decodeURIComponent(opts.className || '');
    backToApply.value = opts.back === '1';
  }
});

const confirmConsent = () => {
  if (!agreed.value) { uni.showToast({ title: '请先勾选同意', icon: 'none' }); return; }
  // 已预选学生则直接到采集步
  step.value = form.studentId ? 3 : 2;
};

const pickStudent = async (s) => {
  form.studentId = s.studentId;
  form.studentNo = s.studentNo || '';
  form.studentName = s.studentName || '';
  form.classId = s.classId || null;
  form.className = s.className || '';
  existStatus.value = '';
  try {
    const rec = await request({ url: `/leave/face/by-no/${form.studentNo}` });
    existStatus.value = rec && rec.facePhotoUrl ? 'has' : 'none';
  } catch { existStatus.value = 'none'; }
};

const onSearch = () => {
  if (searchTimer) clearTimeout(searchTimer);
  const kw = searchKw.value.trim();
  if (!kw) { searchResults.value = []; return; }
  searchTimer = setTimeout(async () => {
    try {
      searchResults.value = await request({
        url: `/permission/selectable-students?keyword=${encodeURIComponent(kw)}`
      }) || [];
    } catch { searchResults.value = []; }
  }, 350);
};

const takePhoto = () => {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['camera', 'album'],
    success: (res) => { photoLocal.value = res.tempFilePaths[0]; }
  });
};

const submit = async () => {
  if (!photoLocal.value || submitting.value) return;
  submitting.value = true;
  try {
    const up = await uploadFile(photoLocal.value, 'face');
    await request({
      url: '/leave/face', method: 'POST',
      data: {
        studentId: form.studentId,
        studentNo: form.studentNo,
        realName: form.studentName,
        classId: form.classId,
        className: form.className,
        facePhotoUrl: up.url,
        consentAgreed: true,
        consentVersion: doc.value.version
      }
    });
    const d = new Date();
    doneTime.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
    step.value = 4;
    try { uni.vibrateShort(); } catch {}
  } catch (e) {
    uni.showToast({ title: e.message || '录入失败', icon: 'none', duration: 2500 });
  } finally {
    submitting.value = false;
  }
};

const goBack = () => uni.navigateBack();
</script>

<style scoped>
.page { min-height: 100vh; background: #04081C; position: relative; overflow: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(43,127,255,0.05) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(43,127,255,0.05) 1rpx, transparent 1rpx); background-size: 48rpx 48rpx; mask-image: linear-gradient(180deg,#000,transparent 70%); }
.bg-glow { position: absolute; width: 520rpx; height: 520rpx; border-radius: 50%; filter: blur(90rpx); background: rgba(0,229,255,0.14); top: -120rpx; right: -150rpx; }
.scroll { position: relative; z-index: 1; height: calc(100vh - 180rpx); }

.header { position: relative; z-index: 2; display: flex; align-items: center; padding: 16rpx 32rpx 12rpx; }
.hd-back { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center; font-size: 52rpx; color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 12rpx; }
.hd-hover { background: rgba(0,229,255,0.12); }
.hd-center { flex: 1; text-align: center; }
.hd-title { display: block; font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }
.hd-sub { display: block; font-size: 20rpx; color: rgba(0,229,255,0.6); margin-top: 4rpx; }

/* 步骤 */
.steps { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; padding: 16rpx 60rpx 8rpx; }
.step { display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.step-dot { width: 52rpx; height: 52rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.06); border: 1rpx solid rgba(255,255,255,0.15); color: rgba(255,255,255,0.4); font-size: 24rpx; font-weight: 700; }
.step.active .step-dot { border-color: #00E5FF; color: #00E5FF; box-shadow: 0 0 14rpx rgba(0,229,255,0.4); }
.step.done .step-dot { background: rgba(0,229,255,0.15); }
.step-label { font-size: 20rpx; color: rgba(255,255,255,0.4); }
.step.active .step-label { color: #00E5FF; }
.step-line { width: 80rpx; height: 1rpx; background: rgba(255,255,255,0.15); margin: 0 12rpx 30rpx; }
.step-line.done { background: rgba(0,229,255,0.5); }

/* 同意书 */
.consent-section { padding: 16rpx 36rpx 0; }
.doc-card { background: rgba(255,255,255,0.05); border: 1rpx solid rgba(255,255,255,0.1); border-radius: 20rpx; padding: 30rpx; }
.doc-title { display: block; font-size: 32rpx; font-weight: 800; color: #fff; text-align: center; }
.doc-version { display: block; font-size: 20rpx; color: rgba(0,229,255,0.6); text-align: center; margin-top: 8rpx; }
.doc-divider { height: 1rpx; background: rgba(255,255,255,0.1); margin: 24rpx 0; }
.doc-body { height: 540rpx; }
.doc-section { margin-bottom: 26rpx; }
.doc-text { font-size: 25rpx; color: rgba(255,255,255,0.78); line-height: 1.8; white-space: pre-wrap; }
.doc-footer { padding: 20rpx; background: rgba(0,229,255,0.06); border-radius: 12rpx; }
.doc-footer-text { font-size: 23rpx; color: #00E5FF; line-height: 1.6; }

.consent-check { display: flex; gap: 16rpx; margin-top: 28rpx; align-items: flex-start; }
.checkbox { width: 40rpx; height: 40rpx; border-radius: 10rpx; border: 2rpx solid rgba(255,255,255,0.3); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s; margin-top: 4rpx; }
.checkbox.checked { background: #00E5FF; border-color: #00E5FF; }
.check-mark { font-size: 26rpx; color: #04081C; font-weight: 900; }
.check-text { flex: 1; font-size: 24rpx; color: rgba(255,255,255,0.7); line-height: 1.6; }

.primary-btn { margin-top: 32rpx; height: 100rpx; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #00C2D6, #2B7FFF); border-radius: 18rpx; box-shadow: 0 12rpx 30rpx rgba(0,194,214,0.35); }
.btn-hover { opacity: 0.85; }
.primary-btn.disabled { opacity: 0.4; box-shadow: none; }
.btn-text { font-size: 31rpx; font-weight: 800; color: #fff; letter-spacing: 4rpx; }
.refuse-link { text-align: center; margin-top: 24rpx; font-size: 24rpx; color: rgba(255,255,255,0.35); }

/* 选学生 */
.select-section { padding: 16rpx 36rpx 0; }
.sec-label { display: flex; align-items: center; gap: 14rpx; margin-bottom: 20rpx; }
.sec-bar { width: 6rpx; height: 28rpx; background: linear-gradient(180deg,#00E5FF,#2B7FFF); border-radius: 3rpx; box-shadow: 0 0 10rpx rgba(0,229,255,0.5); }
.sec-text { font-size: 28rpx; font-weight: 700; color: #fff; }
.empty-tip { padding: 40rpx; text-align: center; font-size: 24rpx; color: rgba(255,255,255,0.4);
  background: rgba(255,255,255,0.04); border-radius: 16rpx; }
.child-list, .result-list { display: flex; flex-direction: column; gap: 16rpx; }
.child-card { display: flex; align-items: center; gap: 20rpx; padding: 24rpx;
  background: rgba(255,255,255,0.04); border: 1rpx solid rgba(255,255,255,0.08); border-radius: 16rpx; }
.child-card.picked { background: rgba(0,229,255,0.07); border-color: rgba(0,229,255,0.4); }
.cc-hover { background: rgba(0,229,255,0.12); }
.cc-avatar { width: 80rpx; height: 80rpx; border-radius: 16rpx; background: linear-gradient(135deg, rgba(43,127,255,0.3), rgba(0,229,255,0.2));
  border: 1rpx solid rgba(0,229,255,0.3); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cc-initial { font-size: 34rpx; font-weight: 800; color: #fff; }
.cc-info { flex: 1; }
.cc-name { display: block; font-size: 30rpx; font-weight: 700; color: #fff; }
.cc-meta { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 6rpx; }
.cc-check { font-size: 34rpx; color: #00E5FF; font-weight: 900; }
.search-wrap { display: flex; align-items: center; gap: 12rpx; background: rgba(255,255,255,0.06);
  border: 1rpx solid rgba(255,255,255,0.12); border-radius: 16rpx; padding: 0 24rpx; height: 88rpx; margin-bottom: 20rpx; }
.search-icon { font-size: 28rpx; }
.search-input { flex: 1; height: 88rpx; font-size: 28rpx; color: #fff; }
.exist-tip { margin-top: 20rpx; padding: 18rpx 24rpx; background: rgba(232,192,104,0.08);
  border: 1rpx solid rgba(232,192,104,0.3); border-radius: 14rpx; font-size: 23rpx; color: rgba(232,192,104,0.9); }
.exist-tip .warn { color: #E8C068; font-weight: 700; }

/* 采集 */
.capture-section { padding: 16rpx 36rpx 0; }
.picked-banner { text-align: center; margin-bottom: 24rpx; }
.pb-name { display: block; font-size: 36rpx; font-weight: 800; color: #fff; }
.pb-meta { display: block; font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 6rpx; }
.photo-frame { position: relative; width: 440rpx; height: 440rpx; margin: 0 auto; border-radius: 24rpx; overflow: hidden;
  background: radial-gradient(circle at center, #0A1A40, #04081C); border: 1rpx solid rgba(0,229,255,0.25); }
.photo-img { width: 100%; height: 100%; }
.photo-empty { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12rpx; }
.pe-icon { font-size: 70rpx; }
.pe-text { font-size: 26rpx; color: rgba(255,255,255,0.6); }
.pe-tip { font-size: 20rpx; color: rgba(0,229,255,0.5); }
.corner { position: absolute; width: 40rpx; height: 40rpx; border-color: #00E5FF; border-style: solid; }
.c-tl { top: 16rpx; left: 16rpx; border-width: 4rpx 0 0 4rpx; border-radius: 8rpx 0 0 0; }
.c-tr { top: 16rpx; right: 16rpx; border-width: 4rpx 4rpx 0 0; border-radius: 0 8rpx 0 0; }
.c-bl { bottom: 16rpx; left: 16rpx; border-width: 0 0 4rpx 4rpx; border-radius: 0 0 0 8rpx; }
.c-br { bottom: 16rpx; right: 16rpx; border-width: 0 4rpx 4rpx 0; border-radius: 0 0 8rpx 0; }
.privacy-note { margin-top: 24rpx; padding: 18rpx 24rpx; background: rgba(0,230,150,0.06);
  border: 1rpx solid rgba(0,230,150,0.25); border-radius: 14rpx; font-size: 22rpx; color: rgba(0,230,150,0.85); text-align: center; }

/* 完成 */
.done-section { padding: 60rpx 36rpx 0; display: flex; flex-direction: column; align-items: center; }
.done-icon { width: 140rpx; height: 140rpx; border-radius: 50%; background: rgba(0,230,150,0.15);
  border: 2rpx solid rgba(0,230,150,0.5); display: flex; align-items: center; justify-content: center;
  font-size: 70rpx; font-weight: 900; color: #00E696; box-shadow: 0 0 40rpx rgba(0,230,150,0.3); }
.done-title { margin-top: 32rpx; font-size: 38rpx; font-weight: 800; color: #fff; }
.done-sub { margin-top: 12rpx; font-size: 24rpx; color: rgba(255,255,255,0.45); }
.done-info { width: 100%; margin-top: 40rpx; background: rgba(255,255,255,0.04);
  border: 1rpx solid rgba(255,255,255,0.08); border-radius: 16rpx; padding: 10rpx 28rpx; }
.di-row { display: flex; justify-content: space-between; padding: 18rpx 0; border-bottom: 1rpx solid rgba(255,255,255,0.05); }
.di-row:last-child { border-bottom: none; }
.di-k { font-size: 24rpx; color: rgba(255,255,255,0.4); }
.di-v { font-size: 24rpx; color: #fff; }
.mono { font-family: 'Courier New', monospace; }
.done-section .primary-btn { width: 100%; }
</style>
