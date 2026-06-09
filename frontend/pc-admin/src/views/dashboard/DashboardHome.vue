<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">数据总览</h1>
      <span class="greeting">欢迎回来，{{ authStore.realName }} 👋</span>
    </div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6" v-for="item in metrics" :key="item.label">
        <div class="metric-card">
          <div class="metric-icon" :style="{ background: item.color }">
            <el-icon size="22" color="#fff"><component :is="item.icon" /></el-icon>
          </div>
          <div class="metric-body">
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-value">{{ item.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="panel">
          <h2 class="section-title">待跟进居家报备</h2>
          <div v-if="pendingReports.length === 0" class="empty-tip">暂无待跟进报备 🎉</div>
          <div v-else>
            <div v-for="r in pendingReports" :key="r.id" class="report-row">
              <div class="report-info">
                <span class="report-date">{{ r.reportDate }}</span>
                <el-tag size="small" type="warning">{{ r.emotionStatus || '无情绪标注' }}</el-tag>
              </div>
              <el-button size="small" type="primary" @click="goFollow">跟进</el-button>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="panel">
          <h2 class="section-title">管理闭环流程</h2>
          <el-steps :active="2" finish-status="success" direction="vertical" style="padding:8px 0">
            <el-step title="家长居家报备" description="家长通过小程序填写居家状态" />
            <el-step title="班主任跟进" description="教师查看并回复，自动通知家长" />
            <el-step title="数据中台沉淀" description="报备记录纳入学生成长档案" />
            <el-step title="大屏预警分析" description="异常情绪触发预警（Phase 7）" />
          </el-steps>
        </div>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { User, ChatDotRound, Finished, Warning } from '@element-plus/icons-vue';
import { useAuthStore } from '../../stores/auth';
import { fetchHomeReports } from '../../api/familySchool';
import { http } from '../../api/http';

const router = useRouter();
const authStore = useAuthStore();

const metrics = ref([
  { label: '在籍学生', value: '0', color: '#1b5ea6', icon: 'User' },
  { label: '待跟进报备', value: '0', color: '#f59e0b', icon: 'ChatDotRound' },
  { label: '待审批事项', value: '0', color: '#10b981', icon: 'Finished' },
  { label: '未读消息', value: '0', color: '#ef4444', icon: 'Warning' },
]);

const pendingReports = ref([]);

onMounted(async () => {
  try {
    const [stuData, reportData, msgData] = await Promise.allSettled([
      http.get('/stu/students', { params: { pageNo: 1, pageSize: 1 } }),
      fetchHomeReports({ followStatus: 'PENDING', pageNo: 1, pageSize: 5 }),
      http.get('/sys/messages/unread-count'),
    ]);
    if (stuData.status === 'fulfilled') metrics.value[0].value = stuData.value?.total ?? 0;
    if (reportData.status === 'fulfilled') {
      metrics.value[1].value = reportData.value?.total ?? 0;
      pendingReports.value = reportData.value?.records || [];
    }
    if (msgData.status === 'fulfilled') metrics.value[3].value = msgData.value?.count ?? 0;
  } catch {}
});

const goFollow = () => router.push('/family-school/home-reports');
</script>

<style scoped>
.greeting { font-size: 14px; color: #6b7280; }

.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-label { font-size: 13px; color: #6b7280; }
.metric-value { font-size: 28px; font-weight: 700; color: #111827; margin-top: 2px; }

.section-title { margin: 0 0 16px; font-size: 15px; font-weight: 600; }
.empty-tip { color: #9ca3af; font-size: 14px; padding: 20px 0; text-align: center; }

.report-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}

.report-info { display: flex; align-items: center; gap: 12px; }
.report-date { font-size: 14px; color: #374151; }
</style>
