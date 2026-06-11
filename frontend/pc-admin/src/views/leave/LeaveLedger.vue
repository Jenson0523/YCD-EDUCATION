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
        <el-table :data="rows" border stripe size="small" @row-click="openNormalDetail" highlight-current-row>
          <el-table-column prop="leaveNo" label="假条号" width="200" />
          <el-table-column prop="studentName" label="学生" width="90" />
          <el-table-column prop="className" label="班级" width="110" />
          <el-table-column prop="applicantLabel" label="申请来源" width="160">
            <template #default="{ row }">
              <el-tag size="small" :type="applicantTagType(row.applicantRole)" effect="plain">
                {{ row.applicantLabel || '—' }}
              </el-tag>
            </template>
          </el-table-column>
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

    <!-- 请假详情弹窗 -->
    <el-dialog v-model="detailVisible" title="请假详情" width="680px" :close-on-click-modal="true" destroy-on-close>
      <div v-if="detailLoading" style="text-align:center;padding:40px;">加载中...</div>
      <div v-else-if="!detail" style="text-align:center;padding:40px;">记录不存在</div>
      <template v-else>
        <!-- 状态卡 -->
        <div class="dt-status" :class="'dt-' + detail.status">
          <div>
            <span class="dt-status-text">{{ statusLabel(detail.status) }}</span>
            <span style="font-size:12px;color:#94a3b8;margin-left:12px;">{{ detail.leaveNo }}</span>
          </div>
          <div>
            <span v-if="detail.leaveType==='SICK'" style="color:#ef4444;">🤒 病假</span>
            <span v-else style="color:#3b82f6;">📌 事假</span>
            <el-tag v-if="detail.isTemp" type="warning" size="small" style="margin-left:8px;">⚡ 临时</el-tag>
          </div>
        </div>

        <!-- 学生信息 -->
        <div class="dt-card">
          <div class="dt-card-title">学生信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">{{ detail.studentName }}</el-descriptions-item>
            <el-descriptions-item label="学籍号">{{ detail.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ detail.className || '—' }}</el-descriptions-item>
            <el-descriptions-item label="申请人">
              <el-tag v-if="detail.applicantRole === 'PARENT'" type="success" size="small" effect="plain">家长</el-tag>
              <el-tag v-else-if="detail.applicantRole === 'TEACHER'" type="primary" size="small" effect="plain">老师代申请</el-tag>
              <el-tag v-else-if="detail.applicantRole === 'GATE'" type="warning" size="small" effect="plain">门卫登记</el-tag>
              <span v-else>{{ detail.applicantRole || '—' }}</span>
              <span v-if="detail.applicantName" style="margin-left:6px;font-weight:600;">{{ detail.applicantName }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 请假信息 -->
        <div class="dt-card">
          <div class="dt-card-title">请假信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="离校时间">{{ fmtDt(detail.leaveStart) }}</el-descriptions-item>
            <el-descriptions-item label="返校时间">{{ fmtDt(detail.leaveEnd) }}</el-descriptions-item>
            <el-descriptions-item label="请假原因" :span="2">{{ detail.reason }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.proofPhotoUrl" label="凭证/病例" :span="2">
              <el-image :src="detail.proofPhotoUrl" style="width:200px;height:auto;" :preview-src-list="[detail.proofPhotoUrl]" fit="contain" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 审批记录 -->
        <div class="dt-card" v-if="detail.status !== 'PENDING' && detail.status !== 'TEMP_PENDING'">
          <div class="dt-card-title">审批记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审批结果">
              <el-tag :type="detail.status === 'REJECTED' ? 'danger' : 'success'" size="small">
                {{ detail.status === 'REJECTED' ? '已驳回' : '已通过' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审批时间">{{ fmtDt(detail.approvedAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.approveRemark" label="审批意见" :span="2">{{ detail.approveRemark }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.approveSignatureUrl" label="审批签字" :span="2">
              <el-image :src="detail.approveSignatureUrl" style="max-width:300px;height:80px;border:1px solid #e5e7eb;border-radius:6px;padding:4px;" fit="contain" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 离校核验记录 -->
        <div class="dt-card" v-if="detail.departAt || (detail.verifications && detail.verifications.length)">
          <div class="dt-card-title">人脸识别核验记录</div>
          <el-table v-if="detail.verifications && detail.verifications.length" :data="detail.verifications" border size="small" style="width:100%">
            <el-table-column label="类型" width="70">
              <template #default="{ row }">{{ row.verifyType === 'DEPART' ? '离校' : '返校' }}</template>
            </el-table-column>
            <el-table-column label="人脸匹配" width="100">
              <template #default="{ row }">
                <el-text :type="row.faceMatchScore >= 80 ? 'success' : 'danger'">{{ row.faceMatchScore }}分</el-text>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">{{ row.result === 'PASS' ? '放行' : '拦截' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="刷脸照片" width="100">
              <template #default="{ row }">
                <el-image v-if="row.capturePhotoUrl" :src="row.capturePhotoUrl" style="width:60px;height:60px;border-radius:6px;" :preview-src-list="[row.capturePhotoUrl]" fit="cover" />
                <span v-else style="color:#cbd5e1;">—</span>
              </template>
            </el-table-column>
            <el-table-column label="核验时间" width="160">
              <template #default="{ row }">{{ fmtDt(row.verifiedAt) }}</template>
            </el-table-column>
          </el-table>
          <div v-else style="color:#9ca3af;font-size:13px;padding:8px 0;">{{ detail.departAt ? '已离校（' + fmtDt(detail.departAt) + '）' : '暂无核验记录' }}</div>
        </div>

        <!-- 返校记录 -->
        <div class="dt-card" v-if="detail.returnAt">
          <div class="dt-card-title">返校记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="返校时间">{{ fmtDt(detail.returnAt) }}</el-descriptions-item>
            <el-descriptions-item label="核验方式">门卫人脸核验</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-dialog>
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

// 详情弹窗
const detailVisible = ref(false);
const detail = ref(null);
const detailLoading = ref(false);

const openNormalDetail = async (row) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await http.get(`/leave/applications/${row.id}`);
  } catch {
    ElMessage.error('加载详情失败');
  } finally { detailLoading.value = false; }
};

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
const applicantTagType = (role) => ({ PARENT: 'success', TEACHER: 'warning', HEAD_TEACHER: 'warning', GATE: 'danger' }[role] || 'info');
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

/* 详情弹窗 */
.dt-status { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-radius: 10px; margin-bottom: 16px; border: 1px solid; background: #f8fafc; }
.dt-APPROVED,.dt-DEPARTED,.dt-RETURNED { background: #f0fdf4; border-color: #bbf7d0; }
.dt-PENDING,.dt-TEMP_PENDING { background: #fefce8; border-color: #fde68a; }
.dt-REJECTED { background: #fef2f2; border-color: #fecaca; }
.dt-status-text { font-size: 18px; font-weight: 700; }
.dt-card { margin-bottom: 16px; }
.dt-card-title { font-size: 16px; font-weight: 700; color: #1e293b; margin-bottom: 10px; padding-left: 10px; border-left: 4px solid #6366f1; }
</style>
