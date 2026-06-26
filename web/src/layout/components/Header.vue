<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { Menu as MenuIcon, ChevronDown } from 'lucide-vue-next'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ (e: 'toggle'): void }>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const breadcrumb = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗?', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } catch {}
}
</script>

<template>
  <header class="layout-header">
    <div class="header-left">
      <button class="toggle-btn" @click="emit('toggle')">
        <MenuIcon :size="20" />
      </button>
      <nav class="breadcrumb">
        <template v-for="(item, idx) in breadcrumb" :key="idx">
          <span v-if="idx > 0" class="sep">/</span>
          <span class="crumb">{{ item.meta?.title }}</span>
        </template>
      </nav>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleLogout">
        <div class="user-info">
          <div class="avatar">{{ userStore.name?.charAt(0) || 'U' }}</div>
          <span class="username">{{ userStore.name || userStore.username || '用户' }}</span>
          <ChevronDown :size="14" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
.layout-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  color: #4B5563;
  display: flex;
  align-items: center;
  &:hover { background: #F3F4F6; }
}
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.crumb { color: #4B5563; }
.sep { color: #9CA3AF; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  &:hover { background: #F3F4F6; }
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #4F6BED;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
}
.username { font-size: 14px; color: #1F2937; }
</style>
