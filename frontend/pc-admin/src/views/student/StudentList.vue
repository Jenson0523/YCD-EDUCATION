<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">学生主档案</h1>
      <el-button type="primary">新建档案</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gradeName" label="年级" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="guardianName" label="监护人" width="120" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { fetchStudents } from '../../api/student';

const rows = ref([]);

onMounted(async () => {
  try {
    const data = await fetchStudents({ pageNo: 1, pageSize: 20 });
    rows.value = data?.records || [];
  } catch {
    rows.value = [];
  }
});
</script>
