<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = ref([
  { value: '500+', label: '合作院校', icon: 'School', color: '#409eff' },
  { value: '1000+', label: '专业数据', icon: 'Reading', color: '#67c23a' },
  { value: '10万+', label: '用户信赖', icon: 'UserFilled', color: '#e6a23c' },
  { value: '98%', label: '满意度', color: '#f56c6c' }
])

const features = ref([
  {
    icon: 'Search',
    title: '院校查询',
    description: '查询全国各大高校的详细信息、历年分数线及专业设置',
    color: '#409eff',
    path: '/universities'
  },
  {
    icon: 'Aim',
    title: '专业推荐',
    description: '根据您的兴趣和成绩，推荐最适合的专业方向',
    color: '#67c23a',
    path: '/majors'
  },
  {
    icon: 'ChatDotRound',
    title: 'AI智能助手',
    description: '基于大语言模型的智能问答系统，解答志愿填报疑问',
    color: '#a855f7',
    path: '/ai-assistant'
  },
  {
    icon: 'Document',
    title: '一键志愿生成',
    description: '输入个人情况，一键生成科学合理的志愿填报方案',
    color: '#06b6d4',
    path: '/ai-assistant'
  }
])

const steps = ref([
  { number: 1, title: '填写个人信息', desc: '输入您的高考分数、兴趣爱好等信息' },
  { number: 2, title: '获取推荐方案', desc: 'AI为您生成个性化志愿填报建议' },
  { number: 3, title: '确认并提交', desc: '完善志愿表并提交至教育考试院' }
])

const handleFeatureClick = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="home-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="hero-bg">
        <div class="hero-pattern"></div>
      </div>
      <div class="container hero-content">
        <div class="hero-text">
          <h1 class="hero-title">
            开启您的
            <span class="gradient-text">大学梦想</span>
            之旅
          </h1>
          <p class="hero-subtitle">
            AI驱动的智能志愿填报系统，助您科学规划未来
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="router.push('/universities')">
              <el-icon><Search /></el-icon>
              开始查询院校
            </el-button>
            <el-button size="large" round @click="router.push('/ai-assistant')">
              <el-icon><ChatDotRound /></el-icon>
              体验AI助手
            </el-button>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="stats-row">
          <el-card
            v-for="(stat, index) in stats"
            :key="index"
            class="stat-card hover-lift"
            shadow="hover"
            :style="{ '--accent-color': stat.color }"
          >
            <div class="stat-icon">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </el-card>
        </div>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="features-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">核心功能</h2>
          <p class="section-subtitle">为您提供全方位的志愿填报支持</p>
        </div>

        <el-row :gutter="24" class="features-grid">
          <el-col
            v-for="(feature, index) in features"
            :key="index"
            :xs="24"
            :sm="12"
            :md="6"
          >
            <el-card
              class="feature-card hover-lift"
              shadow="hover"
              :style="{ '--feature-color': feature.color }"
              @click="handleFeatureClick(feature.path)"
            >
              <div class="feature-icon-wrapper">
                <el-icon :size="36"><component :is="feature.icon" /></el-icon>
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-desc">{{ feature.description }}</p>
              <div class="feature-link">
                <span>了解更多</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </section>

    <!-- 使用流程 -->
    <section class="process-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">使用流程</h2>
          <p class="section-subtitle">简单三步，轻松完成志愿填报</p>
        </div>

        <div class="process-steps">
          <template v-for="(step, index) in steps" :key="index">
            <div class="step-item">
              <div class="step-number">{{ step.number }}</div>
              <h3 class="step-title">{{ step.title }}</h3>
              <p class="step-desc">{{ step.desc }}</p>
            </div>
            <div v-if="index < steps.length - 1" class="step-arrow">
              <el-icon :size="32"><ArrowRight /></el-icon>
            </div>
          </template>
        </div>
      </div>
    </section>

    <!-- CTA 区域 -->
    <section class="cta-section">
      <div class="container">
        <el-card class="cta-card" shadow="never">
          <div class="cta-content">
            <div class="cta-text">
              <h2>准备好开始了吗？</h2>
              <p>立即体验AI智能志愿填报，让您的未来更加清晰</p>
            </div>
            <el-button type="primary" size="large" round @click="router.push('/ai-assistant')">
              立即开始
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  padding-bottom: 2rem;
}

/* 英雄区域 */
.hero-section {
  position: relative;
  padding: 4rem 0 3rem;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}

.hero-pattern {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Ccircle cx='30' cy='30' r='2'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-text {
  text-align: center;
  color: #fff;
  margin-bottom: 3rem;
}

.hero-title {
  font-size: 2.75rem;
  font-weight: 800;
  margin: 0 0 1rem;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.hero-title .gradient-text {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.25rem;
  opacity: 0.95;
  margin: 0 0 2rem;
  font-weight: 400;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.hero-actions .el-button {
  padding: 0.75rem 2rem;
  font-size: 1rem;
}

.hero-actions .el-button--primary {
  background: #fff !important;
  color: #667eea !important;
  border: none !important;
  font-weight: 600;
}

.hero-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.hero-actions .el-button:not(.el-button--primary) {
  background: transparent !important;
  color: #fff !important;
  border: 2px solid rgba(255, 255, 255, 0.5) !important;
}

.hero-actions .el-button:not(.el-button--primary):hover {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: #fff !important;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem !important;
  border-radius: 16px !important;
  cursor: pointer;
  border-left: 4px solid var(--accent-color) !important;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent-color), color-mix(in srgb, var(--accent-color) 70%, white));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 0.9rem;
  color: #64748b;
  margin-top: 0.25rem;
}

/* 功能区域 */
.features-section {
  padding: 5rem 0;
  background: #fff;
}

.section-header {
  text-align: center;
  margin-bottom: 3rem;
}

.section-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.section-subtitle {
  font-size: 1.1rem;
  color: #64748b;
  margin: 0;
}

.features-grid {
  margin: 0 -12px;
}

.feature-card {
  text-align: center;
  padding: 2rem 1.5rem !important;
  border-radius: 20px !important;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 1.5rem;
}

.feature-card:hover {
  transform: translateY(-8px);
}

.feature-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--feature-color), color-mix(in srgb, var(--feature-color) 70%, white));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  color: #fff;
  transition: transform 0.3s ease;
}

.feature-card:hover .feature-icon-wrapper {
  transform: scale(1.1);
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.feature-desc {
  font-size: 0.95rem;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 1rem;
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--feature-color);
  font-weight: 500;
  font-size: 0.9rem;
}

/* 流程区域 */
.process-section {
  padding: 5rem 0;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.process-steps {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 2rem;
  flex-wrap: wrap;
}

.step-item {
  text-align: center;
  flex: 1;
  min-width: 200px;
  max-width: 280px;
}

.step-number {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 1.75rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.step-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.step-desc {
  font-size: 0.95rem;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.step-arrow {
  display: flex;
  align-items: center;
  padding-top: 2rem;
  color: #cbd5e1;
}

/* CTA 区域 */
.cta-section {
  padding: 3rem 0 4rem;
}

.cta-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border-radius: 24px !important;
  padding: 3rem !important;
}

.cta-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  flex-wrap: wrap;
}

.cta-text {
  color: #fff;
}

.cta-text h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
}

.cta-text p {
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
}

.cta-content .el-button {
  background: #fff !important;
  color: #667eea !important;
  border: none !important;
  font-weight: 600;
  padding: 0.75rem 2rem;
}

.cta-content .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: 1rem !important;
  }

  .stat-value {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.75rem;
  }

  .process-steps {
    flex-direction: column;
    align-items: center;
  }

  .step-arrow {
    transform: rotate(90deg);
    padding: 0;
  }

  .cta-content {
    flex-direction: column;
    text-align: center;
  }

  .cta-card {
    padding: 2rem !important;
  }
}
</style>