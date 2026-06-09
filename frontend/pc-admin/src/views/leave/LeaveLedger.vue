<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">请假离校台账</h1>
    </div>

    <!-- 四大台账 Tab -->
    <el-tabs v-model="activeTab" @tab-change="loadCurrent">
      <el-tab-pane label="📋 常规请假台账" name="normal" />
      <el-tab-pane label="⚡ 临时补批台账" name="supplement" />
      <el-tab-pane label="✅ 人脸通行台账" name="gate" />
      <el-tab-pane label="⚠️ 异常拦截台账" name="abnormal" />
    </el-tabs>

    <div class="panel">
      <!-- 筛选 -->
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="学生姓名" clearable style="width:160px" @keyup.enter="loadCurrent" />
        <el-select v-if="activeTab === 'normal'" v-model="statusFilter" placeholder="状态" clearable style="width:130px">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已离校" value="DEPARTED" />
        </el-select>
        <el-select v-if="activeTab === 'supplement'" v-model="statusFilter" placeholder="状态" clearable style="width:130px">
          <el-option label="待补批" value="PENDING" />
          <el-option label="已补批" value="APPROVED" />
          <el-option label="已超时" value="OVERDUE" />
        </el-select>
        <el-select v-if="activeTab === 'gate'" v-model="statusFilter" placeholder="结果" clearable style="width:130px">
          <el-option label="通过" value="PASS" />
          <el-option label="无假条" value="NO_LEAVE" />
          <el-option label="人脸不匹配" value="FACE_MISMATCH" />
        </el-select>
        <el-select v-if="activeTab === 'abnormal'" v-model="statusFilter" placeholder="结果" clearable style="width:130px">
          <el-option label="无假条拦截" value="NO_LEAVE" />
          <el-option label="人脸不匹配" value="FACE_MISMATCH" />
        </el-select>
        <el-button type="primary" @click="loadCurrent">查询</el-button>
        <el-button @click="exportExcel">导出 Excel</el-button>
      </div>

      <!-- 常规请假台账 -->
      <template v-if="activeTab === 'normal'">
        <el-table :data="rows" border stripe size="small">
          <el-table-column prop="leaveNo" label="假条号" width="200" />
          <el-table-column prop="studentName" label="学生" width="90" />
          <el-table-column prop="className" label="班级" width="110" />
          <el-table-column label="类型" width="70">
            <template #default="{ row }">{{ row.leaveType === 'SICK' ? '病假' : '事假' }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
          <el-table-column label="离校时间" width="140">
            <template #default="{ row }">{{ fmtDt(row.leaveStart) }}</template>
          </el-table-column>
          <el-table-column label="返校时间" width="140">
            <template #default="{ row }">{{ fmtDt(row.leaveEnd) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="临时" width="60">
            <template #default="{ row }">
              <el-tag v-if="row.isTemp" type="warning" size="small">⚡</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 临时补批台账 -->
      <template v-else-if="activeTab === 'supplement'">
        <el-table :data="rows" border stripe size="small">
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="className" label="班级" width="110" />
          <el-table-column label="离校时间" width="150">
            <template #default="{ row }">{{ fmtDt(row.departAt) }}</template>
          </el-table-column>
          <el-table-column label="补批截止" width="150">
            <template #default="{ row }">{{ fmtDt(row.deadline) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="suppTagType(row.supplementStatus)" size="small">{{ suppLabel(row.supplementStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补批时间" width="150">
            <template #default="{ row }">{{ fmtDt(row.supplementedAt) }}</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 人脸通行台账 / 异常台账 -->
      <template v-else>
        <el-table :data="rows" border stripe size="small">
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="verifyType" label="类型" width="80">
            <template #default="{ row }">{{ row.verifyType === 'DEPART' ? '离校' : '返校' }}</template>
          </el-table-column>
          <el-table-column label="匹配分" width="90">
            <template #default="{ row }">
              <el-text :type="row.faceMatchScore >= 80 ? 'success' : 'danger'">
                {{ row.faceMatchScore }}分
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="110">
            <template #default="{ row }">
              <el-tag :type="gateTagType(row.result)" size="small">{{ gateLabel(row.result) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column label="核验时间" width="160">
            <template #default="{ row }">{{ fmtDt(row.verifiedAt) }}</template>
          </el-table-column>
        </el-table>
      </template>

      <el-pagination class="pagination" v-model:current-page="pageNo" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @change="loadCurrent" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { http } from '../../api/http';

const activeTab = ref('normal');
const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const statusFilter = ref('');

const loadCurrent = async () => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value };
  if (keyword.value) params.keyword = keyword.value;
  if (statusFilter.value) params.status = statusFilter.value;
  try {
    let data;
    if (activeTab.value === 'normal') {
      data = await http.get('/leave/applications', { params });
    } else if (activeTab.value === 'supplement') {
      data = await http.get('/leave/applications/supplements', { params });
    } else if (activeTab.value === 'gate') {
      data = await http.get('/leave/gate/ledger', { params });
    } else {
      // 异常台账：result in (NO_LEAVE, FACE_MISMATCH)
      params.result = statusFilter.value || 'NO_LEAVE';
      data = await http.get('/leave/gate/ledger', { params });
    }
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(loadCurrent);

const exportExcel = () => ElMessage.info('导出功能需接入 Apache POI，本期留接口预留');

const fmtDt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({ PENDING:'待审批',APPROVED:'已批准',REJECTED:'已驳回',DEPARTED:'已离校',RETURNED:'已返校',TEMP_PENDING:'临时待补批' }[s] || s);
const statusTagType = (s) => ({ PENDING:'warning',APPROVED:'success',REJECTED:'danger',DEPARTED:'info',TEMP_PENDING:'warning' }[s] || '');
const suppLabel = (s) => ({ PENDING:'待补批',APPROVED:'已补批',OVERDUE:'已超时' }[s] || s);
const suppTagType = (s) => ({ PENDING:'warning',APPROVED:'success',OVERDUE:'danger' }[s] || '');
const gateLabel = (s) => ({ PASS:'通过',NO_LEAVE:'无假条',FACE_MISMATCH:'人脸不匹配' }[s] || s);
const gateTagType = (s) => ({ PASS:'success',NO_LEAVE:'warning',FACE_MISMATCH:'danger' }[s] || '');
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; }
</style>
