<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const collapsed = ref(false)
function toggleSidebar() {
  collapsed.value = !collapsed.value
}
</script>

<template>
  <div class="layout">
    <Sidebar :collapsed="collapsed" />
    <div class="layout-main" :class="{ 'is-collapsed': collapsed }">
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
