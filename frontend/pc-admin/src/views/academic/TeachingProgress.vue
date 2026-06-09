<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">教学进度</h1>
      <el-button type="primary" @click="openCreate">记录进度</el-button>
    </div>
    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="filter.semester" placeholder="学期 如 2024-1" clearable style="width:160px" @change="load" />
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="chapter" label="章节" min-width="180" />
        <el-table-column prop="classId" label="班级ID" width="100" />
        <el-table-column prop="subjectId" label="学科ID" width="100" />
        <el-table-column prop="teachDate" label="授课日期" width="120" />
        <el-table-column prop="plannedHours" label="计划课时" width="100" />
        <el-table-column prop="actualHours" label="实际课时" width="100" />
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

    <el-dialog v-model="dialogVisible" title="记录教学进度" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="章节"><el-input v-model="form.chapter" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="班级ID"><el-input-number v-model="form.classId" /></el-form-item>
        <el-form-item label="学科ID"><el-input-number v-model="form.subjectId" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="form.semester" placeholder="2024-1" /></el-form-item>
        <el-form-item label="授课日期"><el-date-picker v-model="form.teachDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="计划课时"><el-input-number v-model="form.plannedHours" :min="1" /></el-form-item>
        <el-form-item label="实际课时"><el-input-number v-model="form.actualHours" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchTeachingProgress, createTeachingProgress } from '../../api/academic';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const filter = ref({ semester: '' });
const form = ref({ chapter: '', content: '', classId: null, subjectId: null, semester: '', teachDate: '', plannedHours: 1, actualHours: 1 });

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (filter.value.semester) params.semester = filter.value.semester;
    const data = await fetchTeachingProgress(params);
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { chapter: '', content: '', classId: null, subjectId: null, semester: '', teachDate: '', plannedHours: 1, actualHours: 1 };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await createTeachingProgress(form.value);
  ElMessage.success('记录成功');
  dialogVisible.value = false;
  load();
};
</script>
