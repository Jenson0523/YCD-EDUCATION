<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>
    <div class="panel">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="姓名/用户名" clearable style="width:200px" @keyup.enter="load" />
        <el-select v-model="roleFilter" placeholder="角色" clearable style="width:140px">
          <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTagType(row.roleCode)">{{ roleLabel(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
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
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!editId" label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="!editId" label="初始密码"><el-input v-model="form.password" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
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
const roleFilter = ref('');
const dialogVisible = ref(false);
const editId = ref(null);
const form = ref({ username: '', password: '', realName: '', roleCode: 'TEACHER', status: 'ACTIVE' });

const roles = [
  { value: 'ADMIN', label: '系统管理员' },
  { value: 'PRINCIPAL', label: '校长/股东' },
  { value: 'ACADEMIC', label: '教务处' },
  { value: 'HR', label: '人事专员' },
  { value: 'FINANCE', label: '财务专员' },
  { value: 'HEAD_TEACHER', label: '班主任' },
  { value: 'TEACHER', label: '科任教师' },
  { value: 'PARENT', label: '家长' },
  { value: 'STUDENT', label: '学生' },
];

const roleLabel = (code) => roles.find(r => r.value === code)?.label || code;
const roleTagType = (code) => ({ ADMIN: 'danger', PRINCIPAL: '', HEAD_TEACHER: 'warning' }[code] || 'info');

const load = async () => {
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (keyword.value) params.keyword = keyword.value;
    if (roleFilter.value) params.roleCode = roleFilter.value;
    const data = await http.get('/sys/users', { params });
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch { rows.value = []; }
};

onMounted(load);

const openCreate = () => {
  editId.value = null;
  form.value = { username: '', password: 'Ycd@123456', realName: '', roleCode: 'TEACHER', status: 'ACTIVE' };
  dialogVisible.value = true;
};

const openEdit = (row) => {
  editId.value = row.id;
  form.value = { realName: row.realName, roleCode: row.roleCode, status: row.status };
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
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 16px; }
</style>
