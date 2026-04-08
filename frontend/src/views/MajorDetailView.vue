<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { majorApi } from '@/services/api'

const route = useRoute()
const router = useRouter()
const major = ref(null)
const loading = ref(true)

const loadMajorDetails = async () => {
  try {
    const id = route.params.id
    const response = await majorApi.getMajorDetail(id)
    major.value = response.data
  } catch (error) {
    console.error('获取专业详情失败:', error)
    // 设置默认数据
    major.value = {
      id: route.params.id,
      name: '计算机科学与技术',
      category: '工学',
      subcategory: '计算机类',
      degree: '本科',
      duration: 4,
      description: '计算机科学与技术专业培养具有良好的科学素养，系统地、较好地掌握计算机科学与技术包括计算机硬件、软件与应用的基本理论、基本知识和基本技能与方法，能在科研部门、教育单位、企业、事业、技术和行政管理部门等单位从事计算机教学、科学研究和应用的高级专门科学技术人才。',
      curriculum: [
        '高等数学',
        '线性代数',
        '概率论与数理统计',
        '离散数学',
        '计算机组成原理',
        '操作系统',
        '数据结构',
        '算法设计与分析',
        '计算机网络',
        '数据库系统原理',
        '软件工程',
        '编译原理'
      ],
      employmentDirection: [
        '软件开发工程师',
        '系统分析师',
        '数据库管理员',
        '网络安全专家',
        '产品经理',
        '技术顾问'
      ],
      avgSalary: '15-30K',
      employmentRate: '98%',
      institutions: [
        { id: 1, name: '清华大学', location: '北京' },
        { id: 2, name: '北京大学', location: '北京' },
        { id: 3, name: '上海交通大学', location: '上海' },
        { id: 4, name: '浙江大学', location: '杭州' }
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMajorDetails()
})
</script>

<template>
  <div class="major-detail-page">
    <div class="container">
      <!-- 顶部导航 -->
      <div class="page-nav">
        <el-button @click="router.go(-1)" link>
          <el-icon><ArrowLeft /></el-icon>
          返回专业列表
        </el-button>
      </div>

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="8" animated />

      <!-- 专业详情 -->
      <div v-else-if="major" class="major-detail">
        <!-- 专业头部信息 -->
        <el-card class="major-header-card" shadow="never">
          <div class="major-header">
            <div class="major-icon">
              <el-icon :size="64"><Reading /></el-icon>
            </div>
            <div class="major-basic-info">
              <h1 class="major-name">{{ major.name }}</h1>
              <div class="major-tags">
                <el-tag size="large" effect="dark" class="category-tag">
                  {{ major.category }}类
                </el-tag>
                <el-tag type="info" size="large">{{ major.subcategory }}</el-tag>
                <el-tag type="success" size="large">{{ major.degree }}</el-tag>
                <el-tag type="warning" size="large">{{ major.duration }}年制</el-tag>
              </div>
              <div class="major-stats">
                <div class="stat-item">
                  <el-icon><Money /></el-icon>
                  <span>平均薪资: <strong>{{ major.avgSalary }}</strong></span>
                </div>
                <div class="stat-item">
                  <el-icon><Check /></el-icon>
                  <span>就业率: <strong>{{ major.employmentRate }}</strong></span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 详情内容 -->
        <el-row :gutter="24" class="detail-content">
          <!-- 专业介绍 -->
          <el-col :xs="24" :md="16">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><InfoFilled /></el-icon>
                  <span>专业介绍</span>
                </div>
              </template>
              <p class="major-description">{{ major.description }}</p>
            </el-card>

            <!-- 课程体系 -->
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Collection /></el-icon>
                  <span>核心课程</span>
                </div>
              </template>
              <el-row :gutter="12">
                <el-col
                  v-for="(course, index) in major.curriculum"
                  :key="index"
                  :xs="12" :sm="8" :md="6"
                >
                  <el-tag type="info" class="course-tag">{{ course }}</el-tag>
                </el-col>
              </el-row>
            </el-card>

            <!-- 就业方向 -->
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Briefcase /></el-icon>
                  <span>就业方向</span>
                </div>
              </template>
              <el-row :gutter="12">
                <el-col
                  v-for="(direction, index) in major.employmentDirection"
                  :key="index"
                  :xs="12" :sm="8" :md="6"
                >
                  <el-tag type="success" class="direction-tag">{{ direction }}</el-tag>
                </el-col>
              </el-row>
            </el-card>
          </el-col>

          <!-- 侧边栏 -->
          <el-col :xs="24" :md="8">
            <!-- 开设院校 -->
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><School /></el-icon>
                  <span>开设院校</span>
                </div>
              </template>
              <div class="institution-list">
                <div
                  v-for="(institution, index) in major.institutions"
                  :key="index"
                  class="institution-item"
                  @click="$router.push(`/university/${institution.id}`)"
                >
                  <div class="institution-name">{{ institution.name }}</div>
                  <div class="institution-location">{{ institution.location }}</div>
                </div>
              </div>
            </el-card>

            <!-- 相关推荐 -->
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Star /></el-icon>
                  <span>相关专业</span>
                </div>
              </template>
              <div class="related-majors">
                <el-tag class="related-tag">软件工程</el-tag>
                <el-tag class="related-tag">网络工程</el-tag>
                <el-tag class="related-tag">信息安全</el-tag>
                <el-tag class="related-tag">物联网工程</el-tag>
              </div>
            </el-card>

            <!-- 咨询按钮 -->
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><ChatLineRound /></el-icon>
                  <span>专业咨询</span>
                </div>
              </template>
              <el-button type="primary" size="large" @click="$router.push('/ai-assistant')" class="consult-btn">
                <el-icon><ChatDotRound /></el-icon>
                AI专业咨询
              </el-button>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 未找到专业 -->
      <el-result v-else icon="error" title="专业不存在" sub-title="抱歉，未能找到该专业的信息">
        <template #extra>
          <el-button type="primary" @click="router.push('/majors')">返回专业列表</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<style scoped>
.major-detail-page {
  min-height: calc(100vh - 200px);
  padding: 2rem 0;
}

.page-nav {
  margin-bottom: 1.5rem;
}

.major-header-card {
  border-radius: 16px !important;
  margin-bottom: 1.5rem;
}

.major-header {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.major-icon {
  width: 120px;
  height: 120px;
  border-radius: 20px;
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.major-basic-info {
  flex: 1;
}

.major-name {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 1rem;
}

.major-tags {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.category-tag {
  background: linear-gradient(135deg, #67c23a, #409eff) !important;
  border: none !important;
}

.major-stats {
  display: flex;
  gap: 2rem;
  color: #64748b;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stat-item strong {
  color: #409eff;
}

.detail-content {
  margin-top: 1rem;
}

.detail-card {
  border-radius: 16px !important;
  margin-bottom: 1.5rem;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1e293b;
}

.major-description {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
}

.course-tag, .direction-tag {
  margin: 0.5rem 0.5rem 0 0;
}

.direction-tag {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  border: none !important;
  color: white !important;
}

.institution-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.institution-item {
  padding: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.institution-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-2px);
}

.institution-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.25rem;
}

.institution-location {
  color: #64748b;
  font-size: 0.9rem;
}

.related-majors {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.related-tag {
  margin-bottom: 0.5rem;
  cursor: pointer;
}

.consult-btn {
  width: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .major-header {
    flex-direction: column;
    text-align: center;
  }
  
  .major-stats {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .major-tags {
    justify-content: center;
  }
}
</style>