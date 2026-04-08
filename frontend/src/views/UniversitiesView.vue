<script setup>
import { ref, computed, onMounted } from 'vue'
import { universityApi } from '@/services/api'

const universities = ref([])
const loading = ref(true)
const searchQuery = ref('')
const selectedRegion = ref('')
const selectedLevel = ref('')
const selectedType = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(12)

// 过滤后的大学列表
const filteredUniversities = computed(() => {
  return universities.value.filter(university => {
    const matchesSearch = !searchQuery.value ||
      university.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (university.alias && university.alias.toLowerCase().includes(searchQuery.value.toLowerCase()))
    const matchesRegion = !selectedRegion.value || university.city === selectedRegion.value
    const matchesLevel = !selectedLevel.value || (university.level && university.level.includes(selectedLevel.value))
    const matchesType = !selectedType.value || university.type === selectedType.value
    return matchesSearch && matchesRegion && matchesLevel && matchesType
  })
})

// 分页后的列表
const paginatedUniversities = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredUniversities.value.slice(start, end)
})

// 地区选项
const regionOptions = ref([
  { value: '北京', label: '北京' },
  { value: '上海', label: '上海' },
  { value: '广州', label: '广州' },
  { value: '杭州', label: '杭州' },
  { value: '南京', label: '南京' },
  { value: '武汉', label: '武汉' },
  { value: '西安', label: '西安' },
  { value: '成都', label: '成都' },
  { value: '深圳', label: '深圳' },
  { value: '天津', label: '天津' }
])

// 层次选项
const levelOptions = [
  { value: '985', label: '985工程' },
  { value: '211', label: '211工程' },
  { value: '双一流', label: '双一流' },
  { value: '普通本科', label: '普通本科' }
]

// 类型选项
const typeOptions = ref([
  { value: '综合', label: '综合类' },
  { value: '工科', label: '工科类' },
  { value: '师范', label: '师范类' },
  { value: '医药', label: '医药类' },
  { value: '财经', label: '财经类' },
  { value: '政法', label: '政法类' },
  { value: '语言', label: '语言类' },
  { value: '艺术', label: '艺术类' },
  { value: '体育', label: '体育类' }
])

// 获取层次标签类型
const getLevelType = (level) => {
  if (!level) return 'info'
  
  if (level.includes('985')) return 'danger'
  if (level.includes('211')) return 'warning'
  if (level.includes('双一流')) return 'success'
  return 'info'
}

const loadUniversities = async () => {
  try {
    loading.value = true
    
    // 调用API获取院校列表
    const response = await universityApi.getUniversityList({
      page: currentPage.value,
      size: pageSize.value,
      name: searchQuery.value,
      city: selectedRegion.value,
      level: selectedLevel.value,
      type: selectedType.value
    })
    
    // 更新数据结构以匹配后端返回
    universities.value = response.data.records || []
  } catch (error) {
    console.error('获取院校列表失败:', error)
    // 设置默认数据
    universities.value = [
      { id: 1, name: '清华大学', city: '北京', level: '985工程,双一流', type: '综合', avgScore: 680, logo: '🎓', alias: 'THU' },
      { id: 2, name: '北京大学', city: '北京', level: '985工程,双一流', type: '综合', avgScore: 685, logo: '🎓', alias: 'PKU' },
      { id: 3, name: '复旦大学', city: '上海', level: '985工程,双一流', type: '综合', avgScore: 675, logo: '🎓', alias: 'FUDAN' },
      { id: 4, name: '上海交通大学', city: '上海', level: '985工程,双一流', type: '综合', avgScore: 670, logo: '🎓', alias: 'SJTU' },
      { id: 5, name: '浙江大学', city: '杭州', level: '985工程,双一流', type: '综合', avgScore: 665, logo: '🎓', alias: 'ZJU' },
      { id: 6, name: '中山大学', city: '广州', level: '985工程,双一流', type: '综合', avgScore: 650, logo: '🎓', alias: 'SYSU' },
      { id: 7, name: '华中科技大学', city: '武汉', level: '985工程,双一流', type: '工科', avgScore: 645, logo: '🎓', alias: 'HUST' },
      { id: 8, name: '西安交通大学', city: '西安', level: '985工程,双一流', type: '综合', avgScore: 640, logo: '🎓', alias: 'XJTU' },
      { id: 9, name: '南京大学', city: '南京', level: '985工程,双一流', type: '综合', avgScore: 660, logo: '🎓', alias: 'NJU' },
      { id: 10, name: '四川大学', city: '成都', level: '985工程,双一流', type: '综合', avgScore: 635, logo: '🎓', alias: 'SCU' },
      { id: 11, name: '同济大学', city: '上海', level: '985工程,双一流', type: '工科', avgScore: 655, logo: '🎓', alias: 'TONGJI' },
      { id: 12, name: '武汉大学', city: '武汉', level: '985工程,双一流', type: '综合', avgScore: 650, logo: '🎓', alias: 'WHU' }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadUniversities()
})

const handlePageChange = (page) => {
  currentPage.value = page
  loadUniversities()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadUniversities()
}

const handleSearch = () => {
  currentPage.value = 1
  loadUniversities()
}

const resetFilters = () => {
  searchQuery.value = ''
  selectedRegion.value = ''
  selectedLevel.value = ''
  selectedType.value = ''
  currentPage.value = 1
  loadUniversities()
}
</script>

<template>
  <div class="universities-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><School /></el-icon>
          院校查询
        </h1>
        <p class="page-subtitle">探索全国知名高校，找到最适合您的理想学府</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 搜索筛选区域 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <el-input
            v-model="searchQuery"
            placeholder="搜索院校名称或别称..."
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
            v-model="selectedRegion"
            placeholder="选择地区"
            size="large"
            clearable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option
              v-for="item in regionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-select
            v-model="selectedLevel"
            placeholder="院校层次"
            size="large"
            clearable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option
              v-for="item in levelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-select
            v-model="selectedType"
            placeholder="院校类型"
            size="large"
            clearable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option
              v-for="item in typeOptions"
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
          共找到 <strong>{{ filteredUniversities.length }}</strong> 所符合条件的院校
        </el-tag>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p>正在加载院校数据...</p>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="filteredUniversities.length === 0"
        description="未找到匹配的院校，请调整筛选条件"
      >
        <el-button type="primary" @click="resetFilters">重置筛选</el-button>
      </el-empty>

      <!-- 院校列表 -->
      <template v-else>
        <el-row :gutter="24" class="university-grid">
          <el-col
            v-for="university in paginatedUniversities"
            :key="university.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card class="university-card hover-lift" shadow="hover">
              <div class="card-header">
                <div class="university-logo">{{ university.logo || '🏫' }}</div>
                <div class="university-info">
                  <h3 class="university-name">{{ university.name }}</h3>
                  <el-tag :type="getLevelType(university.level)" size="small" effect="dark">
                    {{ university.level?.split(',')[0] || '普通本科' }}
                  </el-tag>
                </div>
              </div>

              <el-divider />

              <div class="card-body">
                <div class="info-item">
                  <span class="info-label">
                    <el-icon><Location /></el-icon>
                    所在地
                  </span>
                  <span class="info-value">{{ university.city }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <el-icon><Collection /></el-icon>
                    类型
                  </span>
                  <span class="info-value">{{ university.type }}类</span>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <el-icon><TrendCharts /></el-icon>
                    平均分数线
                  </span>
                  <span class="info-value score">{{ university.avgScore || '暂无' }}分</span>
                </div>
              </div>

              <div class="card-footer">
                <el-button type="primary" size="small" round @click="$router.push(`/university/${university.id}`)">
                  查看详情
                </el-button>
                <el-button size="small" round>
                  <el-icon><Plus /></el-icon>
                  对比
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
            :total="filteredUniversities.length"
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
.universities-page {
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

/* 院校卡片 */
.university-grid {
  margin: 0 -12px;
}

.university-card {
  margin-bottom: 1.5rem;
  border-radius: 16px !important;
  overflow: hidden;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.university-card:hover {
  transform: translateY(-6px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1rem;
}

.university-logo {
  font-size: 2.5rem;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4f8, #e2e8f0);
  border-radius: 12px;
}

.university-info {
  flex: 1;
  min-width: 0;
}

.university-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-body {
  padding: 0.5rem 0;
  flex: 1;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #64748b;
  font-size: 0.9rem;
}

.info-value {
  font-weight: 500;
  color: #1e293b;
  text-align: right;
  flex: 1;
  margin-left: 0.5rem;
}

.info-value.score {
  color: #ef4444;
  font-weight: 700;
}

.card-footer {
  display: flex;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
  margin-top: auto;
}

.card-footer .el-button {
  flex: 1;
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