<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">学生学籍管理</h1>
      <div>
        <el-button @click="downloadTemplate">下载导入模板</el-button>
        <el-upload :show-file-list="false" :auto-upload="true" accept=".xlsx,.xls"
                   :http-request="doImport" style="display:inline-block;margin:0 8px">
          <el-button type="warning">Excel 批量导入</el-button>
        </el-upload>
        <el-button type="primary" @click="openCreate">+ 新建档案</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="姓名/学籍号" clearable style="width:200px" @keyup.enter="load" />
        <el-select v-model="classFilter" placeholder="班级" clearable filterable style="width:180px">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="studentNo" label="学籍号" width="130" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="gradeName" label="年级" width="100" />
        <el-table-column prop="className" label="班级" width="130" />
        <el-table-column prop="guardianName" label="监护人" width="100" />
        <el-table-column prop="guardianMobileEncrypted" label="监护人手机" width="130" />
        <el-table-column label="家长账号" width="100">
          <template #default="{ row }">
            <el-tag :type="row.parentUserId ? 'success' : 'info'" size="small">
              {{ row.parentUserId ? '已开通' : '未开通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '在读' : '休学' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNo" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @change="load" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑学籍' : '新建学籍'" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="学籍号" required>
          <el-input v-model="form.studentNo" :disabled="!!editId" placeholder="唯一学籍号" />
        </el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年级"><el-input v-model="form.gradeName" placeholder="如：高一" /></el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" placeholder="选择班级" filterable clearable style="width:100%"
                     @change="onClassPick">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="在读" value="ACTIVE" />
            <el-option label="休学" value="SUSPENDED" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">监护人信息</el-divider>

        <el-form-item label="监护人姓名"><el-input v-model="form.guardianName" placeholder="家长姓名" /></el-form-item>
        <el-form-item label="监护人手机号"><el-input v-model="form.guardianMobileEncrypted" placeholder="家长手机号" /></el-form-item>
        <el-form-item label="自动开通账号">
          <el-switch v-model="form.parentCreateAccountFlag"
                     :disabled="!form.guardianName || !form.guardianMobileEncrypted"
                     active-text="是" inactive-text="否" />
          <span class="hint">开通后自动创建家长用户（用户名=姓名，密码=手机号）并绑定该学生</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果明细弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入结果" width="520px">
      <div style="margin-bottom:12px">
        <el-tag type="success">成功 {{ importResult.success }} 条</el-tag>
        <el-tag type="warning" style="margin-left:8px">失败 {{ importResult.failed }} 条</el-tag>
        <el-tag v-if="importResult.parentCreated" type="primary" style="margin-left:8px">新增家长 {{ importResult.parentCreated }} 人</el-tag>
        <el-tag v-if="importResult.classCreated" type="success" style="margin-left:8px">新增班级 {{ importResult.classCreated }} 个</el-tag>
      </div>
      <div v-if="importResult.errors && importResult.errors.length" style="max-height:300px;overflow-y:auto">
        <div v-for="(e, i) in importResult.errors" :key="i" style="color:#f56c6c;font-size:13px;margin:4px 0">{{ e }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="importDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { http } from '../../api/http';

const rows = ref([]);
const classes = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const classFilter = ref('');
const dialogVisible = ref(false);
const editId = ref(null);
const importDialogVisible = ref(false);
const importResult = ref({ success: 0, failed: 0, errors: [], parentCreated: 0, classCreated: 0 });
const form = ref({
  studentNo: '', name: '', gender: '男', gradeName: '', classId: null, className: '',
  status: 'ACTIVE', guardianName: '', guardianMobileEncrypted: '', parentCreateAccountFlag: false
});

const load = async () => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value };
  if (keyword.value) params.keyword = keyword.value;
  if (classFilter.value) params.classId = classFilter.value;
  const data = await http.get('/stu/students', { params });
  rows.value = data?.records || [];
  total.value = data?.total || 0;
};

const loadClasses = async () => {
  const data = await http.get('/edu/classes', { params: { pageNo: 1, pageSize: 200 } }).catch(() => ({ records: [] }));
  classes.value = data?.records || [];
};

onMounted(() => { load(); loadClasses(); });

const openCreate = () => {
  editId.value = null;
  form.value = {
    studentNo: '', name: '', gender: '男', gradeName: '', classId: null, className: '',
    status: 'ACTIVE', guardianName: '', guardianMobileEncrypted: '', parentCreateAccountFlag: false
  };
  dialogVisible.value = true;
};
const openEdit = (row) => {
  editId.value = row.id;
  form.value = {
    studentNo: row.studentNo, name: row.name, gender: row.gender,
    gradeName: row.gradeName, classId: row.classId, className: row.className,
    status: row.status,
    guardianName: row.guardianName || '',
    guardianMobileEncrypted: row.guardianMobileEncrypted || '',
    parentCreateAccountFlag: row.parentCreateAccount === 1
  };
  dialogVisible.value = true;
};
const onClassPick = (id) => { form.value.className = classes.value.find(c => c.id === id)?.className || ''; };

const handleSave = async () => {
  if (!form.value.studentNo || !form.value.name) { ElMessage.warning('学籍号和姓名必填'); return; }
  // 构建提交数据
  const submit = {
    ...form.value,
    parentCreateAccount: form.value.parentCreateAccountFlag ? 1 : 0
  };
  delete submit.parentCreateAccountFlag;
  if (editId.value) await http.put(`/stu/students/${editId.value}`, submit);
  else await http.post('/stu/students', submit);
  ElMessage.success('保存成功');
  dialogVisible.value = false;
  load();
};

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该学籍档案？', '提示', { type: 'warning' });
  await http.delete(`/stu/students/${id}`);
  ElMessage.success('已删除');
  load();
};

const downloadTemplate = async () => {
  const token = localStorage.getItem('ycd_token');
  try {
    const resp = await fetch('/api/stu/students/template', { headers: { Authorization: token } });
    if (!resp.ok) { ElMessage.error('模板下载失败'); return; }
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = '学生学籍导入模板.xlsx';
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  } catch { ElMessage.error('模板下载失败'); }
};

const doImport = async (option) => {
  const fd = new FormData();
  fd.append('file', option.file);
  try {
    const res = await http.post('/stu/students/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
    importResult.value = {
      success: res.success || 0,
      failed: res.failed || 0,
      errors: res.errors || [],
      parentCreated: res.parentCreated || 0,
      classCreated: res.classCreated || 0
    };
    importDialogVisible.value = true;
    load();
  } catch { /* 拦截器已提示 */ }
};
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 16px; }
.hint { color: #909399; font-size: 12px; margin-left: 10px; }
</style>
