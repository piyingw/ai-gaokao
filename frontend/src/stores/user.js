import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/services/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  // 登录
  const login = async (loginData) => {
    const response = await userApi.login(loginData)
    token.value = response.data.token
    localStorage.setItem('token', response.data.token)
    await fetchUserInfo()
    return response
  }

  // 注册
  const register = async (registerData) => {
    const response = await userApi.register(registerData)
    return response
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    if (!token.value) return null
    try {
      const response = await userApi.getUserInfo()
      userInfo.value = response.data
      return response.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  // 发送验证码
  const sendEmailCode = async (email) => {
    return await userApi.sendEmailCode(email)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    fetchUserInfo,
    logout,
    sendEmailCode
  }
})