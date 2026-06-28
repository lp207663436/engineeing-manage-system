<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="error-page">
    <div class="error-code">403</div>
    <div class="error-text">抱歉,您没有权限访问该页面</div>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #F9FAFB;
}
.error-code {
  font-size: 96px;
  font-weight: 700;
  color: #4F6BED;
  line-height: 1;
}
.error-text {
  font-size: 16px;
  color: #6B7280;
  margin-bottom: 8px;
}
</style>
