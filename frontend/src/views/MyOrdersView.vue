<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const orders = ref([])
const activeTab = ref('all')

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 状态选项
const statusOptions = [
  { value: 'all', label: '全部订单' },
  { value: 'PENDING', label: '待支付' },
  { value: 'PAID', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' }
]

// 根据状态过滤订单
const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  return orders.value.filter(order => order.status === activeTab.value)
})

// 加载订单列表
const fetchOrders = async () => {
  if (!isLoggedIn.value) return

  try {
    loading.value = true
    const res = await orderApi.getOrderList()
    orders.value = res.data || []
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 取消订单
const cancelOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.cancelOrder(orderId, '用户主动取消')
    ElMessage.success('订单已取消')
    await fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败：' + (error.message || '请稍后重试'))
    }
  }
}

// 支付订单 (Mock)
const payOrder = async (orderId) => {
  try {
    await orderApi.payOrder(orderId, 'MOCK')
    ElMessage.success('支付成功！')
    await fetchOrders()
  } catch (error) {
    ElMessage.error('支付失败：' + (error.message || '请稍后重试'))
  }
}

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    PAID: 'success',
    CANCELLED: 'info',
    EXPIRED: 'danger'
  }
  return typeMap[status] || 'info'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return parseFloat(amount).toFixed(2)
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="orders-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Document /></el-icon>
          我的订单
        </h1>
        <p class="page-subtitle">查看和管理您的购买订单</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 未登录提示 -->
      <el-card v-if="!isLoggedIn" class="login-tip-card" shadow="never">
        <div class="login-tip-content">
          <el-icon :size="64"><UserFilled /></el-icon>
          <h2>请先登录</h2>
          <p>登录后可查看订单信息</p>
          <el-button type="primary" size="large" @click="router.push('/user')">
            前往登录
          </el-button>
        </div>
      </el-card>

      <!-- 已登录状态 -->
      <template v-else>
        <!-- 状态筛选 -->
        <el-tabs v-model="activeTab" class="status-tabs" @tab-change="fetchOrders">
          <el-tab-pane
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :name="option.value"
          />
        </el-tabs>

        <el-skeleton :loading="loading" animated>
          <template #default>
            <!-- 空状态 -->
            <el-empty
              v-if="filteredOrders.length === 0"
              description="暂无订单记录"
            >
              <el-button type="primary" @click="router.push('/member')">
                前往购买会员
              </el-button>
            </el-empty>

            <!-- 订单列表 -->
            <div v-else class="orders-list">
              <el-card
                v-for="order in filteredOrders"
                :key="order.id"
                class="order-card"
                shadow="hover"
              >
                <div class="order-header">
                  <div class="order-no">
                    <span class="label">订单号：</span>
                    <span class="value">{{ order.orderNo }}</span>
                  </div>
                  <el-tag :type="getStatusType(order.status)" effect="light">
                    {{ order.statusName }}
                  </el-tag>
                </div>

                <el-divider />

                <div class="order-body">
                  <el-row :gutter="24">
                    <el-col :xs="24" :md="8">
                      <div class="order-info-item">
                        <span class="label">商品名称</span>
                        <span class="value">{{ order.productName }}</span>
                      </div>
                    </el-col>
                    <el-col :xs="24" :md="4">
                      <div class="order-info-item">
                        <span class="label">订单金额</span>
                        <span class="value price">¥{{ formatAmount(order.amount) }}</span>
                      </div>
                    </el-col>
                    <el-col :xs="24" :md="4">
                      <div class="order-info-item">
                        <span class="label">实付金额</span>
                        <span class="value price">¥{{ formatAmount(order.payAmount) }}</span>
                      </div>
                    </el-col>
                    <el-col :xs="24" :md="6">
                      <div class="order-info-item">
                        <span class="label">创建时间</span>
                        <span class="value">{{ formatDate(order.createTime) }}</span>
                      </div>
                    </el-col>
                  </el-row>
                </div>

                <div class="order-footer">
                  <template v-if="order.status === 'PENDING'">
                    <el-button size="small" @click="payOrder(order.id)">
                      立即支付
                    </el-button>
                    <el-button size="small" type="danger" plain @click="cancelOrder(order.id)">
                      取消订单
                    </el-button>
                  </template>
                  <template v-else-if="order.status === 'PAID'">
                    <el-tag type="success" size="small">
                      <el-icon><SuccessFilled /></el-icon>
                      已完成
                    </el-tag>
                  </template>
                  <template v-else-if="order.status === 'CANCELLED'">
                    <el-tag type="info" size="small">
                      已取消
                    </el-tag>
                  </template>
                </div>
              </el-card>
            </div>
          </template>
        </el-skeleton>
      </template>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
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

/* 订单列表 */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-card {
  border-radius: 16px !important;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no .label {
  color: #64748b;
  font-size: 0.9rem;
}

.order-no .value {
  color: #1e293b;
  font-weight: 500;
}

.order-body {
  padding: 1rem 0;
}

.order-info-item {
  margin-bottom: 0.75rem;
}

.order-info-item .label {
  color: #64748b;
  font-size: 0.85rem;
  display: block;
  margin-bottom: 0.25rem;
}

.order-info-item .value {
  color: #1e293b;
  font-weight: 500;
}

.order-info-item .value.price {
  color: #ef4444;
  font-size: 1.1rem;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

/* 响应式 */
@media (max-width: 768px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .order-footer {
    flex-wrap: wrap;
  }
}
</style>