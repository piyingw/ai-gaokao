<script setup>
import { ref, nextTick, computed } from 'vue'
import { aiApi } from '@/services/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const messages = ref([
  {
    id: 1,
    text: '您好！我是高考志愿填报AI助手，很高兴为您服务！\n\n我可以帮您解答以下问题：\n• 院校选择与推荐\n• 专业分析与建议\n• 志愿填报策略\n• 政策解读与问答\n• 性格分析与专业匹配\n\n请问有什么可以帮您的吗？',
    sender: 'ai',
    timestamp: new Date()
  }
])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

const quickQuestions = [
  { text: '我的分数能上什么大学？', icon: 'School' },
  { text: '如何选择适合的专业？', icon: 'Reading' },
  { text: '什么是冲稳保策略？', icon: 'TrendCharts' },
  { text: '平行志愿怎么填？', icon: 'Document' },
  { text: '热门专业就业前景', icon: 'Briefcase' },
  { text: '如何平衡学校和专业？', icon: 'ScaleToOriginal' }
]

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const userMessage = {
    id: messages.value.length + 1,
    text: inputMessage.value,
    sender: 'user',
    timestamp: new Date()
  }
  messages.value.push(userMessage)
  const question = inputMessage.value
  inputMessage.value = ''
  loading.value = true

  scrollToBottom()

  try {
    const response = await aiApi.chat({
      message: question,
      sessionId: 'session-' + Date.now()
    })

    const aiResponse = {
      id: messages.value.length + 1,
      text: response.data.content || '抱歉，我没有理解您的问题，请重新表述。',
      sender: 'ai',
      timestamp: new Date()
    }
    messages.value.push(aiResponse)
  } catch (error) {
    console.error('AI聊天出错:', error)
    const aiResponse = {
      id: messages.value.length + 1,
      text: '抱歉，AI服务暂时不可用，请稍后再试。您可以尝试刷新页面或联系技术支持。',
      sender: 'ai',
      timestamp: new Date()
    }
    messages.value.push(aiResponse)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatTime = (timestamp) => {
  return timestamp.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 根据登录状态显示不同的功能
const showPersonalizedFeatures = computed(() => userStore.isLoggedIn)
</script>

<template>
  <div class="ai-page">
    <div class="container page-container">
      <el-row :gutter="24" class="chat-layout">
        <!-- 左侧快捷问题和功能 -->
        <el-col :xs="24" :md="6" class="sidebar-col">
          <el-card class="sidebar-card" shadow="never">
            <template #header>
              <div class="sidebar-header">
                <el-icon><Opportunity /></el-icon>
                <span>快捷提问</span>
              </div>
            </template>

            <div class="quick-questions">
              <el-button
                v-for="(question, index) in quickQuestions"
                :key="index"
                class="quick-btn"
                @click="handleQuickQuestion(question.text)"
              >
                <el-icon><component :is="question.icon" /></el-icon>
                {{ question.text }}
              </el-button>
            </div>

            <el-divider />

            <div class="features-section">
              <h4>
                <el-icon><Lightning /></el-icon>
                AI 功能
              </h4>
              <div class="feature-list">
                <div class="feature-item">
                  <el-icon><ChatLineRound /></el-icon>
                  <span>智能问答</span>
                </div>
                <div class="feature-item">
                  <el-icon><Memo /></el-icon>
                  <span>志愿推荐</span>
                </div>
                <div class="feature-item">
                  <el-icon><School /></el-icon>
                  <span>院校查询</span>
                </div>
                <div class="feature-item">
                  <el-icon><Reading /></el-icon>
                  <span>专业分析</span>
                </div>
                <div v-if="showPersonalizedFeatures" class="feature-item">
                  <el-icon><User /></el-icon>
                  <span>个性推荐</span>
                </div>
              </div>
            </div>

            <el-divider />

            <div class="tips-section">
              <h4>
                <el-icon><InfoFilled /></el-icon>
                使用提示
              </h4>
              <ul>
                <li>输入您的高考分数，获取推荐院校</li>
                <li>询问专业详情和就业前景</li>
                <li>了解志愿填报策略和技巧</li>
                <li>获取最新政策解读</li>
              </ul>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧聊天区域 -->
        <el-col :xs="24" :md="18" class="chat-col">
          <el-card class="chat-card" shadow="never">
            <!-- 聊天头部 -->
            <template #header>
              <div class="chat-header">
                <div class="assistant-info">
                  <div class="assistant-avatar">
                    <el-icon :size="28"><ChatDotRound /></el-icon>
                  </div>
                  <div class="assistant-details">
                    <h3>AI智能助手</h3>
                    <div class="status">
                      <span class="status-dot"></span>
                      在线
                    </div>
                  </div>
                </div>
                <el-tag type="success" effect="light">
                  <el-icon><Cpu /></el-icon>
                  通义千问驱动
                </el-tag>
              </div>
            </template>

            <!-- 消息列表 -->
            <div class="messages-container" ref="messagesContainer">
              <div
                v-for="message in messages"
                :key="message.id"
                :class="['message', message.sender]"
              >
                <div class="message-avatar">
                  <el-icon v-if="message.sender === 'ai'" :size="20"><ChatDotRound /></el-icon>
                  <el-icon v-else :size="20"><User /></el-icon>
                </div>
                <div class="message-content">
                  <div class="message-text" v-html="message.text.replace(/\n/g, '<br>')"></div>
                  <div class="message-time">{{ formatTime(message.timestamp) }}</div>
                </div>
              </div>

              <!-- 加载中状态 -->
              <div v-if="loading" class="message ai">
                <div class="message-avatar">
                  <el-icon :size="20"><ChatDotRound /></el-icon>
                </div>
                <div class="message-content">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="input-area">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="2"
                placeholder="请输入您想咨询的问题，例如：我的分数能上什么大学？"
                resize="none"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <el-button
                type="primary"
                :loading="loading"
                :disabled="!inputMessage.trim()"
                @click="sendMessage"
              >
                <el-icon v-if="!loading"><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  min-height: calc(100vh - 200px);
  padding: 2rem 0;
}

.page-container {
  height: calc(100vh - 200px);
}

.chat-layout {
  height: 100%;
}

/* 侧边栏 */
.sidebar-col {
  margin-bottom: 1.5rem;
}

.sidebar-card {
  height: 100%;
  border-radius: 16px !important;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1e293b;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.quick-btn {
  justify-content: flex-start;
  text-align: left;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.quick-btn:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: #fff;
  transform: translateX(4px);
}

.quick-btn .el-icon {
  margin-right: 0.5rem;
}

.features-section h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 8px;
  color: #475569;
  font-size: 0.9rem;
}

.feature-item .el-icon {
  color: #667eea;
}

.tips-section h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #1e293b;
  margin: 0 0 0.75rem;
}

.tips-section ul {
  margin: 0;
  padding-left: 1.25rem;
  color: #64748b;
  font-size: 0.85rem;
  line-height: 1.8;
}

/* 聊天区域 */
.chat-col {
  height: 100%;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px !important;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.assistant-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.assistant-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.assistant-details h3 {
  margin: 0 0 0.25rem;
  font-size: 1.1rem;
  color: #1e293b;
}

.assistant-details .status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
}

/* 消息列表 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.message {
  display: flex;
  gap: 0.75rem;
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.ai .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.message.user .message-avatar {
  background: #e2e8f0;
  color: #64748b;
}

.message-content {
  flex: 1;
}

.message-text {
  padding: 1rem 1.25rem;
  border-radius: 16px;
  line-height: 1.7;
  font-size: 0.95rem;
}

.message.ai .message-text {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #334155;
  border-top-left-radius: 4px;
}

.message.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 0.5rem;
  padding: 0 0.5rem;
}

.message.user .message-time {
  text-align: right;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}

.input-area .el-textarea {
  flex: 1;
}

.input-area .el-button {
  align-self: flex-end;
  height: 40px;
  padding: 0 1.5rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-container {
    height: auto;
  }

  .sidebar-col {
    margin-bottom: 1rem;
  }

  .chat-card {
    min-height: 500px;
  }

  .message {
    max-width: 95%;
  }

  .chat-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .input-area {
    flex-direction: column;
  }

  .input-area .el-button {
    align-self: flex-end;
  }
}
</style>