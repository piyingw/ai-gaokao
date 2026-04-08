<script setup>
import { ref } from 'vue'
import { userApi } from '@/services/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const email = ref('')
const password = ref('')
const showRegister = ref(false)
const registering = ref(false)

// 登录表单数据
const loginForm = ref({
  email: '',
  password: ''
})

// 注册表单数据
const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const handleLogin = async () => {
  try {
    const response = await userApi.login({
      email: loginForm.value.email,
      password: loginForm.value.password
    })
    
    // 存储token
    localStorage.setItem('token', response.data.token)
    
    // 跳转到首页
    router.push('/')
  } catch (error) {
    alert('登录失败: ' + error.message)
  }
}

const handleRegister = async () => {
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  
  try {
    registering.value = true
    await userApi.register({
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password,
      code: registerForm.value.verificationCode
    })
    
    alert('注册成功，请登录')
    showRegister.value = false
  } catch (error) {
    alert('注册失败: ' + error.message)
  } finally {
    registering.value = false
  }
}

const sendVerificationCode = async () => {
  try {
    await userApi.sendEmailCode(registerForm.value.email)
    alert('验证码已发送至您的邮箱')
  } catch (error) {
    alert('发送验证码失败: ' + error.message)
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-form">
      <h2>{{ showRegister ? '用户注册' : '用户登录' }}</h2>
      
      <!-- 登录表单 -->
      <form v-if="!showRegister" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            id="email" 
            v-model="loginForm.email" 
            type="email" 
            required 
            placeholder="请输入邮箱"
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password" 
            v-model="loginForm.password" 
            type="password" 
            required 
            placeholder="请输入密码"
          />
        </div>
        
        <button type="submit" class="submit-btn">登录</button>
        
        <p class="toggle-auth">
          还没有账号？<a href="#" @click.prevent="showRegister = true">立即注册</a>
        </p>
      </form>
      
      <!-- 注册表单 -->
      <form v-else @submit.prevent="handleRegister" class="register-form">
        <div class="form-group">
          <label for="reg-username">用户名</label>
          <input 
            id="reg-username" 
            v-model="registerForm.username" 
            type="text" 
            required 
            placeholder="请输入用户名"
          />
        </div>
        
        <div class="form-group">
          <label for="reg-email">邮箱</label>
          <input 
            id="reg-email" 
            v-model="registerForm.email" 
            type="email" 
            required 
            placeholder="请输入邮箱"
          />
        </div>
        
        <div class="form-group">
          <label for="reg-password">密码</label>
          <input 
            id="reg-password" 
            v-model="registerForm.password" 
            type="password" 
            required 
            placeholder="请输入密码"
          />
        </div>
        
        <div class="form-group">
          <label for="reg-confirm-password">确认密码</label>
          <input 
            id="reg-confirm-password" 
            v-model="registerForm.confirmPassword" 
            type="password" 
            required 
            placeholder="请再次输入密码"
          />
        </div>
        
        <div class="form-group">
          <label for="reg-code">验证码</label>
          <div class="code-input-group">
            <input 
              id="reg-code" 
              v-model="registerForm.verificationCode" 
              type="text" 
              required 
              placeholder="请输入验证码"
              maxlength="6"
            />
            <button 
              type="button" 
              @click="sendVerificationCode" 
              class="code-btn"
              :disabled="!registerForm.email"
            >
              获取验证码
            </button>
          </div>
        </div>
        
        <button type="submit" class="submit-btn" :disabled="registering">
          {{ registering ? '注册中...' : '注册' }}
        </button>
        
        <p class="toggle-auth">
          已有账号？<a href="#" @click.prevent="showRegister = false">立即登录</a>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 1rem;
}

.auth-form {
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.auth-form h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #1e293b;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #334155;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.code-input-group {
  display: flex;
  gap: 0.5rem;
}

.code-btn {
  flex-shrink: 0;
  background-color: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 0.75rem 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
}

.code-btn:hover:not(:disabled) {
  background-color: #cbd5e1;
}

.code-btn:disabled {
  background-color: #f1f5f9;
  cursor: not-allowed;
}

.submit-btn {
  width: 100%;
  background-color: #3b82f6;
  color: white;
  border: none;
  padding: 0.75rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}

.submit-btn:hover:not(:disabled) {
  background-color: #2563eb;
}

.submit-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

.toggle-auth {
  text-align: center;
  margin-top: 1rem;
  color: #64748b;
}

.toggle-auth a {
  color: #3b82f6;
  text-decoration: none;
}

.toggle-auth a:hover {
  text-decoration: underline;
}
</style>