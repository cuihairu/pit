<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const auditLogs = ref([])
const loading = ref(true)
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = ref(20)

onMounted(async () => {
  await loadAuditLogs()
})

async function loadAuditLogs() {
  loading.value = true
  try {
    const response = await api.get(`/api/audit-logs?page=${currentPage.value}&size=${pageSize.value}`)
    auditLogs.value = response.data.content || []
    totalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to load audit logs:', error)
  } finally {
    loading.value = false
  }
}

function getActionColor(action) {
  const colors = {
    CREATE: 'text-green-600',
    UPDATE: 'text-blue-600',
    DELETE: 'text-red-600',
    LOGIN: 'text-purple-600',
    LOGOUT: 'text-gray-600',
    LOGIN_FAILED: 'text-red-600'
  }
  return colors[action] || 'text-gray-600'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function goToPage(page) {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    loadAuditLogs()
  }
}
</script>

<template>
  <div>
    <div class="sm:flex sm:items-center mb-8">
      <div class="sm:flex-auto">
        <h1 class="text-2xl font-bold text-gray-900">审计日志</h1>
        <p class="mt-1 text-sm text-gray-500">
          查看系统操作记录
        </p>
      </div>
    </div>

    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="auditLogs.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无审计日志</h3>
      <p class="mt-1 text-sm text-gray-500">系统操作记录将显示在这里</p>
    </div>

    <div v-else class="card">
      <div class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>时间</th>
              <th>用户</th>
              <th>操作</th>
              <th>资源类型</th>
              <th>资源ID</th>
              <th>状态</th>
              <th>IP地址</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in auditLogs" :key="log.id">
              <td class="text-sm">{{ formatDate(log.createdAt) }}</td>
              <td class="font-medium">{{ log.username || '-' }}</td>
              <td>
                <span :class="getActionColor(log.action)" class="font-medium">
                  {{ log.action }}
                </span>
              </td>
              <td>{{ log.resourceType }}</td>
              <td class="font-mono text-xs">{{ log.resourceId || '-' }}</td>
              <td>
                <span :class="log.status === 'SUCCESS' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'" class="badge">
                  {{ log.status }}
                </span>
              </td>
              <td class="text-sm text-gray-500">{{ log.ipAddress || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="mt-4 flex items-center justify-between">
        <div class="text-sm text-gray-500">
          第 {{ currentPage + 1 }} 页，共 {{ totalPages }} 页
        </div>
        <div class="flex space-x-2">
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn btn-secondary text-sm"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage === 0 }"
          >
            上一页
          </button>
          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn btn-secondary text-sm"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage >= totalPages - 1 }"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>