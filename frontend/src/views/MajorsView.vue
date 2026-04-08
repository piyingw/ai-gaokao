<script setup>
import { ref, computed, onMounted } from 'vue'
import { majorApi } from '@/services/api'

const searchQuery = ref('')
const selectedCategory = ref('')
const selectedDegree = ref('')
const currentPage = ref(1)
const pageSize = ref(12)

const majors = ref([])
const categories = ref([])
const loading = ref(true)
const loadingMajors = ref(true)

const filteredMajors = computed(() => {
  return majors.value.filter(major => {
    const matchesSearch = !searchQuery.value ||
      major.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (major.description && major.description.toLowerCase().includes(searchQuery.value.toLowerCase()))
    const matchesCategory = !selectedCategory.value || major.category === selectedCategory.value
    // 注意：后端API中没有学位层次字段，这里简化处理
    return matchesSearch && matchesCategory
  })
})

const paginatedMajors = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredMajors.value.slice(start, end)
})

const categoryOptions = computed(() => {
  return categories.value.map(cat => ({
    value: cat,
    label: `${cat}类`
  }))
})

const getCategoryColor = (category) => {
  const colors = {
    '工学': '#409eff',
    '理学': '#67c23a',
    '医学': '#f56c6c',
    '经济学': '#e6a23c',
    '法学': '#909399',
    '文学': '#a855f7',
    '管理学': '#06b6d4',
    '艺术学': '#ec4899',
    '农学': '#10b981',
    '哲学': '#8b5cf6',
    '历史学': '#f97316',
    '教育学': '#06b6d4'
  }
  return colors[category] || '#909399'
}

const handleSearch = () => {
  currentPage.value = 1
}

const resetFilters = () => {
  searchQuery.value = ''
  selectedCategory.value = ''
  currentPage.value = 1
}

const loadMajors = async () => {
  try {
    loadingMajors.value = true
    const response = await majorApi.getMajorList({
      page: currentPage.value,
      size: pageSize.value,
      category: selectedCategory.value,
      keyword: searchQuery.value
    })
    
    // 更新数据结构以匹配后端返回
    majors.value = response.data.records || []
  } catch (error) {
    console.error('获取专业列表失败:', error)
    // 设置默认数据
    majors.value = [
      { id: 1, name: '计算机科学与技术', category: '工学', description: '培养计算机软硬件系统设计、开发和应用能力的专业', avgSalary: '15-30K', employmentRate: '98%' },
      { id: 2, name: '软件工程', category: '工学', description: '专注于软件开发过程、方法和工具的专业', avgSalary: '14-28K', employmentRate: '97%' },
      { id: 3, name: '人工智能', category: '工学', description: '研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统', avgSalary: '18-35K', employmentRate: '95%' },
      { id: 4, name: '临床医学', category: '医学', description: '培养具备基础医学和临床医学的基本理论知识和技能的专业人才', avgSalary: '10-25K', employmentRate: '96%' },
      { id: 5, name: '金融学', category: '经济学', description: '研究价值判断和价值规律的学科，培养金融领域专业人才', avgSalary: '12-25K', employmentRate: '92%' },
      { id: 6, name: '法学', category: '法学', description: '学习法律知识，培养法律思维和实践能力的专业', avgSalary: '8-20K', employmentRate: '88%' },
      { id: 7, name: '电子信息工程', category: '工学', description: '应用电子技术和信息系统的设计、开发、应用和集成的专业', avgSalary: '12-25K', employmentRate: '95%' },
      { id: 8, name: '数据科学与大数据技术', category: '工学', description: '以数据为中心，研究数据采集、存储、分析和应用的技术', avgSalary: '15-30K', employmentRate: '96%' }
    ]
  } finally {
    loadingMajors.value = false
  }
}

const loadCategories = async () => {
  try {
    const response = await majorApi.getCategories()
    categories.value = response.data || []
  } catch (error) {
    console.error('获取专业类别失败:', error)
    // 设置默认类别
    categories.value = ['工学', '理学', '医学', '经济学', '法学', '文学', '管理学']
  }
}

onMounted(async () => {
  await loadCategories()
  await loadMajors()
  loading.value = false
})

// 监听筛选条件变化
const handleFilterChange = () => {
  currentPage.value = 1
  loadMajors()
}

// 监听分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadMajors()
}

// 监听每页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadMajors()
}
</script>

<template>
  <div class="majors-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Reading /></el-icon>
          专业查询
        </h1>
        <p class="page-subtitle">了解专业详情，选择适合您的学科方向</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 搜索筛选区域 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <el-input
            v-model="searchQuery"
            placeholder="搜索专业名称或描述..."
            size="large"
            clearable
            class="search-input"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="filter-options">
          <el-select
            v-model="selectedCategory"
            placeholder="专业类别"
            size="large"
            clearable
            class="filter-select"
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-button size="large" @click="resetFilters">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </el-card>

      <!-- 结果统计 -->
      <div class="results-info">
        <el-tag type="info" size="large">
          共找到 <strong>{{ filteredMajors.length }}</strong> 个专业
        </el-tag>
      </div>

      <!-- 加载状态 -->
      <div v-if="loadingMajors" class="loading-container">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p>正在加载专业数据...</p>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="filteredMajors.length === 0"
        description="未找到匹配的专业，请调整筛选条件"
      >
        <el-button type="primary" @click="resetFilters">重置筛选</el-button>
      </el-empty>

      <!-- 专业列表 -->
      <template v-else>
        <el-row :gutter="24" class="major-grid">
          <el-col
            v-for="major in paginatedMajors"
            :key="major.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card class="major-card hover-lift" shadow="hover">
              <div class="card-header">
                <div class="major-icon" :style="{ background: getCategoryColor(major.category) }">
                  <el-icon :size="24"><Reading /></el-icon>
                </div>
              </div>

              <h3 class="major-name">{{ major.name }}</h3>

              <div class="major-tags">
                <el-tag :color="getCategoryColor(major.category)" effect="light" size="small">
                  {{ major.category }}类
                </el-tag>
              </div>

              <p class="major-description">{{ major.description }}</p>

              <el-divider />

              <div class="major-stats">
                <div class="stat-item">
                  <span class="stat-label">平均薪资</span>
                  <span class="stat-value salary">{{ major.avgSalary || '面议' }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">就业率</span>
                  <span class="stat-value">{{ major.employmentRate || '未知' }}</span>
                </div>
              </div>

              <div class="card-footer">
                <el-button type="primary" size="small" round @click="$router.push(`/major/${major.id}`)">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[8, 12, 24, 36]"
            :total="filteredMajors.length"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.majors-page {
  min-height: calc(100vh - 200px);
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
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
  padding: 1.5rem !important;
}

.filter-row {
  margin-bottom: 1rem;
}

.search-input {
  max-width: 400px;
}

.filter-options {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-select {
  width: 160px;
}

/* 结果信息 */
.results-info {
  margin-bottom: 1.5rem;
}

.results-info strong {
  color: #409eff;
  font-size: 1.1rem;
}

/* 加载状态 */
.loading-container {
  text-align: center;
  padding: 4rem 0;
  color: #64748b;
}

.loading-container .el-icon {
  color: #409eff;
  margin-bottom: 1rem;
}

/* 专业卡片 */
.major-grid {
  margin: 0 -12px;
}

.major-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
  text-align: center;
  padding: 1.5rem !important;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.major-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.major-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.75rem;
  text-align: left;
  width: 100%;
}

.major-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-start;
  margin-bottom: 1rem;
}

.major-description {
  text-align: left;
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.5;
  margin: 0 0 1rem;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.major-stats {
  display: flex;
  justify-content: space-around;
  padding: 0.5rem 0;
  margin-top: auto;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.8rem;
  color: #64748b;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-weight: 600;
  color: #1e293b;
}

.stat-value.salary {
  color: #22c55e;
}

.card-footer {
  margin-top: 1rem;
}

.card-footer .el-button {
  width: 100%;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
  padding: 1rem 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-title {
    font-size: 1.5rem;
  }

  .filter-options {
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }

  .search-input {
    max-width: 100%;
  }
}
</style>