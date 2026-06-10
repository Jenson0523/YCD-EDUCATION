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
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column label="人脸照片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.facePhotoUrl"
              :src="resolveUrl(row.facePhotoUrl)"
              :preview-src-list="[resolveUrl(row.facePhotoUrl)]"
              fit="cover"
              style="width:48px;height:48px;border-radius:8px"
              preview-teleported
            />
            <el-avatar v-else :size="40" :icon="UserFilled" />
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学籍号" width="140" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑照片</el-button>
            <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                       @click="toggleStatus(row)">
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNo" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @change="load" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '更新人脸照片' : '新增人脸档案'" width="560px">
      <el-form :model="form" label-width="100px">
        <!-- 学生选择（新增时） -->
        <el-form-item v-if="!editId" label="选择学生" required>
          <el-select
            v-model="selectedStudentId"
            filterable
            remote
            reserve-keyword
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

        <!-- 自动填充字段 -->
        <el-form-item label="学籍号">
          <el-input v-model="form.studentNo" :disabled="!editId" placeholder="选择学生后自动填充" />
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="form.realName" :disabled="!editId && !!selectedStudentId" placeholder="选择学生后自动填充" />
        </el-form-item>
        <el-form-item label="班级名称">
          <el-input v-model="form.className" :disabled="!editId && !!selectedStudentId" placeholder="选择学生后自动填充" />
        </el-form-item>

        <!-- 人脸照片上传 -->
        <el-form-item label="人脸照片" required>
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
        <el-button type="primary" :disabled="!canSave" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { UserFilled, Plus, Camera, Iphone } from '@element-plus/icons-vue';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const statusFilter = ref('');
const dialogVisible = ref(false);
const editId = ref(null);
const form = ref({ studentId: null, studentNo: '', realName: '', className: '', facePhotoUrl: '' });

// 学生搜索
const selectedStudentId = ref(null);
const studentOptions = ref([]);
const searchLoading = ref(false);

// 上传配置
const uploadAction = '/api/upload/image';
const uploadHeaders = computed(() => ({ Authorization: localStorage.getItem('ycd_token') || '' }));

const canSave = computed(() => {
  if (editId.value) return !!form.value.facePhotoUrl;
  return !!form.value.studentNo && !!form.value.facePhotoUrl;
});

// 把后端相对路径转为可访问URL（PC端走vite代理，/uploads 需指向后端）
const resolveUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  // 相对路径 /uploads/... → 通过代理或直连后端
  return url;
};

const load = async () => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value };
  if (keyword.value) params.keyword = keyword.value;
  if (statusFilter.value) params.status = statusFilter.value;
  const data = await http.get('/leave/face', { params });
  rows.value = data?.records || [];
  total.value = data?.total || 0;
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
  }
};

const openCreate = () => {
  editId.value = null;
  selectedStudentId.value = null;
  studentOptions.value = [];
  form.value = { studentId: null, studentNo: '', realName: '', className: '', facePhotoUrl: '' };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  selectedStudentId.value = null;
  form.value = {
    studentId: row.studentId, studentNo: row.studentNo,
    realName: row.realName, className: row.className, facePhotoUrl: row.facePhotoUrl
  };
  dialogVisible.value = true;
};

// 上传前校验
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
    ElMessage.warning('请选择学生并上传人脸照片');
    return;
  }
  await http.post('/leave/face', form.value);
  ElMessage.success('保存成功');
  dialogVisible.value = false;
  load();
};

const toggleStatus = async (row) => {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  await http.put(`/leave/face/${row.id}/status`, { status: newStatus });
  ElMessage.success('已更新');
  load();
};
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 16px; }
.form-tip { font-size: 12px; color: #9ca3af; margin-top: 4px; line-height: 1.5; }

.face-upload-area { display: flex; gap: 16px; align-items: flex-start; }

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
