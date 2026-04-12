<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { memberApi, orderApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const memberInfo = ref(null)
const privileges = ref([])
const products = ref([])
const purchasing = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 加载会员信息
const fetchMemberInfo = async () => {
  if (!isLoggedIn.value) return

  try {
    loading.value = true
    const [infoRes, privRes, prodRes] = await Promise.all([
      memberApi.getMemberInfo(),
      memberApi.getMemberPrivileges(),
      memberApi.getMemberProducts()
    ])

    memberInfo.value = infoRes.data
    privileges.value = privRes.data
    products.value = prodRes.data
  } catch (error) {
    console.error('获取会员信息失败:', error)
    ElMessage.error('获取会员信息失败')
  } finally {
    loading.value = false
  }
}

// Mock支付购买会员
const purchaseMember = async (productId) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/user')
    return
  }

  try {
    purchasing.value = true

    // 1. 创建订单
    const orderRes = await orderApi.createOrder(productId)

    // 2. Mock支付（直接成功）
    await orderApi.payOrder(orderRes.data.id, 'MOCK')

    // 3. 提示成功
    ElMessage.success('支付成功！会员已激活')

    // 4. 刷新会员信息
    await fetchMemberInfo()
  } catch (error) {
    console.error('购买失败:', error)
    ElMessage.error('购买失败：' + (error.message || '请稍后重试'))
  } finally {
    purchasing.value = false
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '未激活'
  return new Date(date).toLocaleDateString('zh-CN')
}

// 计算剩余天数
const remainingDays = computed(() => {
  if (!memberInfo.value?.endTime) return 0
  const end = new Date(memberInfo.value.endTime)
  const now = new Date()
  const days = Math.ceil((end - now) / (1000 * 60 * 60 * 24))
  return days > 0 ? days : 0
})

onMounted(() => {
  fetchMemberInfo()
})
</script>

<template>
  <div class="member-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Medal /></el-icon>
          会员中心
        </h1>
        <p class="page-subtitle">解锁更多功能，享受专属权益</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 未登录提示 -->
      <el-card v-if="!isLoggedIn" class="login-tip-card" shadow="never">
        <div class="login-tip-content">
          <el-icon :size="64"><UserFilled /></el-icon>
          <h2>请先登录</h2>
          <p>登录后可查看会员信息并购买会员</p>
          <el-button type="primary" size="large" @click="router.push('/user')">
            前往登录
          </el-button>
        </div>
      </el-card>

      <!-- 已登录状态 -->
      <template v-else>
        <el-skeleton :loading="loading" animated>
          <template #default>
            <!-- 会员信息卡片 -->
            <el-card class="member-info-card" shadow="never">
              <div class="member-header">
                <div class="member-badge" :class="memberInfo?.levelCode || 'FREE'">
                  <el-icon :size="48"><Medal /></el-icon>
                </div>
                <div class="member-details">
                  <h2 class="member-level">{{ memberInfo?.levelName || '免费用户' }}</h2>
                  <div class="member-status">
                    <el-tag :type="memberInfo?.valid ? 'success' : 'info'" effect="light">
                      {{ memberInfo?.valid ? '已激活' : '未激活' }}
                    </el-tag>
                    <span v-if="memberInfo?.valid && remainingDays > 0" class="remaining-days">
                      剩余 {{ remainingDays }} 天
                    </span>
                  </div>
                  <div class="member-time">
                    <span v-if="memberInfo?.startTime">
                      激活时间：{{ formatDate(memberInfo.startTime) }}
                    </span>
                    <span v-if="memberInfo?.endTime">
                      有效期至：{{ formatDate(memberInfo.endTime) }}
                    </span>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 会员权益 -->
            <el-card class="privileges-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <el-icon><Star /></el-icon>
                  <span>会员权益</span>
                </div>
              </template>

              <el-row :gutter="16">
                <el-col
                  v-for="priv in privileges"
                  :key="priv.privilegeCode"
                  :xs="24"
                  :sm="12"
                  :md="6"
                >
                  <div class="privilege-item">
                    <div class="privilege-name">{{ priv.privilegeName }}</div>
                    <div class="privilege-limit">
                      每日 {{ priv.limitCount === -1 ? '无限' : priv.limitCount }} 次
                    </div>
                    <div class="privilege-used">
                      今日已用 {{ priv.usedCount || 0 }} 次
                    </div>
                    <el-progress
                      :percentage="priv.limitCount === -1 ? 0 : (priv.usedCount / priv.limitCount) * 100"
                      :stroke-width="8"
                      :show-text="false"
                      status="success"
                    />
                  </div>
                </el-col>
              </el-row>
            </el-card>

            <!-- 会员商品 -->
            <el-card class="products-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <el-icon><ShoppingCart /></el-icon>
                  <span>会员商品</span>
                </div>
              </template>

              <el-row :gutter="24">
                <el-col
                  v-for="product in products"
                  :key="product.id"
                  :xs="24"
                  :sm="12"
                  :md="6"
                >
                  <el-card class="product-card hover-lift" shadow="hover">
                    <div class="product-header">
                      <h3 class="product-name">{{ product.name }}</h3>
                      <div class="product-duration">{{ product.durationDays }}天</div>
                    </div>

                    <div class="product-price">
                      <span class="current-price">¥{{ product.price }}</span>
                      <span class="original-price">¥{{ product.originalPrice }}</span>
                    </div>

                    <div class="product-desc">{{ product.description }}</div>

                    <el-button
                      type="primary"
                      size="large"
                      round
                      :loading="purchasing"
                      @click="purchaseMember(product.id)"
                    >
                      立即购买
                    </el-button>
                  </el-card>
                </el-col>
              </el-row>
            </el-card>

            <!-- 功能说明 -->
            <el-card class="features-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <el-icon><InfoFilled /></el-icon>
                  <span>会员等级说明</span>
                </div>
              </template>

              <el-table :data="[
                { level: '免费用户', features: '每日10次AI对话', price: '免费' },
                { level: '普通会员', features: '每日50次AI对话、智能推荐、详细录取数据', price: '¥19/月 或 ¥98/年' },
                { level: 'VIP会员', features: '无限AI对话、一键生成志愿、专家答疑', price: '¥49/月 或 ¥298/年' }
              ]" style="width: 100%">
                <el-table-column prop="level" label="等级" width="120" />
                <el-table-column prop="features" label="权益" />
                <el-table-column prop="price" label="价格" width="180" />
              </el-table>
            </el-card>
          </template>
        </el-skeleton>
      </template>
    </div>
  </div>
</template>

<style scoped>
.member-page {
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
  margin: 0 0 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.page-subtitle {
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
}

.page-content {
  padding: 2rem 0;
}

/* 登录提示卡片 */
.login-tip-card {
  max-width: 480px;
  margin: 2rem auto;
  border-radius: 24px !important;
  padding: 2rem !important;
}

.login-tip-content {
  text-align: center;
}

.login-tip-content .el-icon {
  color: #cbd5e1;
  margin-bottom: 1.5rem;
}

.login-tip-content h2 {
  font-size: 1.5rem;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.login-tip-content p {
  color: #64748b;
  margin: 0 0 2rem;
}

/* 会员信息卡片 */
.member-info-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.member-badge {
  width: 100px;
  height: 100px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.member-badge.FREE {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.member-badge.NORMAL {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.member-badge.VIP {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.member-details {
  flex: 1;
}

.member-level {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.member-status {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.remaining-days {
  color: #22c55e;
  font-weight: 500;
}

.member-time {
  color: #64748b;
  font-size: 0.9rem;
}

.member-time span {
  margin-right: 1rem;
}

/* 权益卡片 */
.privileges-card,
.products-card,
.features-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1e293b;
}

.privilege-item {
  padding: 1rem;
  border-radius: 12px;
  background: #f8fafc;
  margin-bottom: 1rem;
}

.privilege-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.privilege-limit {
  color: #64748b;
  font-size: 0.85rem;
}

.privilege-used {
  color: #409eff;
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
}

/* 商品卡片 */
.product-card {
  text-align: center;
  padding: 1.5rem !important;
  border-radius: 16px !important;
  margin-bottom: 1rem;
}

.product-header {
  margin-bottom: 1rem;
}

.product-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.product-duration {
  color: #64748b;
  font-size: 0.85rem;
}

.product-price {
  margin-bottom: 1rem;
}

.current-price {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ef4444;
}

.original-price {
  font-size: 1rem;
  color: #94a3b8;
  margin-left: 0.5rem;
  text-decoration: line-through;
}

.product-desc {
  color: #64748b;
  font-size: 0.85rem;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.product-card .el-button {
  width: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .member-header {
    flex-direction: column;
    text-align: center;
  }

  .member-time {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }
}
</style>