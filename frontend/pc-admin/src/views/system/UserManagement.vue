<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <!-- 角色分Tab -->
    <div class="panel">
      <div class="tab-bar">
        <el-radio-group v-model="activeTab" @change="onTabChange" size="large">
          <el-radio-button value="ALL">全部用户</el-radio-button>
          <el-radio-button value="ADMIN">管理员</el-radio-button>
          <el-radio-button value="TEACHER">教师</el-radio-button>
          <el-radio-button value="PARENT">家长</el-radio-button>
        </el-radio-group>
        <div class="filter-bar">
          <el-input v-model="keyword" placeholder="姓名/用户名" clearable style="width:200px" @keyup.enter="load" />
          <el-select v-if="activeTab === 'TEACHER'" v-model="teacherRoleFilter" placeholder="教师角色" clearable style="width:140px">
            <el-option label="班主任" value="HEAD_TEACHER" />
            <el-option label="科任教师" value="TEACHER" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTagType(row.roleCode)">{{ roleLabel(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="mobileEncrypted" label="手机号" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 家长：绑定学生 -->
        <el-table-column v-if="activeTab === 'PARENT' || activeTab === 'ALL'" label="绑定学生" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleCode === 'PARENT'">
              <el-tag v-for="s in row.boundStudents" :key="s" size="small" type="success" style="margin:1px">
                {{ s }}
              </el-tag>
              <span v-if="!row.boundStudents || row.boundStudents.length === 0" style="color:#999">无</span>
            </template>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <!-- 教师：管理班级 -->
        <el-table-column v-if="activeTab === 'TEACHER' || activeTab === 'ALL'" label="管理班级" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleCode === 'HEAD_TEACHER' || row.roleCode === 'TEACHER'">
              <el-tag v-for="c in row.managedClasses" :key="c" size="small" type="warning" style="margin:1px">
                {{ c }}
              </el-tag>
              <span v-if="!row.managedClasses || row.managedClasses.length === 0" style="color:#999">无</span>
            </template>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="resetPwd(row.id)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!editId" label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="!editId" label="初始密码"><el-input v-model="form.password" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.mobile" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width:100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" style="width:100%">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editId" label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { http } from '../../api/http';

const rows = ref([]);
const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref('');
const activeTab = ref('ALL');
const teacherRoleFilter = ref('');
const dialogVisible = ref(false);
const editId = ref(null);
const form = ref({
  username: '', password: '', realName: '', roleCode: 'TEACHER',
  status: 'ACTIVE', mobile: '', gender: ''
});

const roles = [
  { value: 'ADMIN', label: '系统管理员' },
  { value: 'PRINCIPAL', label: '校长/股东' },
  { value: 'ACADEMIC', label: '教务处' },
  { value: 'HR', label: '人事专员' },
  { value: 'FINANCE', label: '财务专员' },
  { value: 'HEAD_TEACHER', label: '班主任' },
  { value: 'TEACHER', label: '科任教师' },
  { value: 'GATE', label: '门卫' },
  { value: 'PARENT', label: '家长' },
  { value: 'STUDENT', label: '学生' },
];

const roleLabel = (code) => roles.find(r => r.value === code)?.label || code;
const roleTagType = (code) => ({ ADMIN: 'danger', HEAD_TEACHER: 'warning' }[code] || 'info');
const genderLabel = (g) => ({ MALE: '男', FEMALE: '女', OTHER: '其他' }[g] || g || '-');

const TAB_ROLES = {
  ALL: null,
  ADMIN: 'ADMIN,PRINCIPAL,ACADEMIC,HR,FINANCE',
  TEACHER: 'HEAD_TEACHER,TEACHER',
  PARENT: 'PARENT',
};

const onTabChange = () => {
  pageNo.value = 1;
  teacherRoleFilter.value = '';
  load();
};

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (keyword.value) params.keyword = keyword.value;

    // 根据Tab和子筛选确定roleCode
    if (activeTab.value === 'TEACHER' && teacherRoleFilter.value) {
      params.roleCode = teacherRoleFilter.value;
    } else if (TAB_ROLES[activeTab.value]) {
      params.roleCode = TAB_ROLES[activeTab.value];
    }

    const data = await http.get('/sys/users', { params });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  editId.value = null;
  form.value = { username: '', password: 'Ycd@123456', realName: '', roleCode: 'TEACHER',
    status: 'ACTIVE', mobile: '', gender: '' };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  form.value = {
    realName: row.realName, roleCode: row.roleCode, status: row.status,
    mobile: row.mobileEncrypted || '', gender: row.gender || ''
  };
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (editId.value) {
    await http.put(`/sys/users/${editId.value}`, form.value);
  } else {
    await http.post('/sys/users', form.value);
  }
  ElMessage.success('保存成功');
  dialogVisible.value = false;
  load();
};

const resetPwd = async (id) => {
  const res = await ElMessageBox.confirm('确认重置该用户密码？系统将生成随机密码。', '提示', { type: 'warning' });
  if (res === 'confirm') {
    const data = await http.put(`/sys/users/${id}/reset-password`);
    ElMessageBox.alert(`新密码：${data.newPassword}`, '密码重置成功', { type: 'success' });
  }
};

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' });
  await http.delete(`/sys/users/${id}`);
  ElMessage.success('已删除');
  load();
};
</script>

<style scoped>
.tab-bar { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-top: 12px; }
.pagination { margin-top: 16px; }
</style>
