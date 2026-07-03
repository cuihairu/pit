<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const users = ref([])
const loading = ref(true)

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const response = await api.get('/api/users')
    users.value = response.data.content || []
  } catch (error) {
    console.error('Failed to load users:', error)
  } finally {
    loading.value = false
  }
}

function getStatusColor(status) {
  const colors = {
    ACTIVE: 'bg-green-100 text-green-800',
    INACTIVE: 'bg-gray-100 text-gray-800',
    LOCKED: 'bg-red-100 text-red-800',
    PENDING: 'bg-yellow-100 text-yellow-800'
  }
  return colors[status] || 'bg-gray-100 text-gray-800'
}

function getStatusLabel(status) {
  const labels = {
    ACTIVE: '活跃',
    INACTIVE: '未激活',
    LOCKED: '已锁定',
    PENDING: '待审核'
  }
  return labels[status] || status
}
</script>

<template>
  <div>
    <div class="sm:flex sm:items-center mb-8">
      <div class="sm:flex-auto">
        <h1 class="text-2xl font-bold text-gray-900">用户管理</h1>
        <p class="mt-1 text-sm text-gray-500">
          管理系统用户
        </p>
      </div>
      <div class="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
        <button class="btn btn-primary">
          添加用户
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="users.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无用户</h3>
      <p class="mt-1 text-sm text-gray-500">添加一个用户来开始</p>
    </div>

    <div v-else class="card">
      <div class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>邮箱</th>
              <th>显示名称</th>
              <th>状态</th>
              <th>角色</th>
              <th>最后登录</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td class="font-medium">{{ user.username }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>{{ user.displayName || '-' }}</td>
              <td>
                <span :class="[getStatusColor(user.status), 'badge']">
                  {{ getStatusLabel(user.status) }}
                </span>
              </td>
              <td>
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="role in user.roles"
                    :key="role"
                    class="badge bg-blue-100 text-blue-800"
                  >
                    {{ role }}
                  </span>
                </div>
              </td>
              <td>
                {{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('zh-CN') : '从未登录' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>