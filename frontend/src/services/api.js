// API 服务
const API_BASE_URL = 'http://localhost:8088/api'

// 通用请求方法
async function request(url, options = {}) {
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  }

  // 如果有token，在请求头中添加
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${url}`, config)

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
  }

  return response.json()
}

// 用户相关API
export const userApi = {
  // 用户注册
  register(data) {
    return request('/user/register', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 用户登录 (支持用户名或邮箱登录)
  login(data) {
    return request('/user/login', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 退出登录
  logout() {
    return request('/user/logout', {
      method: 'POST'
    })
  },

  // 获取用户信息
  getUserInfo() {
    return request('/user/info', {
      method: 'GET'
    })
  },

  // 更新用户信息
  updateUserInfo(data) {
    return request('/user/info', {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  // 修改密码
  changePassword(data) {
    const params = new URLSearchParams({
      oldPassword: data.oldPassword,
      newPassword: data.newPassword
    })
    return request(`/user/password?${params}`, {
      method: 'PUT'
    })
  },

  // 发送邮箱验证码
  sendEmailCode(email) {
    return request(`/user/code/email?email=${encodeURIComponent(email)}`, {
      method: 'POST'
    })
  }
}

// 院校相关API
export const universityApi = {
  // 分页查询院校
  getUniversityList(params) {
    const queryParams = new URLSearchParams(params).toString()
    return request(`/university/list?${queryParams}`)
  },

  // 获取院校详情
  getUniversityDetail(id) {
    return request(`/university/${id}`)
  },

  // 获取院校历年分数线
  getUniversityScores(id, params = {}) {
    const queryParams = new URLSearchParams(params).toString()
    return request(`/university/${id}/scores?${queryParams}`)
  },

  // 获取院校开设专业
  getUniversityMajors(id) {
    return request(`/university/${id}/majors`)
  },

  // 院校对比
  compareUniversities(ids) {
    return request('/university/compare', {
      method: 'POST',
      body: JSON.stringify(ids)
    })
  }
}

// 专业相关API
export const majorApi = {
  // 分页查询专业
  getMajorList(params) {
    const queryParams = new URLSearchParams(params).toString()
    return request(`/major/list?${queryParams}`)
  },

  // 获取专业详情
  getMajorDetail(id) {
    return request(`/major/${id}`)
  },

  // 按学科门类获取专业
  getMajorsByCategory(category) {
    return request(`/major/category/${category}`)
  },

  // 搜索专业
  searchMajors(keyword, limit = 10) {
    return request(`/major/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
  },

  // 根据性格推荐专业
  recommendByPersonality(personalityType) {
    return request(`/major/recommend?personalityType=${encodeURIComponent(personalityType)}`)
  },

  // 获取所有学科门类
  getCategories() {
    return request('/major/categories')
  },

  // 获取专业类列表
  getSubCategories(category) {
    return request(`/major/subcategories?category=${encodeURIComponent(category)}`)
  }
}

// AI 相关API
export const aiApi = {
  // AI 对话
  chat(data) {
    return request('/ai/chat', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 一键生成志愿
  oneClickRecommend(data) {
    return request('/ai/recommend/one-click', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 志愿推荐
  recommend(data) {
    return request('/ai/recommend', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 政策问答
  policyQA(question) {
    return request(`/ai/policy/qa?question=${encodeURIComponent(question)}`, {
      method: 'POST'
    })
  },

  // 学校信息查询
  schoolInfo(query) {
    return request(`/ai/school/info?query=${encodeURIComponent(query)}`, {
      method: 'POST'
    })
  },

  // 性格分析
  personalityAnalysis(description) {
    return request(`/ai/personality/analyze?description=${encodeURIComponent(description)}`, {
      method: 'POST'
    })
  },

  // 获取 Agent 列表
  getAgents() {
    return request('/ai/agents', {
      method: 'GET'
    })
  }
}