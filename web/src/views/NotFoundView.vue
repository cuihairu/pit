<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full text-center">
      <div class="mb-8">
        <h1 class="text-9xl font-bold text-primary-600">404</h1>
        <h2 class="mt-4 text-3xl font-bold text-gray-900">页面未找到</h2>
        <p class="mt-2 text-lg text-gray-600">
          抱歉，您访问的页面不存在
        </p>
      </div>
      
      <div class="space-x-4">
        <button @click="router.push('/')" class="btn btn-primary">
          返回首页
        </button>
        <button @click="router.back()" class="btn btn-secondary">
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>