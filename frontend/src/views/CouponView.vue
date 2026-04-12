<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { couponApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const templates = ref([])
const myCoupons = ref([])
const activeTab = ref('available')
const claiming = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 状态选项
const statusOptions = [
  { value: 'available', label: '可领取' },
  { value: 'my', label: '我的优惠券' }
]

// 过滤我的优惠券
const filteredMyCoupons = computed(() => {
  if (activeTab.value !== 'my') return []
  return myCoupons.value.filter(coupon => {
    if (coupon.status === 0) return true // 未使用
    if (coupon.status === 1) return false // 已使用
    if (coupon.status === 2) return false // 已过期
    return true
  })
})

// 加载数据
const fetchData = async () => {
  if (!isLoggedIn.value) return

  try {
    loading.value = true
    const [templatesRes, myCouponsRes] = await Promise.all([
      couponApi.getTemplates(),
      couponApi.getMyCoupons()
    ])
    templates.value = templatesRes.data || []
    myCoupons.value = myCouponsRes.data || []
  } catch (error) {
    console.error('获取优惠券数据失败:', error)
    ElMessage.error('获取优惠券数据失败')
  } finally {
    loading.value = false
  }
}

// 领取优惠券
const claimCoupon = async (templateId) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/user')
    return
  }

  try {
    claiming.value = true
    const res = await couponApi.claimCoupon(templateId)
    ElMessage.success('领取成功！')
    myCoupons.value.push(res.data)
  } catch (error) {
    ElMessage.error('领取失败：' + (error.message || '请稍后重试'))
  } finally {
    claiming.value = false
  }
}

// 检查是否已领取
const isClaimed = (templateId) => {
  return myCoupons.value.some(coupon => coupon.templateId === templateId)
}

// 获取优惠券类型名称
const getCouponTypeName = (type) => {
  const typeMap = {
    1: '折扣券',
    2: '满减券'
  }
  return typeMap[type] || '优惠券'
}

// 获取状态名称
const getStatusName = (status) => {
  const statusMap = {
    0: '未使用',
    1: '已使用',
    2: '已过期'
  }
  return statusMap[status] || '未知'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="coupon-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Ticket /></el-icon>
          优惠券中心
        </h1>
        <p class="page-subtitle">领取优惠券，享受更多优惠</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 未登录提示 -->
      <el-card v-if="!isLoggedIn" class="login-tip-card" shadow="never">
        <div class="login-tip-content">
          <el-icon :size="64"><UserFilled /></el-icon>
          <h2>请先登录</h2>
          <p>登录后可领取优惠券</p>
          <el-button type="primary" size="large" @click="router.push('/user')">
            前往登录
          </el-button>
        </div>
      </el-card>

      <!-- 已登录状态 -->
      <template v-else>
        <!-- 状态筛选 -->
        <el-tabs v-model="activeTab" class="status-tabs">
          <el-tab-pane
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :name="option.value"
          />
        </el-tabs>

        <el-skeleton :loading="loading" animated>
          <template #default>
            <!-- 可领取的优惠券 -->
            <template v-if="activeTab === 'available'">
              <el-empty v-if="templates.length === 0" description="暂无可领取的优惠券" />

              <el-row v-else :gutter="24">
                <el-col
                  v-for="template in templates"
                  :key="template.id"
                  :xs="24"
                  :sm="12"
                  :md="8"
                  :lg="6"
                >
                  <el-card class="coupon-card hover-lift" shadow="hover">
                    <div class="coupon-type">{{ getCouponTypeName(template.type) }}</div>
                    <div class="coupon-value">
                      <span v-if="template.type === 2" class="amount">¥{{ template.value }}</span>
                      <span v-else class="amount">{{ template.value * 10 }}折</span>
                    </div>
                    <div class="coupon-name">{{ template.name }}</div>
                    <div class="coupon-condition">
                      满 {{ template.minAmount || 0 }} 元可用
                    </div>
                    <div class="coupon-time">
                      {{ formatDate(template.startTime) }} - {{ formatDate(template.endTime) }}
                    </div>

                    <el-button
                      v-if="isClaimed(template.id)"
                      type="info"
                      size="small"
                      disabled
                      round
                    >
                      已领取
                    </el-button>
                    <el-button
                      v-else
                      type="primary"
                      size="small"
                      :loading="claiming"
                      round
                      @click="claimCoupon(template.id)"
                    >
                      立即领取
                    </el-button>
                  </el-card>
                </el-col>
              </el-row>
            </template>

            <!-- 我的优惠券 -->
            <template v-else>
              <el-empty v-if="myCoupons.length === 0" description="暂无优惠券">
                <el-button type="primary" @click="activeTab = 'available'">
                  前往领取
                </el-button>
              </el-empty>

              <el-row v-else :gutter="24">
                <el-col
                  v-for="coupon in myCoupons"
                  :key="coupon.id"
                  :xs="24"
                  :sm="12"
                  :md="8"
                  :lg="6"
                >
                  <el-card class="coupon-card my-coupon hover-lift" shadow="hover">
                    <div class="coupon-type">{{ getCouponTypeName(coupon.couponType) }}</div>
                    <div class="coupon-value">
                      <span v-if="coupon.couponType === 2" class="amount">¥{{ coupon.couponValue }}</span>
                      <span v-else class="amount">{{ coupon.couponValue * 10 }}折</span>
                    </div>
                    <div class="coupon-name">{{ coupon.couponName }}</div>
                    <div class="coupon-condition">
                      满 {{ coupon.minAmount || 0 }} 元可用
                    </div>
                    <div class="coupon-time">
                      {{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}
                    </div>

                    <el-tag
                      :type="coupon.status === 0 ? 'success' : (coupon.status === 1 ? 'info' : 'danger')"
                      size="small"
                      effect="light"
                    >
                      {{ getStatusName(coupon.status) }}
                    </el-tag>
                  </el-card>
                </el-col>
              </el-row>
            </template>
          </template>
        </el-skeleton>
      </template>
    </div>
  </div>
</template>

<style scoped>
.coupon-page {
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

/* 状态筛选 */
.status-tabs {
  margin-bottom: 1.5rem;
}

/* 优惠券卡片 */
.coupon-card {
  text-align: center;
  padding: 1.5rem !important;
  border-radius: 16px !important;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #fff 0%, #f8fafc 100%);
  border: 2px solid #f1f5f9 !important;
}

.my-coupon {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #bae6fd !important;
}

.coupon-type {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.coupon-value {
  margin-bottom: 0.75rem;
}

.coupon-value .amount {
  font-size: 2rem;
  font-weight: 700;
  color: #ef4444;
}

.coupon-name {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.coupon-condition {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.coupon-time {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 1rem;
}

.coupon-card .el-button {
  width: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .coupon-value .amount {
    font-size: 1.5rem;
  }
}
</style>