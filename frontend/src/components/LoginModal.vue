<template>
  <el-dialog
    v-model="dialogVisible"
    title="用户登录"
    width="400px"
    :close-on-click-modal="false"
    :show-close="true"
    @closed="handleClose"
  >
    <el-form
      :model="loginForm"
      :rules="loginRules"
      ref="loginFormRef"
      label-width="80px"
      size="large"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="loginForm.username"
          placeholder="请输入用户名或邮箱"
          prefix-icon="User"
          @keyup.enter="handleLogin"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="请输入密码"
          prefix-icon="Lock"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" size="large">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleLogin" 
          :loading="loading" 
          size="large"
        >
          登录
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 定义 emits
const emit = defineEmits(['close', 'login-success'])

// 用户状态
const userStore = useUserStore()

// 响应式数据
const dialogVisible = ref(false)
const loading = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})

// 登录表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符之间', trigger: 'blur' }
  ]
}

// 表单引用
const loginFormRef = ref()

// 显示登录弹窗
const showLogin = () => {
  dialogVisible.value = true
}

// 处理登录
const handleLogin = async () => {
  // 验证表单
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    // 调用用户store的登录方法
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    ElMessage.success('登录成功')
    dialogVisible.value = false
    emit('login-success')
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// 处理关闭
const handleClose = () => {
  dialogVisible.value = false
  emit('close')
  
  // 清空表单
  Object.assign(loginForm, {
    username: '',
    password: ''
  })
  
  // 清除验证错误
  if (loginFormRef.value) {
    loginFormRef.value.clearValidate()
  }
}

// 暴露方法给父组件
defineExpose({
  showLogin
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>