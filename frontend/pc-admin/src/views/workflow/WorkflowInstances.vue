<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">审批中心</h1>
    </div>
    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="businessModule" label="板块" width="120" />
        <el-table-column prop="businessType" label="业务类型" width="140" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="applicantId" label="申请人" width="120" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { fetchFlowInstances } from '../../api/workflow';

const rows = ref([]);

onMounted(async () => {
  try {
    const data = await fetchFlowInstances({ pageNo: 1, pageSize: 20 });
    rows.value = data?.records || [];
  } catch {
    rows.value = [];
  }
});
</script>
