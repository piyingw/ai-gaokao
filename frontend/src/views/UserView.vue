<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    loading.value = true
    await userStore.fetchUserInfo()
    loading.value = false
  }
})

const menuItems = [
  { icon: 'User', title: '基本信息', desc: '查看和修改个人资料' },
  { icon: 'Document', title: '我的志愿', desc: '管理志愿填报方案' },
  { icon: 'Star', title: '我的收藏', desc: '收藏的院校和专业' },
  { icon: 'Setting', title: '账号设置', desc: '密码和安全设置' }
]

const stats = [
  { label: '收藏院校', value: 12, icon: 'School', color: '#409eff' },
  { label: '收藏专业', value: 8, icon: 'Reading', color: '#67c23a' },
  { label: '生成方案', value: 3, icon: 'Document', color: '#e6a23c' }
]

const handleLogin = () => {
  // 触发全局事件来打开登录弹窗
  window.dispatchEvent(new CustomEvent('openLoginDialog'))
}

const handleLogout = () => {
  userStore.logout()
}
</script>

<template>
  <div class="user-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><User /></el-icon>
          个人中心
        </h1>
      </div>
    </div>

    <div class="container page-content">
      <el-skeleton :loading="loading" animated>
        <template #default>
          <!-- 未登录状态 -->
          <template v-if="!isLoggedIn">
            <el-card class="login-card" shadow="never">
              <div class="login-content">
                <div class="login-icon">
                  <el-icon :size="64"><UserFilled /></el-icon>
                </div>
                <h2>欢迎使用高考志愿填报系统</h2>
                <p>登录后即可享受个性化推荐、志愿管理等功能</p>
                <el-button type="primary" size="large" round @click="handleLogin">
                  <el-icon><User /></el-icon>
                  立即登录
                </el-button>
                <div class="login-tips">
                  <span>还没有账号？</span>
                  <el-link type="primary">立即注册</el-link>
                </div>
              </div>
            </el-card>
          </template>

          <!-- 已登录状态 -->
          <template v-else>
            <el-row :gutter="24">
              <!-- 左侧用户信息 -->
              <el-col :xs="24" :md="8">
                <el-card class="profile-card" shadow="never">
                  <div class="profile-header">
                    <el-avatar :size="80" class="avatar">
                      <el-icon :size="40"><UserFilled /></el-icon>
                    </el-avatar>
                    <h2 class="user-name">{{ userInfo?.name || '用户' }}</h2>
                    <p class="user-email">{{ userInfo?.email || '未设置邮箱' }}</p>
                  </div>

                  <el-divider />

                  <div class="profile-info">
                    <div class="info-item">
                      <el-icon><Phone /></el-icon>
                      <span>{{ userInfo?.phone || '未绑定手机' }}</span>
                    </div>
                    <div class="info-item">
                      <el-icon><Location /></el-icon>
                      <span>{{ userInfo?.province || '未设置省份' }}</span>
                    </div>
                    <div class="info-item">
                      <el-icon><TrendCharts /></el-icon>
                      <span>高考分数: <strong>{{ userInfo?.score || '未填写' }}</strong> 分</span>
                    </div>
                    <div class="info-item">
                      <el-icon><Collection /></el-icon>
                      <span>{{ userInfo?.subjectType || '未选择科类' }}</span>
                    </div>
                  </div>

                  <el-button type="danger" plain round class="logout-btn" @click="handleLogout">
                    退出登录
                  </el-button>
                </el-card>
              </el-col>

              <!-- 右侧内容区 -->
              <el-col :xs="24" :md="16">
                <!-- 统计卡片 -->
                <el-row :gutter="16" class="stats-row">
                  <el-col
                    v-for="(stat, index) in stats"
                    :key="index"
                    :xs="8"
                    :sm="8"
                  >
                    <el-card class="stat-card hover-lift" shadow="hover">
                      <div class="stat-icon" :style="{ background: stat.color }">
                        <el-icon :size="24"><component :is="stat.icon" /></el-icon>
                      </div>
                      <div class="stat-value">{{ stat.value }}</div>
                      <div class="stat-label">{{ stat.label }}</div>
                    </el-card>
                  </el-col>
                </el-row>

                <!-- 功能菜单 -->
                <el-card class="menu-card" shadow="never">
                  <template #header>
                    <span class="card-title">功能菜单</span>
                  </template>

                  <el-row :gutter="16">
                    <el-col
                      v-for="(item, index) in menuItems"
                      :key="index"
                      :xs="12"
                      :sm="12"
                      :md="6"
                    >
                      <div class="menu-item hover-lift">
                        <div class="menu-icon">
                          <el-icon :size="28"><component :is="item.icon" /></el-icon>
                        </div>
                        <h4>{{ item.title }}</h4>
                        <p>{{ item.desc }}</p>
                      </div>
                    </el-col>
                  </el-row>
                </el-card>

                <!-- 快捷操作 -->
                <el-card class="action-card" shadow="never">
                  <template #header>
                    <span class="card-title">快捷操作</span>
                  </template>

                  <div class="action-buttons">
                    <el-button type="primary" size="large" @click="router.push('/ai-assistant')">
                      <el-icon><ChatDotRound /></el-icon>
                      AI智能推荐
                    </el-button>
                    <el-button size="large" @click="router.push('/universities')">
                      <el-icon><Search /></el-icon>
                      搜索院校
                    </el-button>
                    <el-button size="large" @click="router.push('/majors')">
                      <el-icon><Reading /></el-icon>
                      浏览专业
                    </el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </template>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<style scoped>
.user-page {
  min-height: calc(100vh - 200px);
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 3rem 0;
  color: #fff;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.page-content {
  padding: 2rem 0;
}

/* 登录卡片 */
.login-card {
  max-width: 480px;
  margin: 2rem auto;
  border-radius: 24px !important;
  padding: 2rem !important;
}

.login-content {
  text-align: center;
}

.login-icon {
  color: #cbd5e1;
  margin-bottom: 1.5rem;
}

.login-content h2 {
  font-size: 1.5rem;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.login-content p {
  color: #64748b;
  margin: 0 0 2rem;
}

.login-tips {
  margin-top: 1.5rem;
  color: #64748b;
  font-size: 0.9rem;
}

/* 用户信息卡片 */
.profile-card {
  border-radius: 20px !important;
  padding: 1.5rem !important;
  text-align: center;
}

.profile-header {
  padding: 1rem 0;
}

.avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0 auto 1rem;
}

.user-name {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.user-email {
  color: #64748b;
  margin: 0;
  font-size: 0.9rem;
}

.profile-info {
  text-align: left;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0;
  color: #475569;
  border-bottom: 1px solid #f1f5f9;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .el-icon {
  color: #94a3b8;
}

.info-item strong {
  color: #409eff;
}

.logout-btn {
  width: 100%;
  margin-top: 1.5rem;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 1.5rem;
}

.stat-card {
  text-align: center;
  padding: 1.25rem !important;
  border-radius: 16px !important;
  margin-bottom: 1rem;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin: 0 auto 0.75rem;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
}

/* 菜单卡片 */
.menu-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
}

.card-title {
  font-weight: 600;
  color: #1e293b;
}

.menu-item {
  text-align: center;
  padding: 1.5rem 1rem;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 1rem;
}

.menu-item:hover {
  background: #f8fafc;
}

.menu-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin: 0 auto 1rem;
}

.menu-item h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.menu-item p {
  font-size: 0.8rem;
  color: #64748b;
  margin: 0;
}

/* 操作卡片 */
.action-card {
  border-radius: 16px !important;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  flex: 1;
  min-width: 140px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-title {
    font-size: 1.5rem;
  }

  .stats-row .el-col {
    margin-bottom: 0.5rem;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }
}
</style>