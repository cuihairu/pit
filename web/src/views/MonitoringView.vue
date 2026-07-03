<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const systemHealth = ref(null)
const loading = ref(true)

onMounted(async () => {
  await loadSystemHealth()
})

async function loadSystemHealth() {
  loading.value = true
  try {
    const response = await api.get('/api/health/overview')
    systemHealth.value = response.data
  } catch (error) {
    console.error('Failed to load system health:', error)
  } finally {
    loading.value = false
  }
}

function getStatusColor(status) {
  const colors = {
    HEALTHY: 'text-green-600',
    DEGRADED: 'text-yellow-600',
    UNHEALTHY: 'text-red-600'
  }
  return colors[status] || 'text-gray-600'
}

function getStatusLabel(status) {
  const labels = {
    HEALTHY: '健康',
    DEGRADED: '降级',
    UNHEALTHY: '不健康'
  }
  return labels[status] || status
}
</script>

<template>
  <div>
    <div class="sm:flex sm:items-center mb-8">
      <div class="sm:flex-auto">
        <h1 class="text-2xl font-bold text-gray-900">系统监控</h1>
        <p class="mt-1 text-sm text-gray-500">
          查看系统健康状态和性能指标
        </p>
      </div>
      <div class="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
        <button @click="loadSystemHealth" class="btn btn-secondary">
          刷新
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="!systemHealth" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">无法获取系统状态</h3>
      <p class="mt-1 text-sm text-gray-500">请稍后重试</p>
    </div>

    <div v-else>
      <!-- 系统状态概览 -->
      <div class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <div class="card">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">系统状态</p>
              <p :class="getStatusColor(systemHealth.overallStatus)" class="text-2xl font-semibold">
                {{ getStatusLabel(systemHealth.overallStatus) }}
              </p>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">运行时间</p>
              <p class="text-2xl font-semibold text-gray-900">
                {{ systemHealth.uptime || '-' }}
              </p>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">版本</p>
              <p class="text-2xl font-semibold text-gray-900">
                {{ systemHealth.version || '-' }}
              </p>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">数据库</p>
              <p class="text-2xl font-semibold text-gray-900">
                {{ systemHealth.database || '-' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 组件状态 -->
      <div class="card mb-8">
        <h3 class="text-lg font-medium text-gray-900 mb-4">组件状态</h3>
        <div v-if="systemHealth.components && systemHealth.components.length > 0" class="space-y-3">
          <div
            v-for="component in systemHealth.components"
            :key="component.name"
            class="flex items-center justify-between p-3 rounded-lg bg-gray-50"
          >
            <div class="flex items-center">
              <div
                :class="[
                  component.status === 'HEALTHY' ? 'bg-green-500' : 
                  component.status === 'DEGRADED' ? 'bg-yellow-500' : 'bg-red-500',
                  'w-3 h-3 rounded-full mr-3'
                ]"
              ></div>
              <div>
                <p class="text-sm font-medium text-gray-900">{{ component.name }}</p>
                <p v-if="component.message" class="text-xs text-gray-500">{{ component.message }}</p>
              </div>
            </div>
            <span :class="getStatusColor(component.status)" class="text-sm font-medium">
              {{ getStatusLabel(component.status) }}
            </span>
          </div>
        </div>
        <div v-else class="text-center py-4 text-gray-500">
          暂无组件状态信息
        </div>
      </div>

      <!-- 指标链接 -->
      <div class="card">
        <h3 class="text-lg font-medium text-gray-900 mb-4">监控指标</h3>
        <p class="text-sm text-gray-600 mb-4">
          系统指标通过 Prometheus 采集，可通过 Grafana 进行可视化。
        </p>
        <div class="space-y-2">
          <a
            href="/actuator/prometheus"
            target="_blank"
            class="flex items-center p-3 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <svg class="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
            <div>
              <p class="text-sm font-medium text-gray-900">Prometheus 指标端点</p>
              <p class="text-xs text-gray-500">/actuator/prometheus</p>
            </div>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>