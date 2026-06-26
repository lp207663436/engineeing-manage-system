<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>首页</h1>
      <p class="sub">欢迎使用智能化工程管理系统</p>
    </div>
    <div class="card-grid">
      <div class="card">
        <div class="card-title">项目总数</div>
        <div class="card-value">0</div>
      </div>
      <div class="card">
        <div class="card-title">进行中合同</div>
        <div class="card-value">0</div>
      </div>
      <div class="card">
        <div class="card-title">待验收报价</div>
        <div class="card-value">0</div>
      </div>
      <div class="card">
        <div class="card-title">在维保项目</div>
        <div class="card-value">0</div>
      </div>
    </div>
    <div class="card mt-24">
      <div class="card-title">快速入口</div>
      <p class="empty-tip">业务模块开发中,请通过左侧菜单访问「系统管理」。</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard { padding: 24px; }
.page-header {
  margin-bottom: 24px;
  h1 { font-size: 20px; font-weight: 600; color: #1F2937; letter-spacing: -0.01em; }
  .sub { font-size: 14px; color: #9CA3AF; margin-top: 4px; }
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 20px;
}
.card-title { font-size: 12px; color: #9CA3AF; }
.card-value { font-size: 28px; font-weight: 600; color: #1F2937; margin-top: 8px; }
.empty-tip { font-size: 14px; color: #9CA3AF; margin-top: 12px; }
</style>
