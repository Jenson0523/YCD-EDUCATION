<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">审批中心</h1>
      <span class="page-desc">查看全校所有请假审批记录，支持搜索、筛选与详情查看</span>
    </div>

    <div class="panel">
      <!-- ══════ 搜索筛选栏 ══════ -->
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索学生姓名 / 假条号" clearable style="width:220px"
          @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="statusFilter" placeholder="审批状态" clearable style="width:150px" @change="search">
          <el-option label="待审批" value="PENDING" />
          <el-option label="临时待补批" value="TEMP_PENDING" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已离校" value="DEPARTED" />
          <el-option label="已返校" value="RETURNED" />
        </el-select>

        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期"
          format="YYYY-MM-DD" value-format="YYYY-MM-DD"
          style="width:260px" @change="search" />

        <el-select v-model="classFilter" placeholder="班级" clearable style="width:150px" @change="search">
          <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="resetFilter">重置</el-button>
        <el-button @click="exportExcel" :disabled="total === 0" style="margin-left:auto;">
          <el-icon><Download /></el-icon>导出 Excel
        </el-button>
      </div>

      <!-- ══════ 统计概览 ══════ -->
      <div class="stats-row">
        <div class="stat-chip pending" @click="statusFilter='PENDING';search()">
          <span class="stat-chip-num">{{ statsPending }}</span>
          <span class="stat-chip-label">待审批</span>
        </div>
        <div class="stat-chip temp" @click="statusFilter='TEMP_PENDING';search()">
          <span class="stat-chip-num">{{ statsTemp }}</span>
          <span class="stat-chip-label">临时待补</span>
        </div>
        <div class="stat-chip approved" @click="statusFilter='APPROVED';search()">
          <span class="stat-chip-num">{{ statsApproved }}</span>
          <span class="stat-chip-label">已批准</span>
        </div>
        <div class="stat-chip rejected" @click="statusFilter='REJECTED';search()">
          <span class="stat-chip-num">{{ statsRejected }}</span>
          <span class="stat-chip-label">已驳回</span>
        </div>
        <div class="stat-chip departed" @click="statusFilter='DEPARTED';search()">
          <span class="stat-chip-num">{{ statsDeparted }}</span>
          <span class="stat-chip-label">已离校</span>
        </div>
      </div>

      <!-- ══════ 数据表格 ══════ -->
      <el-table :data="rows" border stripe size="small" v-loading="loading"
        highlight-current-row @row-click="openDetail" style="cursor:pointer;">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="leaveNo" label="假条号" width="195" sortable />
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="studentNo" label="学籍号" width="130" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="申请来源" width="100">
          <template #default="{ row }">
            <el-tag :type="applicantTag(row.applicantRole)" size="small" effect="plain">
              {{ applicantLabel(row.applicantRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.leaveType === 'SICK'" type="danger" size="small" effect="plain">病假</el-tag>
            <el-tag v-else type="primary" size="small" effect="plain">事假</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="请假原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="离校时间" width="145" sortable prop="leaveStart">
          <template #default="{ row }">{{ fmt(row.leaveStart) }}</template>
        </el-table-column>
        <el-table-column label="返校时间" width="145">
          <template #default="{ row }">{{ fmt(row.leaveEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
            <el-tag v-if="row.isTemp" type="warning" size="small" style="margin-left:4px;">⚡</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批时间" width="145">
          <template #default="{ row }">{{ fmt(row.approvedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无审批记录" :image-size="80" />
        </template>
      </el-table>

      <el-pagination class="pagination"
        v-model:current-page="pageNo" v-model:page-size="pageSize"
        :total="total" :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="search" @current-change="search" />
    </div>

    <!-- ══════ 详情弹窗 ══════ -->
    <el-dialog v-model="detailVisible" title="审批详情" width="720px"
      :close-on-click-modal="true" destroy-on-close>
      <div v-if="detailLoading" class="dt-loading">加载中，请稍候...</div>
      <div v-else-if="!detail" class="dt-empty">记录不存在或已被删除</div>
      <template v-else>
        <!-- 状态卡 -->
        <div class="dt-status" :class="'dt-' + detail.status">
          <div class="dt-status-left">
            <span class="dt-status-text">{{ statusLabel(detail.status) }}</span>
            <span class="dt-status-no">#{{ detail.leaveNo }}</span>
          </div>
          <div class="dt-status-right">
            <span class="dt-leave-type">{{ detail.leaveType === 'SICK' ? '🤒 病假' : '📌 事假' }}</span>
            <el-tag v-if="detail.isTemp" type="warning" size="small">⚡ 临时</el-tag>
          </div>
        </div>

        <!-- 学生信息 -->
        <div class="dt-card">
          <div class="dt-card-title">学生信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="学生姓名" :span="1">{{ detail.studentName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="学籍号">{{ detail.studentNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ detail.className || '—' }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ detail.gradeName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="班主任">{{ detail.headTeacherName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="申请人">
              <el-tag :type="applicantTag(detail.applicantRole)" size="small" effect="plain">
                {{ applicantLabel(detail.applicantRole) }}
              </el-tag>
              <span v-if="detail.applicantName" class="dt-inline-name">{{ detail.applicantName }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 请假信息 -->
        <div class="dt-card">
          <div class="dt-card-title">请假信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="离校时间">{{ fmt(detail.leaveStart) }}</el-descriptions-item>
            <el-descriptions-item label="预计返校">{{ fmt(detail.leaveEnd) }}</el-descriptions-item>
            <el-descriptions-item label="请假原因" :span="2">{{ detail.reason || '—' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.proofPhotoUrl" label="凭证附件" :span="2">
              <el-image :src="resolveUrl(detail.proofPhotoUrl)" style="max-width:300px;max-height:200px;border-radius:8px;"
                :preview-src-list="[resolveUrl(detail.proofPhotoUrl)]" preview-teleported fit="contain" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 审批记录 -->
        <div class="dt-card" v-if="detail.status !== 'PENDING' && detail.status !== 'TEMP_PENDING'">
          <div class="dt-card-title">审批记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审批结果">
              <el-tag :type="detail.status === 'REJECTED' ? 'danger' : 'success'" size="small" effect="dark">
                {{ detail.status === 'REJECTED' ? '已驳回' : '已批准' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审批时间">{{ fmt(detail.approvedAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.approveRemark" label="审批意见" :span="2">
              {{ detail.approveRemark }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.approveSignatureUrl" label="审批签字" :span="2">
              <el-image :src="resolveUrl(detail.approveSignatureUrl)"
                style="max-width:320px;height:80px;border:1px solid #e5e7eb;border-radius:6px;padding:6px;background:#fff;"
                :preview-src-list="[resolveUrl(detail.approveSignatureUrl)]" preview-teleported fit="contain" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 离校/返校核验 -->
        <div class="dt-card" v-if="detail.departAt || detail.returnAt">
          <div class="dt-card-title">离校 / 返校记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-if="detail.departAt" label="离校时间">{{ fmt(detail.departAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.departAt" label="离校核验">门卫人脸核验放行</el-descriptions-item>
            <el-descriptions-item v-if="detail.returnAt" label="返校时间">{{ fmt(detail.returnAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.returnAt" label="返校核验">门卫人脸核验入校</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 核验记录明细 -->
        <div class="dt-card" v-if="detail.verifications && detail.verifications.length > 0">
          <div class="dt-card-title">人脸核验记录明细</div>
          <el-table :data="detail.verifications" border size="small" style="width:100%">
            <el-table-column label="类型" width="70">
              <template #default="{ row }">{{ row.verifyType === 'DEPART' ? '离校' : '返校' }}</template>
            </el-table-column>
            <el-table-column label="人脸匹配分" width="110">
              <template #default="{ row }">
                <el-text :type="row.faceMatchScore >= 80 ? 'success' : 'danger'" tag="b">
                  {{ row.faceMatchScore }} 分
                </el-text>
              </template>
            </el-table-column>
            <el-table-column label="核验结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">
                  {{ row.result === 'PASS' ? '✅ 放行' : '🚫 拦截' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="抓拍照片" width="90">
              <template #default="{ row }">
                <el-image v-if="row.capturePhotoUrl" :src="resolveUrl(row.capturePhotoUrl)"
                  style="width:56px;height:56px;border-radius:6px;object-fit:cover;"
                  :preview-src-list="[resolveUrl(row.capturePhotoUrl)]" preview-teleported />
                <span v-else style="color:#cbd5e1;">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="核验时间" width="150">
              <template #default="{ row }">{{ fmt(row.verifiedAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Download } from '@element-plus/icons-vue';
import { http } from '../../api/http';

/** 受保护图片：/uploads 已不公开，统一经 /api/files/preview 鉴权访问 */
const resolveUrl = (url) => {
  if (!url) return '';
  let p = String(url);
  const i = p.indexOf('/uploads/');
  if (i >= 0) p = p.substring(i);
  else if (p.startsWith('http')) return p;
  const token = localStorage.getItem('ycd_token') || '';
  return `/api/files/preview?path=${encodeURIComponent(p)}&token=${encodeURIComponent(token)}`;
};

// ═══ 列表数据 ═══
const rows = ref([]);
const loading = ref(false);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);

// ═══ 筛选条件 ═══
const keyword = ref('');
const statusFilter = ref('');
const dateRange = ref(null);
const classFilter = ref('');
const classList = ref([]);

// ═══ 统计芯片 ═══
const statsPending = ref(0);
const statsTemp = ref(0);
const statsApproved = ref(0);
const statsRejected = ref(0);
const statsDeparted = ref(0);

// ═══ 详情弹窗 ═══
const detailVisible = ref(false);
const detail = ref(null);
const detailLoading = ref(false);

// ═══ 工具函数 ═══
const fmt = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '—';
const statusLabel = (s) => ({
  PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
  DEPARTED: '已离校', RETURNED: '已返校', TEMP_PENDING: '临时待补批'
}[s] || s);
const statusTag = (s) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
  DEPARTED: 'info', RETURNED: '', TEMP_PENDING: 'warning'
}[s] || 'info');
const applicantLabel = (r) => ({ PARENT: '家长', TEACHER: '教师', HEAD_TEACHER: '班主任', GATE: '门卫' }[r] || r);
const applicantTag = (r) => ({ PARENT: 'success', TEACHER: 'warning', HEAD_TEACHER: '', GATE: 'danger' }[r] || 'info');

// ═══ 加载班级列表 ═══
const loadClasses = async () => {
  try {
    const res = await http.get('/edu/classes', { params: { pageSize: 999 } });
    classList.value = (res?.records || []).map(c => ({ id: c.id, name: c.className }));
  } catch { classList.value = []; }
};

// ═══ 加载统计 ═══
const loadStats = async () => {
  try {
    const [pending, temp, approved, rejected, departed] = await Promise.all([
      http.get('/leave/applications', { params: { status: 'PENDING', pageSize: 1 } }),
      http.get('/leave/applications', { params: { status: 'TEMP_PENDING', pageSize: 1 } }),
      http.get('/leave/applications', { params: { status: 'APPROVED', pageSize: 1 } }),
      http.get('/leave/applications', { params: { status: 'REJECTED', pageSize: 1 } }),
      http.get('/leave/applications', { params: { status: 'DEPARTED', pageSize: 1 } }),
    ]);
    statsPending.value = pending?.total || 0;
    statsTemp.value = temp?.total || 0;
    statsApproved.value = approved?.total || 0;
    statsRejected.value = rejected?.total || 0;
    statsDeparted.value = departed?.total || 0;
  } catch { /* 统计非关键 */ }
};

// ═══ 主查询 ═══
const search = async () => {
  loading.value = true;
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (keyword.value) params.keyword = keyword.value;
    if (statusFilter.value) params.status = statusFilter.value;
    if (classFilter.value) params.classId = classFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const data = await http.get('/leave/applications', { params });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch (e) {
    ElMessage.error('加载审批数据失败');
    rows.value = [];
  } finally { loading.value = false; }
};

// ═══ 重置 ═══
const resetFilter = () => {
  keyword.value = '';
  statusFilter.value = '';
  dateRange.value = null;
  classFilter.value = '';
  pageNo.value = 1;
  search();
};

// ═══ 打开详情 ═══
const openDetail = async (row) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await http.get(`/leave/applications/${row.id}`);
  } catch {
    ElMessage.error('加载详情失败');
  } finally { detailLoading.value = false; }
};

// ═══ 导出 ═══
const exportExcel = () => ElMessage.info('导出功能开发中，即将支持导出为 Excel');

// ═══ 初始化 ═══
onMounted(() => {
  loadClasses();
  loadStats();
  search();
});
</script>

<style scoped>
.page-header { display: flex; align-items: baseline; gap: 16px; margin-bottom: 6px; }
.page-title { font-size: 22px; font-weight: 700; color: #1e293b; }
.page-desc { font-size: 13px; color: #94a3b8; }

.panel { background: #fff; border-radius: 10px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }

/* 筛选栏 */
.filter-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }

/* 统计芯片 */
.stats-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.stat-chip {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s;
  border: 1px solid; user-select: none;
}
.stat-chip:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.stat-chip-num { font-weight: 700; font-size: 16px; }
.stat-chip-label { color: #64748b; }

.stat-chip.pending { background: #fefce8; border-color: #fde68a; }
.stat-chip.pending .stat-chip-num { color: #d97706; }
.stat-chip.temp { background: #fff7ed; border-color: #fed7aa; }
.stat-chip.temp .stat-chip-num { color: #ea580c; }
.stat-chip.approved { background: #f0fdf4; border-color: #bbf7d0; }
.stat-chip.approved .stat-chip-num { color: #16a34a; }
.stat-chip.rejected { background: #fef2f2; border-color: #fecaca; }
.stat-chip.rejected .stat-chip-num { color: #dc2626; }
.stat-chip.departed { background: #eff6ff; border-color: #bfdbfe; }
.stat-chip.departed .stat-chip-num { color: #2563eb; }

.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }

/* 详情弹窗 */
.dt-loading, .dt-empty { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; }

.dt-status { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; border-radius: 10px; margin-bottom: 16px; border: 1px solid; }
.dt-PENDING, .dt-TEMP_PENDING { background: #fefce8; border-color: #fde68a; }
.dt-APPROVED, .dt-DEPARTED, .dt-RETURNED { background: #f0fdf4; border-color: #bbf7d0; }
.dt-REJECTED { background: #fef2f2; border-color: #fecaca; }
.dt-status-left { display: flex; align-items: baseline; gap: 12px; }
.dt-status-text { font-size: 20px; font-weight: 700; color: #1e293b; }
.dt-status-no { font-size: 12px; color: #94a3b8; font-family: monospace; }
.dt-status-right { display: flex; align-items: center; gap: 10px; }
.dt-leave-type { font-size: 14px; color: #475569; }

.dt-card { margin-bottom: 16px; }
.dt-card-title { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 10px; padding-left: 10px; border-left: 4px solid #6366f1; }
.dt-inline-name { margin-left: 6px; font-weight: 600; color: #334155; }
</style>
