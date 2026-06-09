<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">居家状态报备</h1>
    </div>

    <!-- 筛选栏 -->
    <div class="panel filter-bar" style="margin-bottom:16px">
      <el-select v-model="filter.followStatus" placeholder="跟进状态" clearable style="width:140px" @change="load">
        <el-option label="待跟进" value="PENDING" />
        <el-option label="已跟进" value="FOLLOWED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows" border stripe>
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="reportDate" label="报备日期" width="120" />
        <el-table-column label="作息" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.sleepStatus" size="small" :type="sleepType(row.sleepStatus)">{{ row.sleepStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="情绪" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.emotionStatus" size="small" :type="emotionType(row.emotionStatus)">{{ row.emotionStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studyStatus" label="居家学习" width="120" />
        <el-table-column prop="familySpecialSituation" label="特殊情况" show-overflow-tooltip />
        <el-table-column label="跟进状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.followStatus === 'FOLLOWED' ? 'success' : 'warning'">
              {{ row.followStatus === 'FOLLOWED' ? '已跟进' : '待跟进' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="followRemark" label="跟进备注" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.followStatus === 'PENDING'"
              link type="primary"
              @click="openFollow(row)"
            >跟进</el-button>
            <el-button v-else link type="info" @click="openFollow(row)">查看</el-button>
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

    <!-- 跟进弹窗 -->
    <el-dialog v-model="dialogVisible" :title="current?.followStatus === 'FOLLOWED' ? '查看跟进' : '教师跟进'" width="500px">
      <el-descriptions :column="2" border size="small" class="report-detail">
        <el-descriptions-item label="报备日期">{{ current?.reportDate }}</el-descriptions-item>
        <el-descriptions-item label="学生ID">{{ current?.studentId }}</el-descriptions-item>
        <el-descriptions-item label="作息情况">{{ current?.sleepStatus }}</el-descriptions-item>
        <el-descriptions-item label="情绪状态">{{ current?.emotionStatus }}</el-descriptions-item>
        <el-descriptions-item label="居家学习">{{ current?.studyStatus }}</el-descriptions-item>
        <el-descriptions-item label="外出报备">{{ current?.outgoingReport }}</el-descriptions-item>
        <el-descriptions-item label="特殊情况" :span="2">{{ current?.familySpecialSituation }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="current?.followStatus === 'FOLLOWED'" class="follow-result">
        <el-divider>跟进记录</el-divider>
        <p>{{ current?.followRemark || '无备注' }}</p>
      </div>

      <template v-if="current?.followStatus === 'PENDING'">
        <el-divider>教师回复</el-divider>
        <el-input
          v-model="followRemark"
          type="textarea"
          :rows="3"
          placeholder="输入跟进内容，将通知家长（可不填）"
        />
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button
          v-if="current?.followStatus === 'PENDING'"
          type="primary"
          :loading="following"
          @click="submitFollow"
        >确认跟进</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchHomeReports, followHomeReport } from '../../api/familySchool';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filter = ref({ followStatus: 'PENDING' });
const dialogVisible = ref(false);
const current = ref(null);
const followRemark = ref('');
const following = ref(false);

const sleepType = (v) => v?.includes('正常') ? 'success' : v ? 'warning' : '';
const emotionType = (v) => v?.includes('稳定') ? 'success' : v?.includes('焦虑') || v?.includes('低落') ? 'danger' : 'warning';

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (filter.value.followStatus) params.followStatus = filter.value.followStatus;
    const data = await fetchHomeReports(params);
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openFollow = (row) => {
  current.value = row;
  followRemark.value = row.followRemark || '';
  dialogVisible.value = true;
};

const submitFollow = async () => {
  following.value = true;
  try {
    await followHomeReport(current.value.id, followRemark.value);
    ElMessage.success('跟进成功，已通知家长');
    dialogVisible.value = false;
    load();
  } finally {
    following.value = false;
  }
};
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; align-items: center; padding: 16px; }
.report-detail { margin-bottom: 8px; }
.follow-result { padding: 12px 0; color: #374151; }
.pagination { margin-top: 16px; }
</style>
