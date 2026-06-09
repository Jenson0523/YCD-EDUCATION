<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">学生评优</h1>
      <el-button type="primary" @click="openCreate">新增候选</el-button>
    </div>
    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="filter.semester" placeholder="学期 如 2024-1" clearable style="width:160px" @change="load" />
        <el-select v-model="filter.status" placeholder="状态" clearable style="width:120px" @change="load">
          <el-option label="候选" value="CANDIDATE" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="studentId" label="学生ID" width="120" />
        <el-table-column prop="honorType" label="评优类型" width="160" />
        <el-table-column prop="semester" label="学期" width="100" />
        <el-table-column prop="score" label="综合分" width="100" />
        <el-table-column prop="rankInClass" label="班级排名" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="{ CANDIDATE: '', APPROVED: 'success', REJECTED: 'danger' }[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <template v-if="row.status === 'CANDIDATE'">
              <el-button link type="success" @click="handleApprove(row.id)">通过</el-button>
              <el-button link type="danger" @click="handleReject(row.id)">拒绝</el-button>
            </template>
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

    <el-dialog v-model="dialogVisible" title="新增评优候选" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学生ID"><el-input-number v-model="form.studentId" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="form.semester" placeholder="2024-1" /></el-form-item>
        <el-form-item label="评优类型">
          <el-select v-model="form.honorType">
            <el-option label="三好学生" value="三好学生" />
            <el-option label="优秀班干部" value="优秀班干部" />
            <el-option label="文明学生" value="文明学生" />
          </el-select>
        </el-form-item>
        <el-form-item label="综合得分"><el-input-number v-model="form.score" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchStudentHonors, approveStudentHonor, rejectStudentHonor } from '../../api/academic';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const filter = ref({ semester: '', status: '' });
const form = ref({ studentId: null, semester: '', honorType: '三好学生', score: null });

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (filter.value.semester) params.semester = filter.value.semester;
    if (filter.value.status) params.status = filter.value.status;
    const data = await fetchStudentHonors(params);
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { studentId: null, semester: '', honorType: '三好学生', score: null };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await http.post('/edu/student-honors', form.value);
  ElMessage.success('提交成功');
  dialogVisible.value = false;
  load();
};

const handleApprove = async (id) => {
  await approveStudentHonor(id);
  ElMessage.success('已通过');
  load();
};

const handleReject = async (id) => {
  await rejectStudentHonor(id);
  ElMessage.success('已拒绝');
  load();
};
</script>
