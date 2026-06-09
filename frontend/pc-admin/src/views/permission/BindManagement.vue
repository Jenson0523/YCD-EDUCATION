<template>
  <section class="page">
    <div class="page-header">
      <h1 class="page-title">数据权限绑定</h1>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="👨‍🏫 教师 ↔ 班级" name="teacher" />
      <el-tab-pane label="👨‍👩‍👧 家长 ↔ 学生" name="parent" />
    </el-tabs>

    <!-- 教师班级绑定 -->
    <div v-if="activeTab === 'teacher'" class="panel">
      <div class="filter-bar">
        <el-input v-model="tcKeyword" placeholder="教师姓名" clearable style="width:180px" @keyup.enter="loadTC" />
        <el-button type="primary" @click="loadTC">查询</el-button>
        <el-button type="success" @click="openTcDialog">+ 新增绑定</el-button>
      </div>
      <el-table :data="tcRows" border stripe>
        <el-table-column prop="teacherName" label="教师" width="120" />
        <el-table-column prop="teacherUserId" label="教师ID" width="100" />
        <el-table-column prop="className" label="班级" width="160" />
        <el-table-column prop="classId" label="班级ID" width="100" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isHeadTeacher ? 'warning' : 'info'" size="small">
              {{ row.isHeadTeacher ? '班主任' : '任课' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="任教学科" width="120" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="unbindTC(row.id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="tcPage" :page-size="20" :total="tcTotal"
        layout="total, prev, pager, next" @change="loadTC" />
    </div>

    <!-- 家长学生绑定 -->
    <div v-if="activeTab === 'parent'" class="panel">
      <div class="filter-bar">
        <el-input v-model="psKeyword" placeholder="家长/学生姓名" clearable style="width:180px" @keyup.enter="loadPS" />
        <el-button type="primary" @click="loadPS">查询</el-button>
        <el-button type="success" @click="openPsDialog">+ 新增绑定</el-button>
      </div>
      <el-table :data="psRows" border stripe>
        <el-table-column prop="parentName" label="家长" width="120" />
        <el-table-column prop="parentUserId" label="家长ID" width="100" />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="studentNo" label="学籍号" width="140" />
        <el-table-column label="关系" width="100">
          <template #default="{ row }">{{ relationLabel(row.relation) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="unbindPS(row.id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="psPage" :page-size="20" :total="psTotal"
        layout="total, prev, pager, next" @change="loadPS" />
    </div>

    <!-- 教师绑定弹窗 -->
    <el-dialog v-model="tcDialog" title="新增教师班级绑定" width="460px">
      <el-form :model="tcForm" label-width="90px">
        <el-form-item label="教师">
          <el-select v-model="tcForm.teacherUserId" filterable placeholder="选择教师" style="width:100%"
                     @change="onTeacherPick">
            <el-option v-for="u in teachers" :key="u.id" :label="`${u.realName}(${roleLabel(u.roleCode)})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="tcForm.classId" filterable placeholder="选择班级" style="width:100%" @change="onClassPick">
            <el-option v-for="c in classes" :key="c.id" :label="c.className || c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="tcForm.isHeadTeacher">
            <el-radio :value="1">班主任</el-radio>
            <el-radio :value="0">任课老师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="任教学科">
          <el-input v-model="tcForm.subjectName" placeholder="如：语文（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tcDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTC">保存</el-button>
      </template>
    </el-dialog>

    <!-- 家长绑定弹窗 -->
    <el-dialog v-model="psDialog" title="新增家长学生绑定" width="460px">
      <el-form :model="psForm" label-width="90px">
        <el-form-item label="家长">
          <el-select v-model="psForm.parentUserId" filterable placeholder="选择家长账号" style="width:100%"
                     @change="onParentPick">
            <el-option v-for="u in parents" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生">
          <el-select v-model="psForm.studentId" filterable placeholder="选择学生" style="width:100%" @change="onStudentPick">
            <el-option v-for="s in students" :key="s.id" :label="`${s.name}(${s.studentNo})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="psForm.relation" style="width:100%">
            <el-option label="父亲" value="FATHER" />
            <el-option label="母亲" value="MOTHER" />
            <el-option label="其他监护人" value="OTHER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="psDialog = false">取消</el-button>
        <el-button type="primary" @click="savePS">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { http } from '../../api/http';

const activeTab = ref('teacher');

// ===== 教师班级 =====
const tcRows = ref([]); const tcPage = ref(1); const tcTotal = ref(0); const tcKeyword = ref('');
const tcDialog = ref(false);
const tcForm = ref({ teacherUserId: null, teacherName: '', classId: null, className: '', isHeadTeacher: 1, subjectName: '' });

const loadTC = async () => {
  const data = await http.get('/permission/teacher-class', { params: { pageNo: tcPage.value, pageSize: 20, keyword: tcKeyword.value || undefined } });
  tcRows.value = data?.records || []; tcTotal.value = data?.total || 0;
};

// ===== 家长学生 =====
const psRows = ref([]); const psPage = ref(1); const psTotal = ref(0); const psKeyword = ref('');
const psDialog = ref(false);
const psForm = ref({ parentUserId: null, parentName: '', studentId: null, studentNo: '', studentName: '', relation: 'FATHER' });

const loadPS = async () => {
  const data = await http.get('/permission/parent-student', { params: { pageNo: psPage.value, pageSize: 20, keyword: psKeyword.value || undefined } });
  psRows.value = data?.records || []; psTotal.value = data?.total || 0;
};

// ===== 下拉数据源 =====
const teachers = ref([]); const parents = ref([]); const classes = ref([]); const students = ref([]);

const loadOptions = async () => {
  const [uData, cData, sData] = await Promise.all([
    http.get('/sys/users', { params: { pageNo: 1, pageSize: 200 } }),
    http.get('/academic/classes', { params: { pageNo: 1, pageSize: 200 } }).catch(() => ({ records: [] })),
    http.get('/stu/students', { params: { pageNo: 1, pageSize: 500 } }),
  ]);
  const users = uData?.records || [];
  teachers.value = users.filter(u => ['HEAD_TEACHER', 'TEACHER'].includes(u.roleCode));
  parents.value = users.filter(u => u.roleCode === 'PARENT');
  classes.value = cData?.records || [];
  students.value = sData?.records || [];
};

const openTcDialog = () => {
  tcForm.value = { teacherUserId: null, teacherName: '', classId: null, className: '', isHeadTeacher: 1, subjectName: '' };
  tcDialog.value = true;
};
const openPsDialog = () => {
  psForm.value = { parentUserId: null, parentName: '', studentId: null, studentNo: '', studentName: '', relation: 'FATHER' };
  psDialog.value = true;
};

const onTeacherPick = (id) => { tcForm.value.teacherName = teachers.value.find(t => t.id === id)?.realName || ''; };
const onClassPick = (id) => { const c = classes.value.find(c => c.id === id); tcForm.value.className = c?.className || c?.name || ''; };
const onParentPick = (id) => { psForm.value.parentName = parents.value.find(p => p.id === id)?.realName || ''; };
const onStudentPick = (id) => { const s = students.value.find(s => s.id === id); psForm.value.studentNo = s?.studentNo || ''; psForm.value.studentName = s?.name || ''; };

const saveTC = async () => {
  if (!tcForm.value.teacherUserId || !tcForm.value.classId) { ElMessage.warning('请选择教师和班级'); return; }
  await http.post('/permission/teacher-class', tcForm.value);
  ElMessage.success('绑定成功'); tcDialog.value = false; loadTC();
};
const savePS = async () => {
  if (!psForm.value.parentUserId || !psForm.value.studentId) { ElMessage.warning('请选择家长和学生'); return; }
  await http.post('/permission/parent-student', psForm.value);
  ElMessage.success('绑定成功'); psDialog.value = false; loadPS();
};

const unbindTC = async (id) => {
  await ElMessageBox.confirm('确认解除该绑定？', '提示', { type: 'warning' });
  await http.delete(`/permission/teacher-class/${id}`); ElMessage.success('已解绑'); loadTC();
};
const unbindPS = async (id) => {
  await ElMessageBox.confirm('确认解除该绑定？', '提示', { type: 'warning' });
  await http.delete(`/permission/parent-student/${id}`); ElMessage.success('已解绑'); loadPS();
};

const roleLabel = (c) => ({ HEAD_TEACHER: '班主任', TEACHER: '科任' }[c] || c);
const relationLabel = (r) => ({ FATHER: '父亲', MOTHER: '母亲', OTHER: '其他监护人' }[r] || r);

onMounted(() => { loadTC(); loadPS(); loadOptions(); });
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 16px; }
</style>
