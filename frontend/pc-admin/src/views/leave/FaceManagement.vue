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
        <el-table-column label="人脸照片" width="80">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.facePhotoUrl || ''" icon="UserFilled" />
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
        <el-table-column prop="createdAt" label="创建时间" width="160" />
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
    <el-dialog v-model="dialogVisible" :title="editId ? '更新人脸照片' : '新增人脸档案'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学籍号"><el-input v-model="form.studentNo" :disabled="!!editId" /></el-form-item>
        <el-form-item label="学生姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="班级名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="人脸照片URL">
          <el-input v-model="form.facePhotoUrl" placeholder="输入图片地址或上传后填入" />
          <div style="font-size:12px;color:#9ca3af;margin-top:4px">
            实际部署时接入OSS上传，当前填入图片URL即可
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const statusFilter = ref('');
const dialogVisible = ref(false);
const editId = ref(null);
const form = ref({ studentNo: '', realName: '', className: '', facePhotoUrl: '' });

const load = async () => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value };
  if (keyword.value) params.keyword = keyword.value;
  if (statusFilter.value) params.status = statusFilter.value;
  const data = await http.get('/leave/face', { params });
  rows.value = data?.records || [];
  total.value = data?.total || 0;
};

onMounted(load);

const openCreate = () => {
  editId.value = null;
  form.value = { studentNo: '', realName: '', className: '', facePhotoUrl: '' };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  form.value = { studentNo: row.studentNo, realName: row.realName, className: row.className, facePhotoUrl: row.facePhotoUrl };
  dialogVisible.value = true;
};

const handleSave = async () => {
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
</style>
