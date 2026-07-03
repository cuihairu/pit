<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const apiKeys = ref([])
const loading = ref(true)
const showCreateModal = ref(false)
const newApiKey = ref({
  name: '',
  gameId: '',
  environment: 'dev',
  type: 'client'
})

const games = ref([])

onMounted(async () => {
  await Promise.all([loadApiKeys(), loadGames()])
})

async function loadApiKeys() {
  loading.value = true
  try {
    const response = await api.get('/api/api-keys')
    apiKeys.value = response.data.content || []
  } catch (error) {
    console.error('Failed to load API keys:', error)
  } finally {
    loading.value = false
  }
}

async function loadGames() {
  try {
    const response = await api.get('/api/games')
    games.value = response.data.content || []
  } catch (error) {
    console.error('Failed to load games:', error)
  }
}

async function createApiKey() {
  try {
    await api.post('/api/api-keys', newApiKey.value)
    showCreateModal.value = false
    newApiKey.value = {
      name: '',
      gameId: '',
      environment: 'dev',
      type: 'client'
    }
    await loadApiKeys()
  } catch (error) {
    console.error('Failed to create API key:', error)
    alert('创建API密钥失败: ' + (error.response?.data?.message || error.message))
  }
}

async function deleteApiKey(keyId) {
  if (!confirm('确定要删除这个API密钥吗？')) return
  
  try {
    await api.delete(`/api/api-keys/${keyId}`)
    await loadApiKeys()
  } catch (error) {
    console.error('Failed to delete API key:', error)
    alert('删除API密钥失败: ' + (error.response?.data?.message || error.message))
  }
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text)
    .then(() => alert('已复制到剪贴板'))
    .catch(() => alert('复制失败'))
}

function maskKey(key) {
  if (!key) return ''
  return key.substring(0, 8) + '...' + key.substring(key.length - 8)
}
</script>

<template>
  <div>
    <div class="sm:flex sm:items-center mb-8">
      <div class="sm:flex-auto">
        <h1 class="text-2xl font-bold text-gray-900">API密钥管理</h1>
        <p class="mt-1 text-sm text-gray-500">
          管理您的API访问密钥
        </p>
      </div>
      <div class="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
        <button @click="showCreateModal = true" class="btn btn-primary">
          创建API密钥
        </button>
      </div>
    </div>

    <!-- API密钥列表 -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="apiKeys.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无API密钥</h3>
      <p class="mt-1 text-sm text-gray-500">创建一个API密钥来开始使用</p>
      <div class="mt-6">
        <button @click="showCreateModal = true" class="btn btn-primary">
          创建API密钥
        </button>
      </div>
    </div>

    <div v-else class="card">
      <div class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>名称</th>
              <th>密钥</th>
              <th>游戏</th>
              <th>环境</th>
              <th>类型</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="apiKey in apiKeys" :key="apiKey.id">
              <td>{{ apiKey.name }}</td>
              <td>
                <div class="flex items-center space-x-2">
                  <code class="text-xs bg-gray-100 px-2 py-1 rounded">
                    {{ maskKey(apiKey.key) }}
                  </code>
                  <button
                    @click="copyToClipboard(apiKey.key)"
                    class="text-primary-600 hover:text-primary-700"
                    title="复制"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </button>
                </div>
              </td>
              <td>{{ apiKey.gameId }}</td>
              <td>{{ apiKey.environment }}</td>
              <td>
                <span class="badge bg-blue-100 text-blue-800">
                  {{ apiKey.type }}
                </span>
              </td>
              <td>{{ new Date(apiKey.createdAt).toLocaleDateString('zh-CN') }}</td>
              <td>
                <button
                  @click="deleteApiKey(apiKey.id)"
                  class="text-red-600 hover:text-red-700"
                  title="删除"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 创建API密钥模态框 -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 z-50 overflow-y-auto"
      @click.self="showCreateModal = false"
    >
      <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>
        
        <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
          <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
            <h3 class="text-lg font-medium text-gray-900 mb-4">创建API密钥</h3>
            
            <form @submit.prevent="createApiKey">
              <div class="space-y-4">
                <div>
                  <label class="label">名称 *</label>
                  <input
                    v-model="newApiKey.name"
                    type="text"
                    required
                    class="input"
                    placeholder="例如: iOS SDK Key"
                  />
                </div>
                
                <div>
                  <label class="label">游戏 *</label>
                  <select v-model="newApiKey.gameId" required class="input">
                    <option value="">选择游戏</option>
                    <option v-for="game in games" :key="game.id" :value="game.id">
                      {{ game.displayName || game.name }}
                    </option>
                  </select>
                </div>
                
                <div>
                  <label class="label">环境</label>
                  <select v-model="newApiKey.environment" class="input">
                    <option value="dev">开发</option>
                    <option value="staging">测试</option>
                    <option value="prod">生产</option>
                  </select>
                </div>
                
                <div>
                  <label class="label">类型</label>
                  <select v-model="newApiKey.type" class="input">
                    <option value="client">客户端</option>
                    <option value="server">服务端</option>
                    <option value="admin">管理</option>
                  </select>
                </div>
              </div>
              
              <div class="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                <button type="submit" class="btn btn-primary w-full sm:w-auto sm:ml-3">
                  创建
                </button>
                <button
                  type="button"
                  @click="showCreateModal = false"
                  class="btn btn-secondary w-full sm:w-auto mt-3 sm:mt-0"
                >
                  取消
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>