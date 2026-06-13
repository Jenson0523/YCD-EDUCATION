<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">班级管理</h1>
      <el-button type="primary" @click="openCreate">新增班级</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="classCode" label="班级编码" width="140" />
        <el-table-column prop="className" label="班级名称" width="160" />
        <el-table-column prop="gradeId" label="年级ID" width="90" />
        <el-table-column label="班主任" width="140">
          <template #default="{ row }">
            <span v-if="row.headTeacherName">{{ row.headTeacherName }}</span>
            <el-tag v-else size="small" type="info">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学生人数" width="100" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @change="load"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑班级' : '新增班级'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.classCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级ID"><el-input-number v-model="form.gradeId" /></el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" placeholder="请选择班主任" clearable filterable style="width:100%">
            <el-option v-for="t in teacherOptions" :key="t.id" :label="`${t.realName} (${t.mobileEncrypted || '无手机号'})`" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchClasses, createClass } from '../../api/academic';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const editId = ref(null);
const form = ref({ classCode: '', className: '', gradeId: null, headTeacherId: null });
const teacherOptions = ref([]);

// 加载班主任选项
const loadTeachers = async () => {
  try {
    const data = await http.get('/sys/users', {
      params: { pageNo: 1, pageSize: 200, roleCode: 'HEAD_TEACHER,TEACHER' }
    });
    teacherOptions.value = data?.records || [];
  } catch { teacherOptions.value = []; }
};

const load = async () => {
  try {
    const data = await fetchClasses({ pageNo: pageNo.value, pageSize: pageSize.value });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(() => { load(); loadTeachers(); });

const openCreate = () => {
  editId.value = null;
  form.value = { classCode: '', className: '', gradeId: null, headTeacherId: null };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  form.value = {
    classCode: row.classCode,
    className: row.className,
    gradeId: row.gradeId,
    headTeacherId: row.headTeacherId || null
  };
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (editId.value) {
    await http.put(`/edu/classes/${editId.value}`, form.value);
  } else {
    await createClass(form.value);
  }
  ElMessage.success(editId.value ? '更新成功' : '创建成功');
  dialogVisible.value = false;
  load();
};

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该班级？', '提示', { type: 'warning' });
  await http.delete(`/edu/classes/${id}`);
  ElMessage.success('已删除');
  load();
};
</script>

<style scoped>
.pagination { margin-top: 16px; }
</style>
