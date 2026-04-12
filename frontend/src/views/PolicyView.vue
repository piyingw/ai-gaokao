<script setup>
import { ref, onMounted } from 'vue'
import { policyApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const policies = ref([])
const hotPolicies = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选条件
const searchKeyword = ref('')
const selectedType = ref('')
const selectedProvince = ref('')
const policyTypes = ref([])

// 详情对话框
const showDetail = ref(false)
const currentPolicy = ref(null)

// 类型选项
const typeOptions = ref([
  { value: '招生政策', label: '招生政策' },
  { value: '录取规则', label: '录取规则' },
  { value: '志愿填报', label: '志愿填报' },
  { value: '加分政策', label: '加分政策' },
  { value: '艺术体育', label: '艺术体育' }
])

// 份选项
const provinceOptions = ref([
  { value: '北京', label: '北京' },
  { value: '上海', label: '上海' },
  { value: '广东', label: '广东' },
  { value: '江苏', label: '江苏' },
  { value: '浙江', label: '浙江' },
  { value: '山东', label: '山东' },
  { value: '河南', label: '河南' },
  { value: '四川', label: '四川' },
  { value: '湖北', label: '湖北' },
  { value: '湖南', label: '湖南' }
])

// 加载政策列表
const fetchPolicies = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      type: selectedType.value,
      province: selectedProvince.value
    }
    const res = await policyApi.getPolicyList(params)
    policies.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取政策列表失败:', error)
    ElMessage.error('获取政策列表失败')
  } finally {
    loading.value = false
  }
}

// 加载热门政策
const fetchHotPolicies = async () => {
  try {
    const res = await policyApi.getHotPolicies(5)
    hotPolicies.value = res.data || []
  } catch (error) {
    console.error('获取热门政策失败:', error)
  }
}

// 加载政策类型
const fetchPolicyTypes = async () => {
  try {
    const res = await policyApi.getPolicyTypes()
    policyTypes.value = res.data || []
  } catch (error) {
    console.error('获取政策类型失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPolicies()
}

// 重置筛选
const resetFilters = () => {
  searchKeyword.value = ''
  selectedType.value = ''
  selectedProvince.value = ''
  currentPage.value = 1
  fetchPolicies()
}

// 分页
const handlePageChange = (page) => {
  currentPage.value = page
  fetchPolicies()
}

// 查看详情
const viewDetail = async (id) => {
  try {
    const res = await policyApi.getPolicyDetail(id)
    currentPolicy.value = res.data
    showDetail.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 按类型筛选
const filterByType = async (type) => {
  selectedType.value = type
  currentPage.value = 1
  await fetchPolicies()
}

// 按省份筛选
const filterByProvince = async (province) => {
  selectedProvince.value = province
  currentPage.value = 1
  await fetchPolicies()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchPolicies()
  fetchHotPolicies()
  fetchPolicyTypes()
})
</script>

<template>
  <div class="policy-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Reading /></el-icon>
          政策文档
        </h1>
        <p class="page-subtitle">了解高考政策，科学填报志愿</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 搜索筛选区域 -->
      <el-card class="filter-card" shadow="never">
        <el-row :gutter="16">
          <el-col :xs="24" :md="8">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索政策文档..."
              size="large"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="12" :md="4">
            <el-select
              v-model="selectedType"
              placeholder="政策类型"
              size="large"
              clearable
              @change="handleSearch"
            >
              <el-option
                v-for="item in typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-col>
          <el-col :xs="12" :md="4">
            <el-select
              v-model="selectedProvince"
              placeholder="省份"
              size="large"
              clearable
              @change="handleSearch"
            >
              <el-option
                v-for="item in provinceOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-col>
          <el-col :xs="24" :md="4">
            <el-button size="large" @click="resetFilters">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-col>
        </el-row>
      </el-card>

      <el-row :gutter="24">
        <!-- 左侧政策列表 -->
        <el-col :xs="24" :md="16">
          <el-skeleton :loading="loading" animated>
            <template #default>
              <!-- 空状态 -->
              <el-empty v-if="policies.length === 0" description="暂无政策文档" />

              <!-- 政策列表 -->
              <div v-else class="policy-list">
                <el-card
                  v-for="policy in policies"
                  :key="policy.id"
                  class="policy-card hover-lift"
                  shadow="hover"
                  @click="viewDetail(policy.id)"
                >
                  <div class="policy-header">
                    <el-tag type="primary" size="small">{{ policy.type }}</el-tag>
                    <el-tag type="info" size="small">{{ policy.province }}</el-tag>
                    <span class="policy-time">{{ formatDate(policy.publishTime) }}</span>
                  </div>
                  <h3 class="policy-title">{{ policy.title }}</h3>
                  <p class="policy-summary">{{ policy.summary || policy.content?.slice(0, 100) }}</p>
                  <div class="policy-footer">
                    <el-icon><View /></el-icon>
                    <span>{{ policy.viewCount || 0 }} 次阅读</span>
                  </div>
                </el-card>

                <!-- 分页 -->
                <div v-if="total > pageSize" class="pagination-container">
                  <el-pagination
                    v-model:current-page="currentPage"
                    :page-size="pageSize"
                    :total="total"
                    layout="prev, pager, next"
                    background
                    @current-change="handlePageChange"
                  />
                </div>
              </div>
            </template>
          </el-skeleton>
        </el-col>

        <!-- 右侧热门政策 -->
        <el-col :xs="24" :md="8">
          <el-card class="sidebar-card" shadow="never">
            <template #header>
              <div class="sidebar-header">
                <el-icon><Fire /></el-icon>
                <span>热门政策</span>
              </div>
            </template>

            <div class="hot-list">
              <div
                v-for="(policy, index) in hotPolicies"
                :key="policy.id"
                class="hot-item"
                @click="viewDetail(policy.id)"
              >
                <span class="hot-index">{{ index + 1 }}</span>
                <span class="hot-title">{{ policy.title }}</span>
              </div>
            </div>
          </el-card>

          <el-card class="sidebar-card" shadow="never">
            <template #header>
              <div class="sidebar-header">
                <el-icon><Folder /></el-icon>
                <span>政策分类</span>
              </div>
            </template>

            <div class="category-list">
              <el-button
                v-for="type in typeOptions"
                :key="type.value"
                size="small"
                :type="selectedType === type.value ? 'primary' : 'default'"
                @click="filterByType(type.value)"
              >
                {{ type.label }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 详情对话框 -->
      <el-dialog
        v-model="showDetail"
        title="政策详情"
        width="800px"
      >
        <template v-if="currentPolicy">
          <div class="detail-header">
            <h2 class="detail-title">{{ currentPolicy.title }}</h2>
            <div class="detail-meta">
              <el-tag type="primary">{{ currentPolicy.type }}</el-tag>
              <el-tag type="info">{{ currentPolicy.province }}</el-tag>
              <span>发布时间：{{ formatDate(currentPolicy.publishTime) }}</span>
            </div>
          </div>

          <el-divider />

          <div class="detail-content">
            {{ currentPolicy.content }}
          </div>

          <el-divider />

          <div class="detail-footer">
            <el-button @click="showDetail = false">关闭</el-button>
            <el-button type="primary" @click="$router.push('/ai-assistant')">
              咨询AI助手
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.policy-page {
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

/* 筛选区域 */
.filter-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
}

/* 政策列表 */
.policy-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.policy-card {
  border-radius: 16px !important;
  cursor: pointer;
}

.policy-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.policy-time {
  color: #94a3b8;
  font-size: 0.85rem;
}

.policy-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.policy-summary {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0;
}

.policy-footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #94a3b8;
  font-size: 0.85rem;
  margin-top: 0.75rem;
}

/* 侧边栏 */
.sidebar-card {
  margin-bottom: 1rem;
  border-radius: 16px !important;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1e293b;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.hot-item:hover {
  background: #f8fafc;
}

.hot-index {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hot-title {
  color: #475569;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

/* 详情对话框 */
.detail-header {
  text-align: center;
}

.detail-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 1rem;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: #64748b;
}

.detail-content {
  padding: 1rem;
  line-height: 1.8;
  color: #334155;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-meta {
    flex-wrap: wrap;
  }

  .policy-header {
    flex-wrap: wrap;
  }
}
</style>