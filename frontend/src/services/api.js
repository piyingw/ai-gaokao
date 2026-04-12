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

// 会员相关API
export const memberApi = {
  // 获取会员信息
  getMemberInfo() {
    return request('/member/info')
  },

  // 获取会员权益列表
  getMemberPrivileges() {
    return request('/member/privileges')
  },

  // 获取会员商品列表
  getMemberProducts() {
    return request('/member/products')
  },

  // 获取今日权益使用情况
  getTodayUsage() {
    return request('/member/usage')
  },

  // 获取AI对话使用次数
  getAiUsage() {
    return request('/member/ai-usage')
  }
}

// 订单相关API (Mock支付)
export const orderApi = {
  // 创建会员订单
  createOrder(productId) {
    return request('/order/create', {
      method: 'POST',
      body: JSON.stringify({ productId })
    })
  },

  // 发起支付 (Mock: 直接成功)
  payOrder(orderId, paymentMethod = 'MOCK') {
    return request('/order/pay', {
      method: 'POST',
      body: JSON.stringify({ orderId, paymentMethod })
    })
  },

  // 取消订单
  cancelOrder(orderId, reason = '') {
    return request(`/order/cancel/${orderId}?reason=${encodeURIComponent(reason)}`, {
      method: 'POST'
    })
  },

  // 获取订单列表
  getOrderList(status = '') {
    return request(`/order/list?status=${status}`)
  },

  // 获取订单详情
  getOrderDetail(orderId) {
    return request(`/order/detail/${orderId}`)
  },

  // 查询订单支付状态
  queryOrderStatus(orderNo) {
    return request(`/order/status/${orderNo}`)
  }
}

// 优惠券相关API
export const couponApi = {
  // 获取可领取的优惠券列表
  getTemplates() {
    return request('/coupon/templates')
  },

  // 领取优惠券
  claimCoupon(templateId) {
    return request(`/coupon/claim/${templateId}`, {
      method: 'POST'
    })
  },

  // 获取我的优惠券列表
  getMyCoupons() {
    return request('/coupon/list')
  },

  // 获取可用优惠券
  getAvailableCoupons(orderAmount) {
    return request(`/coupon/available?orderAmount=${orderAmount || ''}`)
  },

  // 计算优惠金额
  calculateDiscount(couponCode, orderAmount) {
    return request(`/coupon/calculate?couponCode=${couponCode}&orderAmount=${orderAmount}`, {
      method: 'POST'
    })
  }
}

// 志愿管理API
export const applicationApi = {
  // 分页查询志愿列表
  getApplicationList(pageNum = 1, pageSize = 10) {
    return request(`/application/list?pageNum=${pageNum}&pageSize=${pageSize}`)
  },

  // 获取志愿详情
  getApplicationDetail(id) {
    return request(`/application/${id}`)
  },

  // 创建志愿
  createApplication(data) {
    return request('/application', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 更新志愿
  updateApplication(data) {
    return request('/application', {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  // 删除志愿
  deleteApplication(id) {
    return request(`/application/${id}`, {
      method: 'DELETE'
    })
  },

  // 提交志愿
  submitApplication(id) {
    return request(`/application/${id}/submit`, {
      method: 'POST'
    })
  },

  // 复制志愿
  copyApplication(id) {
    return request(`/application/${id}/copy`, {
      method: 'POST'
    })
  },

  // 获取最新志愿
  getLatestApplication() {
    return request('/application/latest')
  },

  // 分析志愿方案
  analyzeApplication(id) {
    return request(`/application/${id}/analyze`)
  }
}

// 政策文档API
export const policyApi = {
  // 分页查询政策文档
  getPolicyList(params = {}) {
    const queryParams = new URLSearchParams(params).toString()
    return request(`/policy/list?${queryParams}`)
  },

  // 获取政策文档详情
  getPolicyDetail(id) {
    return request(`/policy/${id}`)
  },

  // 搜索政策文档
  searchPolicies(keyword, limit = 10) {
    return request(`/policy/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
  },

  // 按类型获取政策文档
  getPoliciesByType(type) {
    return request(`/policy/type/${type}`)
  },

  // 按省份获取政策文档
  getPoliciesByProvince(province) {
    return request(`/policy/province/${province}`)
  },

  // 获取热门政策
  getHotPolicies(limit = 10) {
    return request(`/policy/hot?limit=${limit}`)
  },

  // 获取政策文档类型列表
  getPolicyTypes() {
    return request('/policy/types')
  }
}