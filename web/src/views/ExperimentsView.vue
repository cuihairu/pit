<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const experiments = ref([])
const loading = ref(true)

onMounted(async () => {
  await loadExperiments()
})

async function loadExperiments() {
  loading.value = true
  try {
    const response = await api.get('/api/experiments')
    experiments.value = response.data.content || []
  } catch (error) {
    console.error('Failed to load experiments:', error)
  } finally {
    loading.value = false
  }
}

function getStatusColor(status) {
  const colors = {
    DRAFT: 'bg-gray-100 text-gray-800',
    RUNNING: 'bg-green-100 text-green-800',
    PAUSED: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-blue-100 text-blue-800'
  }
  return colors[status] || 'bg-gray-100 text-gray-800'
}

function getStatusLabel(status) {
  const labels = {
    DRAFT: '草稿',
    RUNNING: '运行中',
    PAUSED: '已暂停',
    COMPLETED: '已完成'
  }
  return labels[status] || status
}
</script>

<template>
  <div>
    <div class="sm:flex sm:items-center mb-8">
      <div class="sm:flex-auto">
        <h1 class="text-2xl font-bold text-gray-900">实验管理</h1>
        <p class="mt-1 text-sm text-gray-500">
          管理A/B测试实验
        </p>
      </div>
      <div class="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
        <button class="btn btn-primary">
          创建实验
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="experiments.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无实验</h3>
      <p class="mt-1 text-sm text-gray-500">创建一个A/B测试实验来开始</p>
    </div>

    <div v-else class="card">
      <div class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>实验名称</th>
              <th>游戏</th>
              <th>状态</th>
              <th>变体数</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="exp in experiments" :key="exp.id">
              <td class="font-medium">{{ exp.name }}</td>
              <td>{{ exp.gameId }}</td>
              <td>
                <span :class="[getStatusColor(exp.status), 'badge']">
                  {{ getStatusLabel(exp.status) }}
                </span>
              </td>
              <td>{{ exp.variants?.length || 0 }}</td>
              <td>{{ new Date(exp.createdAt).toLocaleDateString('zh-CN') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>