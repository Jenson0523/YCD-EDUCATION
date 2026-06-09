<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">数据总览</h1>
    </div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="item in metrics" :key="item.label">
        <div class="metric">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </el-col>
    </el-row>
    <div class="panel overview">
      <h2>Phase 1 管理闭环</h2>
      <el-steps :active="2" finish-status="success">
        <el-step title="学生建档" />
        <el-step title="家长报备" />
        <el-step title="班主任跟进" />
        <el-step title="数据中台沉淀" />
      </el-steps>
    </div>
  </section>
</template>

<script setup>
const metrics = [
  { label: '学生主档案', value: '0' },
  { label: '待跟进居家报备', value: '0' },
  { label: '待审批事项', value: '0' },
  { label: '风险预警', value: '0' }
];
</script>

<style scoped>
.metric {
  min-height: 96px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.metric span {
  color: var(--ycd-text-secondary);
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.overview {
  margin-top: 16px;
}

.overview h2 {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
