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
        <el-table-column prop="gradeId" label="年级ID" width="120" />
        <el-table-column prop="studentCount" label="学生人数" width="100" />
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

    <el-dialog v-model="dialogVisible" title="新增班级" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.classCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级ID"><el-input-number v-model="form.gradeId" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchClasses, createClass } from '../../api/academic';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const form = ref({ classCode: '', className: '', gradeId: null });

const load = async () => {
  try {
    const data = await fetchClasses({ pageNo: pageNo.value, pageSize: pageSize.value });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { classCode: '', className: '', gradeId: null };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await createClass(form.value);
  ElMessage.success('创建成功');
  dialogVisible.value = false;
  load();
};
</script>
