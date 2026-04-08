<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { universityApi } from '@/services/api'

const route = useRoute()
const router = useRouter()
const university = ref(null)
const loading = ref(true)
const activeTab = ref('overview')

const loadUniversityDetails = async () => {
  try {
    const id = route.params.id
    const response = await universityApi.getUniversityDetail(id)
    university.value = response.data
  } catch (error) {
    console.error('获取院校详情失败:', error)
    // 设置默认数据
    university.value = {
      id: route.params.id,
      name: '示例大学',
      city: '北京市',
      level: '985工程,双一流',
      type: '综合类',
      foundingYear: 1898,
      description: '这是一所综合性研究型大学，拥有悠久的历史和优良的传统。学校以严谨的学风和卓越的教学质量著称，培养了大批杰出人才。',
      campusArea: '455公顷',
      studentCount: '50000人',
      facultyCount: '3000人',
      contact: {
        address: '北京市海淀区颐和园路5号',
        phone: '010-12345678',
        website: 'https://www.example.edu.cn'
      },
      scores: [
        { year: 2023, scienceAvg: 650, artsAvg: 620 },
        { year: 2022, scienceAvg: 648, artsAvg: 618 },
        { year: 2021, scienceAvg: 645, artsAvg: 615 }
      ],
      majors: [
        { id: 1, name: '计算机科学与技术', category: '工学', degree: '本科' },
        { id: 2, name: '临床医学', category: '医学', degree: '本科' },
        { id: 3, name: '金融学', category: '经济学', degree: '本科' },
        { id: 4, name: '法学', category: '法学', degree: '本科' }
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUniversityDetails()
})
</script>

<template>
  <div class="university-detail-page">
    <div class="container">
      <!-- 顶部导航 -->
      <div class="page-nav">
        <el-button @click="router.go(-1)" link>
          <el-icon><ArrowLeft /></el-icon>
          返回院校列表
        </el-button>
      </div>

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="8" animated />

      <!-- 院校详情 -->
      <div v-else-if="university" class="university-detail">
        <!-- 院校头部信息 -->
        <el-card class="university-header-card" shadow="never">
          <div class="university-header">
            <div class="university-logo">
              <el-icon :size="64"><School /></el-icon>
            </div>
            <div class="university-basic-info">
              <h1 class="university-name">{{ university.name }}</h1>
              <div class="university-tags">
                <el-tag v-for="lvl in university.level?.split(',')" :key="lvl" size="large" effect="dark" class="level-tag">
                  {{ lvl }}
                </el-tag>
                <el-tag type="info" size="large">{{ university.type }}类</el-tag>
                <el-tag type="success" size="large">{{ university.city }}</el-tag>
              </div>
              <div class="university-meta">
                <div class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  <span>创办于 {{ university.foundingYear }} 年</span>
                </div>
                <div class="meta-item">
                  <el-icon><House /></el-icon>
                  <span>{{ university.campusArea }} 校园面积</span>
                </div>
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>{{ university.studentCount }} 在校生</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 详情标签页 -->
        <el-tabs v-model="activeTab" class="detail-tabs" type="card">
          <!-- 概况 -->
          <el-tab-pane label="院校概况" name="overview">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><InfoFilled /></el-icon>
                  <span>院校简介</span>
                </div>
              </template>
              <p class="university-description">{{ university.description }}</p>
              
              <el-row :gutter="24" class="contact-info">
                <el-col :md="8">
                  <h4><el-icon><Location /></el-icon> 地址</h4>
                  <p>{{ university.contact?.address || '暂无信息' }}</p>
                </el-col>
                <el-col :md="8">
                  <h4><el-icon><Phone /></el-icon> 电话</h4>
                  <p>{{ university.contact?.phone || '暂无信息' }}</p>
                </el-col>
                <el-col :md="8">
                  <h4><el-icon><Link /></el-icon> 官网</h4>
                  <p>
                    <el-link :href="university.contact?.website" target="_blank" type="primary">
                      {{ university.contact?.website || '暂无信息' }}
                    </el-link>
                  </p>
                </el-col>
              </el-row>
            </el-card>
          </el-tab-pane>

          <!-- 录取分数 -->
          <el-tab-pane label="历年分数" name="scores">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><TrendCharts /></el-icon>
                  <span>历年录取分数线</span>
                </div>
              </template>
              
              <el-table :data="university.scores" style="width: 100%">
                <el-table-column prop="year" label="年份" width="100" />
                <el-table-column prop="scienceAvg" label="理科平均分" width="150" />
                <el-table-column prop="artsAvg" label="文科平均分" width="150" />
                <el-table-column label="操作">
                  <template #default>
                    <el-button size="small" @click="$router.push('/ai-assistant')">咨询报考建议</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>

          <!-- 开设专业 -->
          <el-tab-pane label="开设专业" name="majors">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Collection /></el-icon>
                  <span>开设专业</span>
                </div>
              </template>
              
              <el-table :data="university.majors" style="width: 100%">
                <el-table-column prop="name" label="专业名称" />
                <el-table-column prop="category" label="学科门类" width="120" />
                <el-table-column prop="degree" label="学位" width="100" />
                <el-table-column label="操作" width="150">
                  <template #default>
                    <el-button size="small">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>

          <!-- 对比功能 -->
          <el-tab-pane label="院校对比" name="compare">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-icon><ScaleToOriginal /></el-icon>
                  <span>与相似院校对比</span>
                </div>
              </template>
              
              <div class="compare-section">
                <p>选择其他院校进行对比：</p>
                <el-button type="primary" @click="$router.push('/universities')">选择院校</el-button>
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 未找到院校 -->
      <el-result v-else icon="error" title="院校不存在" sub-title="抱歉，未能找到该院校的信息">
        <template #extra>
          <el-button type="primary" @click="router.push('/universities')">返回院校列表</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<style scoped>
.university-detail-page {
  min-height: calc(100vh - 200px);
  padding: 2rem 0;
}

.page-nav {
  margin-bottom: 1.5rem;
}

.university-header-card {
  border-radius: 16px !important;
  margin-bottom: 1.5rem;
}

.university-header {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.university-logo {
  width: 120px;
  height: 120px;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.university-basic-info {
  flex: 1;
}

.university-name {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 1rem;
}

.university-tags {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.level-tag {
  background: linear-gradient(135deg, #f56565, #e53e3e) !important;
  border: none !important;
}

.university-meta {
  display: flex;
  gap: 2rem;
  color: #64748b;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-item .el-icon {
  color: #409eff;
}

.detail-tabs {
  margin-top: 1rem;
}

.detail-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.detail-card {
  border-radius: 16px !important;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1e293b;
}

.university-description {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
  margin-bottom: 2rem;
}

.contact-info h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.contact-info p {
  color: #64748b;
  margin: 0;
}

.compare-section {
  text-align: center;
  padding: 3rem 0;
}

.compare-section p {
  margin-bottom: 1.5rem;
  color: #64748b;
}

/* 响应式 */
@media (max-width: 768px) {
  .university-header {
    flex-direction: column;
    text-align: center;
  }
  
  .university-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .university-tags {
    justify-content: center;
  }
}
</style>