<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">居家状态报备</h1>
      <el-button type="primary">导出</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="studentId" label="学生ID" width="120" />
        <el-table-column prop="reportDate" label="报备日期" width="140" />
        <el-table-column prop="sleepStatus" label="作息" />
        <el-table-column prop="emotionStatus" label="情绪" />
        <el-table-column prop="studyStatus" label="居家学习" />
        <el-table-column prop="followStatus" label="跟进状态" width="120" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { fetchHomeReports } from '../../api/familySchool';

const rows = ref([]);

onMounted(async () => {
  try {
    const data = await fetchHomeReports({ pageNo: 1, pageSize: 20 });
    rows.value = data?.records || [];
  } catch {
    rows.value = [];
  }
});
</script>
