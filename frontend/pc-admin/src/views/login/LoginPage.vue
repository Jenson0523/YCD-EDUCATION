<template>
  <div class="login-bg">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-title">云辰盾</div>
        <div class="logo-sub">家校共育共同体综合管理平台</div>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width:100%;margin-top:8px"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
      <div class="login-tip">默认账号：admin / Admin@123</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref(null);
const loading = ref(false);

const form = reactive({ username: '', password: '' });
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleLogin = async () => {
  await formRef.value?.validate();
  loading.value = true;
  try {
    await authStore.login(form.username, form.password);
    ElMessage.success('登录成功');
    router.replace('/dashboard');
  } catch {
    // error already shown by http interceptor
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(135deg, #1b5ea6 0%, #0d3d73 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 400px;
  padding: 48px 40px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-title {
  font-size: 32px;
  font-weight: 700;
  color: #1b5ea6;
  letter-spacing: 4px;
}

.logo-sub {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}

.login-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}
</style>
