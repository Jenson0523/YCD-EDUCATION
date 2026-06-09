<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">学科管理</h1>
      <el-button type="primary" @click="openCreate">新增学科</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="subjectCode" label="编码" width="120" />
        <el-table-column prop="subjectName" label="学科名称" width="160" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新增学科" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.subjectCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.subjectName" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category">
            <el-option label="文科" value="文科" />
            <el-option label="理科" value="理科" />
            <el-option label="综合" value="综合" />
            <el-option label="艺体" value="艺体" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchSubjects, createSubject, deleteSubject } from '../../api/academic';

const rows = ref([]);
const dialogVisible = ref(false);
const form = ref({ subjectCode: '', subjectName: '', category: '综合', sortOrder: 0 });

const load = async () => {
  try { rows.value = await fetchSubjects() || []; } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { subjectCode: '', subjectName: '', category: '综合', sortOrder: 0 };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await createSubject(form.value);
  ElMessage.success('创建成功');
  dialogVisible.value = false;
  load();
};

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该学科？', '提示', { type: 'warning' });
  await deleteSubject(id);
  ElMessage.success('已删除');
  load();
};
</script>
