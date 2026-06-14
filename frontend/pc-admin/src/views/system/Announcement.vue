<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">通知公告</h1>
      <el-button type="primary" :icon="Plus" @click="openCompose">发布公告</el-button>
    </div>

    <div class="panel">
      <div class="hint">发布后将推送到所选角色用户的小程序：首页弹窗 + 未读消息提示。</div>
      <el-table :data="sentList" border stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="紧急度" width="90">
          <template #default="{ row }">
            <el-tag :type="row.priority === 1 ? 'danger' : row.priority === 2 ? 'warning' : 'info'" size="small">
              {{ row.priority === 1 ? '紧急' : row.priority === 2 ? '重要' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="送达/已读" width="120">
          <template #default="{ row }">
            <span style="color:#2b6cff;font-weight:600;">{{ row.readCount }}</span>
            <span style="color:#94a3b8;"> / {{ row.total }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="170">
          <template #default="{ row }">{{ fmtDt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && sentList.length === 0" description="暂无已发布公告" />
    </div>

    <!-- 发布弹窗 -->
    <el-dialog v-model="composeVisible" title="发布通知公告" width="560px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="推送对象" required>
          <el-checkbox v-model="toAll" @change="onToAllChange">全体用户</el-checkbox>
          <el-checkbox-group v-model="form.targetRoles" :disabled="toAll" style="margin-left:12px;">
            <el-checkbox value="PARENT">家长</el-checkbox>
            <el-checkbox value="STUDENT">学生</el-checkbox>
            <el-checkbox value="HEAD_TEACHER">班主任</el-checkbox>
            <el-checkbox value="TEACHER">任课教师</el-checkbox>
            <el-checkbox value="GATE">门卫</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="form.priority">
            <el-radio :value="3">普通</el-radio>
            <el-radio :value="2">重要</el-radio>
            <el-radio :value="1">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composeVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="publish">确认发布</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { http } from '../../api/http';

const loading = ref(false);
const sentList = ref([]);
const composeVisible = ref(false);
const sending = ref(false);
const toAll = ref(true);
const form = reactive({ title: '', content: '', targetRoles: [], priority: 3 });

const fmtDt = (dt) => dt ? String(dt).replace('T', ' ').slice(0, 16) : '—';

const load = async () => {
  loading.value = true;
  try {
    sentList.value = await http.get('/sys/announcements/sent') || [];
  } catch { sentList.value = []; }
  finally { loading.value = false; }
};
onMounted(load);

const openCompose = () => {
  toAll.value = true;
  Object.assign(form, { title: '', content: '', targetRoles: [], priority: 3 });
  composeVisible.value = true;
};

const onToAllChange = (v) => { if (v) form.targetRoles = []; };

const publish = async () => {
  if (!form.title.trim() || !form.content.trim()) { ElMessage.warning('标题和内容不能为空'); return; }
  const targetRoles = toAll.value ? ['ALL'] : form.targetRoles;
  if (targetRoles.length === 0) { ElMessage.warning('请选择推送对象'); return; }
  sending.value = true;
  try {
    const r = await http.post('/sys/announcements', { title: form.title, content: form.content, targetRoles, priority: form.priority });
    ElMessage.success(`已发布，送达 ${r.sentCount} 人`);
    composeVisible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e?.message || '发布失败');
  } finally {
    sending.value = false;
  }
};
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 600; }
.panel { background: #fff; border-radius: 10px; padding: 16px; }
.hint { font-size: 13px; color: #94a3b8; margin-bottom: 12px; }
</style>
