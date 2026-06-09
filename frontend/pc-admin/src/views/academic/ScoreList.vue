<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">成绩管理</h1>
      <el-button type="primary" @click="openCreate">录入成绩</el-button>
    </div>
    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="filter.semester" placeholder="学期 如 2024-1" clearable style="width:160px" @change="load" />
        <el-select v-model="filter.examType" placeholder="考试类型" clearable style="width:140px" @change="load">
          <el-option label="月考" value="MONTHLY" />
          <el-option label="期中" value="MID" />
          <el-option label="期末" value="FINAL" />
          <el-option label="随堂" value="QUIZ" />
        </el-select>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="studentId" label="学生ID" width="120" />
        <el-table-column prop="subjectId" label="学科ID" width="120" />
        <el-table-column prop="examType" label="考试类型" width="100" />
        <el-table-column prop="semester" label="学期" width="100" />
        <el-table-column prop="score" label="分数" width="100" />
        <el-table-column prop="rankInClass" label="班级排名" width="100" />
        <el-table-column prop="rankInGrade" label="年级排名" width="100" />
        <el-table-column prop="remark" label="备注" />
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

    <el-dialog v-model="dialogVisible" title="录入成绩" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学生ID"><el-input-number v-model="form.studentId" /></el-form-item>
        <el-form-item label="班级ID"><el-input-number v-model="form.classId" /></el-form-item>
        <el-form-item label="学科ID"><el-input-number v-model="form.subjectId" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="form.semester" placeholder="2024-1" /></el-form-item>
        <el-form-item label="考试类型">
          <el-select v-model="form.examType">
            <el-option label="月考" value="MONTHLY" />
            <el-option label="期中" value="MID" />
            <el-option label="期末" value="FINAL" />
            <el-option label="随堂" value="QUIZ" />
          </el-select>
        </el-form-item>
        <el-form-item label="分数"><el-input-number v-model="form.score" :precision="2" /></el-form-item>
        <el-form-item label="班级排名"><el-input-number v-model="form.rankInClass" :min="1" /></el-form-item>
        <el-form-item label="年级排名"><el-input-number v-model="form.rankInGrade" :min="1" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
import { fetchScores, createScore } from '../../api/academic';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const filter = ref({ semester: '', examType: '' });
const form = ref({ studentId: null, classId: null, subjectId: null, semester: '', examType: 'FINAL', score: null, rankInClass: null, rankInGrade: null, remark: '' });

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (filter.value.semester) params.semester = filter.value.semester;
    if (filter.value.examType) params.examType = filter.value.examType;
    const data = await fetchScores(params);
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  form.value = { studentId: null, classId: null, subjectId: null, semester: '', examType: 'FINAL', score: null, rankInClass: null, rankInGrade: null, remark: '' };
  dialogVisible.value = true;
};

const handleCreate = async () => {
  await createScore(form.value);
  ElMessage.success('录入成功');
  dialogVisible.value = false;
  load();
};
</script>
