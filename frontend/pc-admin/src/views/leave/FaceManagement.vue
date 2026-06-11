<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">人脸档案管理</h1>
      <el-button type="primary" @click="openCreate">+ 新增档案</el-button>
    </div>
    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="姓名/学籍号" clearable style="width:200px" @keyup.enter="load" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:120px">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-select v-model="gradeFilter" placeholder="年级" clearable style="width:120px">
          <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <!-- 人脸照片：放大预览 -->
        <el-table-column label="人脸照片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.facePhotoUrl"
              :src="resolveUrl(row.facePhotoUrl)"
              :preview-src-list="[resolveUrl(row.facePhotoUrl)]"
              :preview-teleported="true"
              fit="cover"
              style="width:52px;height:52px;border-radius:8px;cursor:pointer"
            />
            <el-avatar v-else :size="44" :icon="UserFilled" />
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学籍号" width="130" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="gradeName" label="年级" width="90">
          <template #default="{ row }">{{ row.gradeName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="110">
          <template #default="{ row }">{{ row.className || '—' }}</template>
        </el-table-column>
        <el-table-column prop="headTeacherName" label="班主任" width="100">
          <template #default="{ row }">{{ row.headTeacherName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="previewPhoto(row)">查看照片</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                       @click="toggleStatus(row)">
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确认删除该档案？" confirm-button-text="删除"
                           @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNo" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @change="load" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑人脸档案' : '新增人脸档案'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <!-- 学生选择（仅新增） -->
        <el-form-item v-if="!editId" label="选择学生" required>
          <el-select
            v-model="selectedStudentId"
            filterable remote reserve-keyword
            placeholder="输入学籍号或姓名搜索"
            :remote-method="searchStudents"
            :loading="searchLoading"
            style="width:100%"
            @change="onStudentPick"
          >
            <el-option
              v-for="s in studentOptions"
              :key="s.studentId"
              :label="`${s.studentName}（${s.studentNo}）${s.className ? ' · ' + s.className : ''}`"
              :value="s.studentId"
            />
          </el-select>
          <div class="form-tip">支持按学籍号或学生姓名模糊搜索，选择后自动填充信息</div>
        </el-form-item>

        <el-form-item label="学籍号">
          <el-input v-model="form.studentNo" disabled placeholder="选择学生后自动填充" />
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="form.realName" :disabled="!editId && !!selectedStudentId" placeholder="选择学生后自动填充" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="年级">
              <el-input v-model="form.gradeName" placeholder="如：三年级" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班主任">
              <el-input v-model="form.headTeacherName" placeholder="班主任姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="班级名称">
          <el-input v-model="form.className" placeholder="班级名称" />
        </el-form-item>

        <!-- 人脸照片上传 -->
        <el-form-item label="人脸照片">
          <div class="face-upload-area">
            <el-upload
              class="face-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              :data="{ category: 'face' }"
              name="file"
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeUpload"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
            >
              <div v-if="form.facePhotoUrl" class="preview-wrap">
                <img :src="resolveUrl(form.facePhotoUrl)" class="face-preview" />
                <div class="preview-mask">
                  <el-icon><Camera /></el-icon>
                  <span>点击更换</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">上传人脸照片</div>
                <div class="upload-sub">jpg/png · 最大 5MB</div>
              </div>
            </el-upload>
            <!-- 已有照片大图预览 -->
            <div v-if="form.facePhotoUrl" class="photo-preview-btn" @click="previewCurrentPhoto">
              <el-icon><ZoomIn /></el-icon>
              <span>点击查看大图</span>
            </div>
            <!-- 预留：手机刷脸采集 -->
            <div class="face-scan-card" @click="comingSoon">
              <el-icon class="scan-icon"><Iphone /></el-icon>
              <div class="scan-text">手机刷脸采集</div>
              <el-tag size="small" type="info" effect="plain">即将上线</el-tag>
            </div>
          </div>
          <div class="form-tip">上传清晰正脸照片，用于门卫端 AI 人脸比对核验</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canSave" :loading="saving" @click="handleSave">
          {{ editId ? '保存修改' : '确定新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 照片大图预览弹窗 -->
    <el-dialog v-model="photoVisible" title="人脸照片" width="480px" align-center>
      <div style="text-align:center">
        <img :src="previewUrl" style="max-width:100%;max-height:60vh;border-radius:12px" />
        <p style="margin-top:12px;color:#606266;font-size:14px">{{ previewTitle }}</p>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { UserFilled, Plus, Camera, Iphone, ZoomIn } from '@element-plus/icons-vue';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const statusFilter = ref('');
const gradeFilter = ref('');
const gradeOptions = ref([]);
const dialogVisible = ref(false);
const editId = ref(null);
const saving = ref(false);
const form = ref({
  studentId: null, studentNo: '', realName: '', className: '',
  gradeName: '', headTeacherName: '', facePhotoUrl: ''
});

// 学生搜索
const selectedStudentId = ref(null);
const studentOptions = ref([]);
const searchLoading = ref(false);

// 照片预览
const photoVisible = ref(false);
const previewUrl = ref('');
const previewTitle = ref('');

// 上传配置
const uploadAction = '/api/upload/image';
const uploadHeaders = computed(() => ({ Authorization: localStorage.getItem('ycd_token') || '' }));

const canSave = computed(() => {
  if (editId.value) return !!form.value.studentNo;
  return !!form.value.studentNo && !!form.value.facePhotoUrl;
});

const resolveUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  return url;
};

const load = async () => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value };
  if (keyword.value) params.keyword = keyword.value;
  if (statusFilter.value) params.status = statusFilter.value;
  const data = await http.get('/leave/face', { params });
  rows.value = data?.records || [];
  total.value = data?.total || 0;
  // 收集年级选项
  const grades = new Set();
  rows.value.forEach(r => { if (r.gradeName) grades.add(r.gradeName); });
  gradeOptions.value = [...grades].sort();
};

onMounted(load);

const searchStudents = async (kw) => {
  if (!kw) { studentOptions.value = []; return; }
  searchLoading.value = true;
  try {
    studentOptions.value = await http.get('/permission/selectable-students', { params: { keyword: kw } }) || [];
  } catch {
    studentOptions.value = [];
  } finally {
    searchLoading.value = false;
  }
};

const onStudentPick = (id) => {
  const s = studentOptions.value.find(x => x.studentId === id);
  if (s) {
    form.value.studentId = s.studentId;
    form.value.studentNo = s.studentNo;
    form.value.realName = s.studentName;
    form.value.className = s.className || '';
    form.value.gradeName = s.gradeName || '';
  }
};

const openCreate = () => {
  editId.value = null;
  selectedStudentId.value = null;
  studentOptions.value = [];
  form.value = { studentId: null, studentNo: '', realName: '', className: '', gradeName: '', headTeacherName: '', facePhotoUrl: '' };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  selectedStudentId.value = null;
  form.value = {
    studentId: row.studentId, studentNo: row.studentNo,
    realName: row.realName, className: row.className || '',
    gradeName: row.gradeName || '', headTeacherName: row.headTeacherName || '',
    facePhotoUrl: row.facePhotoUrl || ''
  };
  dialogVisible.value = true;
};

// 照片预览
const previewPhoto = (row) => {
  previewUrl.value = resolveUrl(row.facePhotoUrl);
  previewTitle.value = `${row.realName}（${row.studentNo}）${row.className ? ' · ' + row.className : ''}`;
  photoVisible.value = true;
};

const previewCurrentPhoto = () => {
  previewUrl.value = resolveUrl(form.value.facePhotoUrl);
  previewTitle.value = `${form.value.realName}（${form.value.studentNo}）`;
  photoVisible.value = true;
};

const beforeUpload = (file) => {
  const isImg = file.type.startsWith('image/');
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isImg) { ElMessage.error('只能上传图片文件'); return false; }
  if (!isLt5M) { ElMessage.error('图片大小不能超过 5MB'); return false; }
  return true;
};

const onUploadSuccess = (resp) => {
  if (resp?.code === 0 && resp.data?.url) {
    form.value.facePhotoUrl = resp.data.url;
    ElMessage.success('照片上传成功');
  } else {
    ElMessage.error(resp?.message || '上传失败');
  }
};

const onUploadError = () => {
  ElMessage.error('照片上传失败，请重试');
};

const comingSoon = () => {
  ElMessage.info('手机刷脸采集功能即将上线，敬请期待');
};

const handleSave = async () => {
  if (!canSave.value) {
    ElMessage.warning('请完善必要信息');
    return;
  }
  saving.value = true;
  try {
    if (editId.value) {
      await http.put(`/leave/face/${editId.value}`, form.value);
      ElMessage.success('档案已更新');
    } else {
      await http.post('/leave/face', form.value);
      ElMessage.success('档案创建成功');
    }
    dialogVisible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e?.message || '操作失败');
  } finally {
    saving.value = false;
  }
};

const toggleStatus = async (row) => {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  await http.put(`/leave/face/${row.id}/status`, { status: newStatus });
  ElMessage.success('已更新');
  load();
};

const handleDelete = async (row) => {
  try {
    await http.delete(`/leave/face/${row.id}`);
    ElMessage.success('档案已删除');
    load();
  } catch (e) {
    ElMessage.error(e?.message || '删除失败');
  }
};
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 600; color: #1f2937; margin: 0; }
.panel { background: #fff; border-radius: 12px; padding: 20px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; }
.form-tip { font-size: 12px; color: #9ca3af; margin-top: 4px; line-height: 1.5; }

.face-upload-area { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }

/* 上传框 */
.face-uploader :deep(.el-upload) {
  border: 1px dashed #d4d7de; border-radius: 12px; cursor: pointer;
  width: 140px; height: 140px; display: flex; align-items: center; justify-content: center;
  transition: border-color .2s; overflow: hidden;
}
.face-uploader :deep(.el-upload:hover) { border-color: #2b6cff; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; color: #8a93a2; }
.upload-icon { font-size: 28px; color: #b3bac6; }
.upload-text { font-size: 13px; }
.upload-sub { font-size: 11px; color: #b3bac6; }
.preview-wrap { position: relative; width: 140px; height: 140px; }
.face-preview { width: 100%; height: 100%; object-fit: cover; }
.preview-mask {
  position: absolute; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; color: #fff; font-size: 13px; opacity: 0; transition: opacity .2s;
}
.preview-wrap:hover .preview-mask { opacity: 1; }

/* 大图预览按钮 */
.photo-preview-btn {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  width: 100px; height: 140px; background: #f0f5ff; border: 1px dashed #2b6cff;
  border-radius: 12px; cursor: pointer; color: #2b6cff; font-size: 12px; transition: all .2s;
}
.photo-preview-btn:hover { background: #e0ebff; }

/* 预留刷脸卡 */
.face-scan-card {
  width: 140px; height: 140px; border-radius: 12px;
  border: 1px dashed #d4d7de; background: #fafbfc;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; cursor: pointer; color: #8a93a2; transition: all .2s;
}
.face-scan-card:hover { background: #f0f5ff; border-color: #2b6cff; }
.scan-icon { font-size: 32px; color: #2b6cff; }
.scan-text { font-size: 13px; color: #606266; }
</style>
