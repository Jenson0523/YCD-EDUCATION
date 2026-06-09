<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">作业管理</h1>
      <el-button type="primary" @click="openCreate">布置作业</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="title" label="作业标题" min-width="200" />
        <el-table-column prop="classId" label="班级ID" width="100" />
        <el-table-column prop="subjectId" label="学科ID" width="100" />
        <el-table-column prop="assignedDate" label="布置日期" width="120" />
        <el-table-column prop="dueDate" label="截止日期" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
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

    <el-dialog v-model="dialogVisible" title="布置作业" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="班级ID"><el-input-number v-model="form.classId" /></el-form-item>
        <el-form-item label="学科ID"><el-input-number v-model="form.subjectId" /></el-form-item>
        <el-form-item label="布置日期"><el-date-picker v-model="form.assignedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">发布</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchHomeworks, createHomework } from '../../api/academic';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const form = ref({ title: '', content: '', classId: null, subjectId: null, assignedDate: '', dueDate: '' });

const load = async () => {
  try {
    const data = await fetchHomeworks({ pageNo: pageNo.value, pageSize: pageSize.value });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { title: '', content: '', classId: null, subjectId: null, assignedDate: '', dueDate: '' };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await createHomework(form.value);
  ElMessage.success('作业已发布');
  dialogVisible.value = false;
  load();
};
</script>
