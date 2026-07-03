import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || null)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.roles?.includes('ADMIN') || false)
  const userName = computed(() => user.value?.displayName || user.value?.username || '')

  async function login(username, password) {
    loading.value = true
    error.value = null
    
    try {
      const response = await api.post('/api/auth/login', { username, password })
      token.value = response.data.token
      user.value = response.data.user
      localStorage.setItem('token', token.value)
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
      return true
    } catch (err) {
      error.value = err.response?.data?.message || '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await api.post('/api/auth/logout')
    } catch (err) {
      // 忽略登出错误
    } finally {
      token.value = null
      user.value = null
      localStorage.removeItem('token')
      delete api.defaults.headers.common['Authorization']
    }
  }

  async function fetchUser() {
    if (!token.value) return
    
    try {
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
      const response = await api.get('/api/users/me')
      user.value = response.data
    } catch (err) {
      // Token无效，清除认证状态
      token.value = null
      user.value = null
      localStorage.removeItem('token')
      delete api.defaults.headers.common['Authorization']
    }
  }

  function init() {
    if (token.value) {
      fetchUser()
    }
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    userName,
    login,
    logout,
    fetchUser,
    init
  }
})