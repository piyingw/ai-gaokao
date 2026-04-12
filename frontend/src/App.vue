<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeIndex = computed(() => route.path)
const showLoginDialog = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)
const isRegister = ref(false)
const countdown = ref(0)

// 登录表单
const loginForm = ref({
  username: '',
  password: ''
})

// 注册表单
const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
})

const navItems = [
  { path: '/', title: '首页', icon: 'HomeFilled' },
  { path: '/universities', title: '院校查询', icon: 'School' },
  { path: '/majors', title: '专业查询', icon: 'Reading' },
  { path: '/ai-assistant', title: 'AI助手', icon: 'ChatDotRound' },
  { path: '/user', title: '个人中心', icon: 'User' }
]

const handleSelect = (path) => {
  router.push(path)
}

// 打开登录对话框
const openLoginDialog = () => {
  if (userStore.isLoggedIn) {
    router.push('/user')
  } else {
    showLoginDialog.value = true
    isRegister.value = false
  }
}

// 切换登录/注册
const toggleAuthMode = () => {
  isRegister.value = !isRegister.value
}

// 登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }

  loginLoading.value = true
  try {
    await userStore.login({
      username: loginForm.value.username,
      password: loginForm.value.password
    })
    ElMessage.success('登录成功')
    showLoginDialog.value = false
    loginForm.value = { username: '', password: '' }
  } catch (error) {
    ElMessage.error('登录失败：' + (error.message || '请检查用户名和密码'))
  } finally {
    loginLoading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!registerForm.value.code) {
    ElMessage.warning('请输入验证码')
    return
  }

  registerLoading.value = true
  try {
    await userStore.register({
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password,
      code: registerForm.value.code
    })
    ElMessage.success('注册成功，请登录')
    isRegister.value = false
    registerForm.value = { username: '', email: '', password: '', confirmPassword: '', code: '' }
  } catch (error) {
    ElMessage.error('注册失败：' + (error.message || '请稍后重试'))
  } finally {
    registerLoading.value = false
  }
}

// 发送验证码
const sendCode = async () => {
  if (!registerForm.value.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  // 检查邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  // 检查倒计时是否结束
  if (countdown.value > 0) {
    ElMessage.warning(`请等待 ${countdown.value} 秒后再次发送`)
    return
  }

  try {
    await userStore.sendEmailCode(registerForm.value.email)
    ElMessage.success('验证码已发送，请注意查收')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送失败：' + (error.message || '请稍后重试'))
  }
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

// 关闭对话框时重置表单
const closeDialog = () => {
  loginForm.value = { username: '', password: '' }
  registerForm.value = { username: '', email: '', password: '', confirmPassword: '', code: '' }
}

onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
  
  // 监听来自子组件的打开登录弹窗事件
  window.addEventListener('openLoginDialog', openLoginDialog)
})

// 组件卸载前移除事件监听器
onUnmounted(() => {
  window.removeEventListener('openLoginDialog', openLoginDialog)
})
</script>

<template>
  <el-container class="app-container">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-content">
        <!-- Logo 区域 -->
        <div class="logo-section" @click="router.push('/')">
          <el-icon class="logo-icon"><GraduationCap /></el-icon>
          <div class="logo-text">
            <span class="logo-title">高考志愿填报系统</span>
            <span class="logo-subtitle">智能选校 · 科学填报</span>
          </div>
        </div>

        <!-- 导航菜单 -->
        <el-menu
          :default-active="activeIndex"
          mode="horizontal"
          class="nav-menu"
          :ellipsis="false"
          @select="handleSelect"
        >
          <el-menu-item
            v-for="item in navItems"
            :key="item.path"
            :index="item.path"
            class="nav-item"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>

        <!-- 用户操作区 -->
        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="(cmd) => cmd === 'logout' && handleLogout()">
              <div class="user-info-dropdown">
                <el-avatar :size="36" class="user-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile" @click="router.push('/user')">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/member')">
                    <el-icon><Medal /></el-icon>
                    会员中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/my-applications')">
                    <el-icon><Document /></el-icon>
                    我的志愿
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/my-orders')">
                    <el-icon><ShoppingCart /></el-icon>
                    我的订单
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/coupons')">
                    <el-icon><Ticket /></el-icon>
                    我的优惠券
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/policy')">
                    <el-icon><Reading /></el-icon>
                    政策文档
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="openLoginDialog">
              <el-icon><User /></el-icon>
              登录
            </el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主内容区域 -->
    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <!-- 底部信息 -->
    <el-footer class="app-footer">
      <div class="footer-content">
        <div class="footer-info">
          <p>© 2026 高考志愿填报系统 - 用AI助力每一位学子的梦想</p>
        </div>
        <div class="footer-links">
          <el-link type="info" :underline="false">关于我们</el-link>
          <el-divider direction="vertical" />
          <el-link type="info" :underline="false">帮助中心</el-link>
          <el-divider direction="vertical" />
          <el-link type="info" :underline="false">联系我们</el-link>
        </div>
      </div>
    </el-footer>

    <!-- 登录/注册对话框 -->
    <el-dialog
      v-model="showLoginDialog"
      :title="isRegister ? '用户注册' : '用户登录'"
      width="420px"
      :close-on-click-modal="false"
      @closed="closeDialog"
      class="auth-dialog"
    >
      <!-- 登录表单 -->
      <el-form v-if="!isRegister" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loginLoading"
          class="submit-btn"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <!-- 注册表单 -->
      <el-form v-else label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input
              v-model="registerForm.code"
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large"
              maxlength="6"
            />
            <el-button
              size="large"
              :disabled="countdown > 0 || !registerForm.email"
              @click="sendCode"
            >
              <template v-if="countdown > 0">
                <el-icon><Timer /></el-icon>
                {{ countdown }}s后重发
              </template>
              <template v-else>
                <el-icon><Position /></el-icon>
                获取验证码
              </template>
            </el-button>
          </div>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="registerLoading"
          class="submit-btn"
          @click="handleRegister"
        >
          注册
        </el-button>
      </el-form>

      <div class="auth-footer">
        <span v-if="!isRegister">还没有账号？</span>
        <span v-else>已有账号？</span>
        <el-link type="primary" @click="toggleAuthMode">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </el-link>
      </div>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 头部样式 */
.app-header {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 50%, #667eea 100%);
  height: auto !important;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0.75rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

/* Logo 样式 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.logo-section:hover {
  transform: scale(1.02);
}

.logo-icon {
  font-size: 2.5rem;
  color: #fff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.logo-subtitle {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 400;
}

/* 导航菜单样式 */
.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  background: transparent !important;
  border: none !important;
}

.nav-menu .nav-item {
  color: rgba(255, 255, 255, 0.85) !important;
  font-weight: 500;
  font-size: 0.95rem;
  padding: 0 1.25rem;
  height: 50px;
  line-height: 50px;
  border-radius: 8px;
  margin: 0 0.25rem;
  transition: all 0.3s ease;
}

.nav-menu .nav-item:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
}

.nav-menu .nav-item.is-active {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  border-bottom: 3px solid #fff !important;
}

.nav-menu .nav-item .el-icon {
  margin-right: 0.5rem;
  font-size: 1.1rem;
}

/* 用户操作区 */
.user-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-actions .el-button {
  font-weight: 500;
}

.user-info-dropdown {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  transition: background 0.3s ease;
}

.user-info-dropdown:hover {
  background: rgba(255, 255, 255, 0.15);
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.user-name {
  color: #fff;
  font-weight: 500;
}

.user-info-dropdown .arrow {
  color: rgba(255, 255, 255, 0.7);
}

/* 主内容区域 */
.app-main {
  flex: 1;
  padding: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: calc(100vh - 140px);
}

/* 底部样式 */
.app-footer {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  height: auto !important;
  padding: 1.5rem 0;
  color: rgba(255, 255, 255, 0.8);
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.footer-info p {
  margin: 0;
  font-size: 0.9rem;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.footer-links .el-link {
  color: rgba(255, 255, 255, 0.8) !important;
  font-size: 0.9rem;
}

.footer-links .el-link:hover {
  color: #fff !important;
}

.footer-links .el-divider {
  border-color: rgba(255, 255, 255, 0.3);
}

/* 登录对话框 */
.auth-dialog :deep(.el-dialog__header) {
  text-align: center;
  font-weight: 600;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 0 1.5rem 1.5rem;
}

.submit-btn {
  width: 100%;
  margin-top: 0.5rem;
}

.code-row {
  display: flex;
  gap: 0.75rem;
}

.code-row .el-input {
  flex: 1;
}

.auth-footer {
  text-align: center;
  margin-top: 1.5rem;
  color: #64748b;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .header-content {
    flex-wrap: wrap;
    padding: 0.75rem 1rem;
  }

  .nav-menu {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
  }

  .nav-menu .nav-item {
    padding: 0 1rem;
    font-size: 0.85rem;
  }
}

@media (max-width: 768px) {
  .logo-text {
    display: none;
  }

  .logo-icon {
    font-size: 2rem;
  }

  .nav-menu .nav-item span:not(.el-icon) {
    display: none;
  }

  .nav-menu .nav-item {
    padding: 0 0.75rem;
  }

  .user-actions .el-button span:not(.el-icon) {
    display: none;
  }

  .user-name {
    display: none;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
  }
}
</style>