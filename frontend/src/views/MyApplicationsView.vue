<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { applicationApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const applications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框
const showDialog = ref(false)
const dialogType = ref('create') // create | edit
const applicationForm = ref({
  id: null,
  name: '',
  score: null,
  province: '',
  subjectType: '',
  remark: '',
  applications: []
})

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 加载志愿列表
const fetchApplications = async () => {
  if (!isLoggedIn.value) return

  try {
    loading.value = true
    const res = await applicationApi.getApplicationList(currentPage.value, pageSize.value)
    applications.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取志愿列表失败:', error)
    ElMessage.error('获取志愿列表失败')
  } finally {
    loading.value = false
  }
}

// 创建志愿
const createApplication = async () => {
  try {
    const res = await applicationApi.createApplication(applicationForm.value)
    ElMessage.success('创建成功')
    showDialog.value = false
    resetForm()
    await fetchApplications()
  } catch (error) {
    ElMessage.error('创建失败：' + (error.message || '请稍后重试'))
  }
}

// 更新志愿
const updateApplication = async () => {
  try {
    await applicationApi.updateApplication(applicationForm.value)
    ElMessage.success('更新成功')
    showDialog.value = false
    resetForm()
    await fetchApplications()
  } catch (error) {
    ElMessage.error('更新失败：' + (error.message || '请稍后重试'))
  }
}

// 删除志愿
const deleteApplication = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该志愿方案吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await applicationApi.deleteApplication(id)
    ElMessage.success('删除成功')
    await fetchApplications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + (error.message || '请稍后重试'))
    }
  }
}

// 提交志愿
const submitApplication = async (id) => {
  try {
    await ElMessageBox.confirm('确定要提交该志愿方案吗？提交后将无法修改。', '提交确认', {
      confirmButtonText: '确定提交',
      cancelButtonText: '取消',
      type: 'info'
    })

    await applicationApi.submitApplication(id)
    ElMessage.success('提交成功')
    await fetchApplications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交失败：' + (error.message || '请稍后重试'))
    }
  }
}

// 复制志愿
const copyApplication = async (id) => {
  try {
    const res = await applicationApi.copyApplication(id)
    ElMessage.success('复制成功')
    await fetchApplications()
  } catch (error) {
    ElMessage.error('复制失败：' + (error.message || '请稍后重试'))
  }
}

// 分析志愿
const analyzeApplication = async (id) => {
  try {
    const res = await applicationApi.analyzeApplication(id)
    ElMessage.success('分析完成，请查看详情')
    // TODO: 显示分析结果
  } catch (error) {
    ElMessage.error('分析失败：' + (error.message || '请稍后重试'))
  }
}

// 打开创建对话框
const openCreateDialog = () => {
  dialogType.value = 'create'
  resetForm()
  showDialog.value = true
}

// 打开编辑对话框
const openEditDialog = (application) => {
  dialogType.value = 'edit'
  applicationForm.value = {
    id: application.id,
    name: application.name,
    score: application.score,
    province: application.province,
    subjectType: application.subjectType,
    remark: application.remark || '',
    applications: application.applications || []
  }
  showDialog.value = true
}

// 重置表单
const resetForm = () => {
  applicationForm.value = {
    id: null,
    name: '',
    score: null,
    province: '',
    subjectType: '',
    remark: '',
    applications: []
  }
}

// 保存
const handleSave = () => {
  if (!applicationForm.value.name) {
    ElMessage.warning('请输入方案名称')
    return
  }

  if (dialogType.value === 'create') {
    createApplication()
  } else {
    updateApplication()
  }
}

// 分页
const handlePageChange = (page) => {
  currentPage.value = page
  fetchApplications()
}

// 获取状态名称
const getStatusName = (status) => {
  const statusMap = {
    0: '草稿',
    1: '已提交',
    2: '已录取'
  }
  return statusMap[status] || '未知'
}

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'success',
    2: 'primary'
  }
  return typeMap[status] || 'info'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchApplications()
})
</script>

<template>
  <div class="applications-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">
          <el-icon><Document /></el-icon>
          我的志愿
        </h1>
        <p class="page-subtitle">管理您的志愿填报方案</p>
      </div>
    </div>

    <div class="container page-content">
      <!-- 未登录提示 -->
      <el-card v-if="!isLoggedIn" class="login-tip-card" shadow="never">
        <div class="login-tip-content">
          <el-icon :size="64"><UserFilled /></el-icon>
          <h2>请先登录</h2>
          <p>登录后可管理志愿方案</p>
          <el-button type="primary" size="large" @click="router.push('/user')">
            前往登录
          </el-button>
        </div>
      </el-card>

      <!-- 已登录状态 -->
      <template v-else>
        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新建方案
          </el-button>
          <el-button @click="router.push('/ai-assistant')">
            <el-icon><ChatDotRound /></el-icon>
            AI推荐
          </el-button>
        </div>

        <el-skeleton :loading="loading" animated>
          <template #default>
            <!-- 空状态 -->
            <el-empty v-if="applications.length === 0" description="暂无志愿方案">
              <el-button type="primary" @click="openCreateDialog">
                创建第一个方案
              </el-button>
            </el-empty>

            <!-- 志愿列表 -->
            <el-row v-else :gutter="24">
              <el-col
                v-for="app in applications"
                :key="app.id"
                :xs="24"
                :sm="12"
                :md="8"
                :lg="6"
              >
                <el-card class="application-card hover-lift" shadow="hover">
                  <div class="app-header">
                    <h3 class="app-name">{{ app.name }}</h3>
                    <el-tag :type="getStatusType(app.status)" size="small" effect="light">
                      {{ getStatusName(app.status) }}
                    </el-tag>
                  </div>

                  <div class="app-info">
                    <div class="info-item">
                      <el-icon><Document /></el-icon>
                      <span>{{ app.choiceCount || 0 }} 个志愿</span>
                    </div>
                    <div class="info-item">
                      <el-icon><Clock /></el-icon>
                      <span>{{ formatDate(app.updateTime) }}</span>
                    </div>
                  </div>

                  <div class="app-desc">
                    {{ app.remark || '暂无备注' }}
                  </div>

                  <el-divider />

                  <div class="app-actions">
                    <el-button size="small" @click="openEditDialog(app)">
                      编辑
                    </el-button>
                    <el-button size="small" @click="analyzeApplication(app.id)">
                      分析
                    </el-button>
                    <el-dropdown trigger="click">
                      <el-button size="small">
                        更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item @click="copyApplication(app.id)">
                            复制
                          </el-dropdown-item>
                          <el-dropdown-item
                            v-if="app.status === 0"
                            @click="submitApplication(app.id)"
                          >
                            提交
                          </el-dropdown-item>
                          <el-dropdown-item divided @click="deleteApplication(app.id)">
                            删除
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </el-card>
              </el-col>
            </el-row>

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
          </template>
        </el-skeleton>

        <!-- 创建/编辑对话框 -->
        <el-dialog
          v-model="showDialog"
          :title="dialogType === 'create' ? '新建志愿方案' : '编辑志愿方案'"
          width="500px"
        >
          <el-form label-position="top">
            <el-form-item label="方案名称">
              <el-input
                v-model="applicationForm.name"
                placeholder="例如：2024年志愿方案"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="高考分数">
              <el-input-number
                v-model="applicationForm.score"
                :min="0"
                :max="750"
                placeholder="请输入高考分数"
              />
            </el-form-item>
            <el-form-item label="省份">
              <el-input
                v-model="applicationForm.province"
                placeholder="请输入所在省份"
              />
            </el-form-item>
            <el-form-item label="科类">
              <el-select v-model="applicationForm.subjectType" placeholder="请选择科类">
                <el-option label="物理类" value="物理类" />
                <el-option label="历史类" value="历史类" />
                <el-option label="理科" value="理科" />
                <el-option label="文科" value="文科" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="applicationForm.remark"
                type="textarea"
                :rows="3"
                placeholder="描述您的填报策略和目标..."
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>

          <template #footer>
            <el-button @click="showDialog = false">取消</el-button>
            <el-button type="primary" @click="handleSave">保存</el-button>
          </template>
        </el-dialog>
      </template>
    </div>
  </div>
</template>

<style scoped>
.applications-page {
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

/* 操作栏 */
.action-bar {
  margin-bottom: 1.5rem;
  display: flex;
  gap: 1rem;
}

/* 志愿卡片 */
.application-card {
  border-radius: 16px !important;
  margin-bottom: 1rem;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.app-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.app-info {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: #64748b;
  font-size: 0.85rem;
}

.app-desc {
  color: #94a3b8;
  font-size: 0.85rem;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
  }
}
</style>