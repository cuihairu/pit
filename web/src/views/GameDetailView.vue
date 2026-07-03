<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'

const route = useRoute()
const router = useRouter()
const game = ref(null)
const environments = ref([])
const loading = ref(true)
const error = ref(null)

const gameId = route.params.gameId

onMounted(async () => {
  await loadGame()
})

async function loadGame() {
  loading.value = true
  error.value = null
  
  try {
    const [gameRes, envsRes] = await Promise.all([
      api.get(`/api/games/${gameId}`),
      api.get(`/api/games/${gameId}/environments`)
    ])
    
    game.value = gameRes.data
    environments.value = envsRes.data || []
  } catch (err) {
    error.value = err.response?.data?.message || '加载游戏详情失败'
    console.error('Failed to load game:', err)
  } finally {
    loading.value = false
  }
}

function getStatusColor(status) {
  const colors = {
    DEVELOPMENT: 'bg-yellow-100 text-yellow-800',
    TESTING: 'bg-blue-100 text-blue-800',
    LIVE: 'bg-green-100 text-green-800',
    MAINTENANCE: 'bg-orange-100 text-orange-800',
    DISCONTINUED: 'bg-gray-100 text-gray-800'
  }
  return colors[status] || 'bg-gray-100 text-gray-800'
}

function getStatusLabel(status) {
  const labels = {
    DEVELOPMENT: '开发中',
    TESTING: '测试中',
    LIVE: '已上线',
    MAINTENANCE: '维护中',
    DISCONTINUED: '已停服'
  }
  return labels[status] || status
}
</script>

<template>
  <div>
    <div class="mb-8">
      <button @click="router.push('/games')" class="text-primary-600 hover:text-primary-700 mb-4">
        ← 返回游戏列表
      </button>
      
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
        <p class="mt-4 text-gray-500">加载中...</p>
      </div>
      
      <div v-else-if="error" class="text-center py-12">
        <p class="text-red-600">{{ error }}</p>
        <button @click="loadGame" class="btn btn-primary mt-4">重试</button>
      </div>
      
      <div v-else-if="game">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-2xl font-bold text-gray-900">{{ game.displayName || game.name }}</h1>
          <span :class="[getStatusColor(game.status), 'badge']">
            {{ getStatusLabel(game.status) }}
          </span>
        </div>
        
        <p v-if="game.description" class="text-gray-600 mb-6">{{ game.description }}</p>
        
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <!-- 游戏信息 -->
          <div class="card">
            <h3 class="text-lg font-medium text-gray-900 mb-4">游戏信息</h3>
            <dl class="space-y-3">
              <div>
                <dt class="text-sm font-medium text-gray-500">游戏ID</dt>
                <dd class="text-sm text-gray-900 font-mono">{{ game.id }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">类型</dt>
                <dd class="text-sm text-gray-900">{{ game.genre }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">平台</dt>
                <dd class="text-sm text-gray-900">{{ game.platforms?.join(', ') || '-' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">默认货币</dt>
                <dd class="text-sm text-gray-900">{{ game.defaultCurrency || 'USD' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">创建时间</dt>
                <dd class="text-sm text-gray-900">{{ new Date(game.createdAt).toLocaleString('zh-CN') }}</dd>
              </div>
            </dl>
          </div>
          
          <!-- 环境列表 -->
          <div class="card">
            <h3 class="text-lg font-medium text-gray-900 mb-4">环境</h3>
            <div v-if="environments.length === 0" class="text-center py-4 text-gray-500">
              暂无环境配置
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="env in environments"
                :key="env.id"
                class="flex items-center justify-between p-3 rounded-lg bg-gray-50"
              >
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ env.name }}</p>
                  <p class="text-xs text-gray-500">{{ env.description || env.environment }}</p>
                </div>
                <span class="badge bg-green-100 text-green-800">
                  {{ env.environment }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>