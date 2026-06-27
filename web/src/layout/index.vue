<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const collapsed = ref(false)
// 移动端抽屉式侧边栏:< 768px 时 toggle 行为改为开/关抽屉
const isMobile = ref(false)
const mobileOpen = ref(false)

function syncMobile() {
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
  if (!isMobile.value) mobileOpen.value = false
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
  } else {
    collapsed.value = !collapsed.value
  }
}

// 路由切换后自动关闭移动端抽屉
const route = useRoute()
watch(() => route.path, () => { mobileOpen.value = false })

onMounted(() => {
  syncMobile()
  window.addEventListener('resize', syncMobile)
})
onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<template>
  <div class="layout" :class="{ 'is-mobile-open': isMobile && mobileOpen }">
    <Sidebar :collapsed="collapsed" />
    <div class="layout-main" :class="{ 'is-collapsed': collapsed && !isMobile }">
      <Header :collapsed="collapsed" @toggle="toggleSidebar" />
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout { display: flex; height: 100vh; }
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 240px;
  transition: margin-left 0.2s ease;
  &.is-collapsed { margin-left: 64px; }
}
.layout-content {
  flex: 1;
  overflow: auto;
  background: #F9FAFB;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
