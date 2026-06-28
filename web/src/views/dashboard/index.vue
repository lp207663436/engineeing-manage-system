<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  // 此页面已被 workbench.vue 替代,自动重定向
  router.replace('/dashboard')
})
</script>

<template>
  <div style="padding: 24px; color: #9CA3AF;">正在跳转到工作台...</div>
</template>
