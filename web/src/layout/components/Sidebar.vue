<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import * as LucideIcons from 'lucide-vue-next'

const props = defineProps<{ collapsed: boolean }>()

const route = useRoute()
const userStore = useUserStore()

// 合并静态路由的 system 分组 + 后端动态菜单
// 简化处理:直接用前端 staticRoutes 中带 meta.title 的顶层路由组
const menuGroups = computed(() => {
  // 从 router.options.routes 中取带 children 的非隐藏路由
  const allRoutes = [
    { path: '/dashboard', meta: { title: '首页', icon: 'Home' }, single: true },
    { path: '/business', meta: { title: '业务管理', icon: 'Briefcase' }, children: [
      { path: '/business/project', meta: { title: '项目管理', icon: 'FolderKanban' } },
      { path: '/business/contract', meta: { title: '合同管理', icon: 'FileText' } },
      { path: '/business/quote', meta: { title: '报价管理', icon: 'FileSpreadsheet' } },
      { path: '/business/equipment', meta: { title: '设备台账', icon: 'Server' } },
      { path: '/business/progress', meta: { title: '进度管理', icon: 'ListTree' } },
      { path: '/business/acceptance', meta: { title: '验收管理', icon: 'CheckSquare' } },
      { path: '/business/contract-payment', meta: { title: '合同收付款', icon: 'Money' } },
      { path: '/business/approval', meta: { title: '审批中心', icon: 'Stamp' } },
      { path: '/business/approval/flow-config', meta: { title: '审批流配置', icon: 'Settings' } },
    ]},
    { path: '/business-maintenance', meta: { title: '维护型项目', icon: 'Wrench' }, children: [
      { path: '/business/maintenance-point', meta: { title: '维护点位', icon: 'MapPin' } },
      { path: '/business/maintenance-contract', meta: { title: '维保主合同', icon: 'FileSignature' } },
      { path: '/business/point-settlement', meta: { title: '点位结算', icon: 'Receipt' } },
      { path: '/business/quarterly-settlement', meta: { title: '季度结算', icon: 'CalendarClock' } },
      { path: '/business/maintenance-task', meta: { title: '维保任务', icon: 'Wrench' } },
      { path: '/business/maintenance-record', meta: { title: '维保记录', icon: 'ClipboardList' } },
    ]},
    { path: '/business-dashboard', meta: { title: '运营看板', icon: 'BarChart3' }, children: [
      { path: '/business/dashboard', meta: { title: '结算看板', icon: 'BarChart3' } },
      { path: '/business/attachment', meta: { title: '附件管理', icon: 'Paperclip' } },
      { path: '/business/report', meta: { title: '报表中心', icon: 'FileSpreadsheet' } },
      { path: '/business/maintenance-stat', meta: { title: '维保统计', icon: 'BarChart3' } },
    ]},
    { path: '/system', meta: { title: '系统管理', icon: 'Settings' }, children: [
      { path: '/system/user', meta: { title: '用户管理', icon: 'User' } },
      { path: '/system/role', meta: { title: '角色管理', icon: 'UserCog' } },
      { path: '/system/menu', meta: { title: '菜单管理', icon: 'Menu' } },
      { path: '/system/dept', meta: { title: '部门管理', icon: 'Building2' } },
    ]},
  ]
  return allRoutes
})

function getIcon(name?: string) {
  if (!name) return null
  return (LucideIcons as any)[name] || null
}

const activePath = computed(() => route.path)
</script>

<template>
  <aside class="sidebar" :class="{ 'is-collapsed': props.collapsed }">
    <div class="sidebar-logo">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#4F6BED" stroke-width="2">
        <path d="M12 2L2 7l10 5 10-5-10-5z" />
        <path d="M2 17l10 5 10-5" />
        <path d="M2 12l10 5 10-5" />
      </svg>
      <span v-show="!props.collapsed" class="logo-text">EMS</span>
    </div>
    <nav class="sidebar-nav">
      <template v-for="item in menuGroups" :key="item.path">
        <!-- 单项(无 children) -->
        <router-link
          v-if="item.single"
          :to="item.path"
          class="nav-item"
          :class="{ 'is-active': activePath === item.path }"
        >
          <component :is="getIcon(item.meta?.icon)" v-if="getIcon(item.meta?.icon)" :size="18" />
          <span v-show="!props.collapsed" class="nav-text">{{ item.meta?.title }}</span>
        </router-link>
        <!-- 分组(有 children) -->
        <div v-else class="nav-group">
          <div v-show="!props.collapsed" class="nav-group-title">{{ item.meta?.title }}</div>
          <router-link
            v-for="child in item.children"
            :key="child.path"
            :to="child.path"
            class="nav-item"
            :class="{ 'is-active': activePath === child.path }"
          >
            <component :is="getIcon(child.meta?.icon)" v-if="getIcon(child.meta?.icon)" :size="18" />
            <span v-show="!props.collapsed" class="nav-text">{{ child.meta?.title }}</span>
          </router-link>
        </div>
      </template>
    </nav>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 240px;
  background: #FAFAFA;
  border-right: 1px solid #E5E7EB;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  z-index: 100;
  &.is-collapsed { width: 64px; }
}
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #F3F4F6;
  .logo-text { font-size: 16px; font-weight: 600; color: #1F2937; letter-spacing: -0.01em; }
}
.sidebar-nav { flex: 1; padding: 12px 8px; overflow-y: auto; }
.nav-group { margin-bottom: 8px; }
.nav-group-title {
  padding: 8px 12px 4px;
  font-size: 11px;
  color: #9CA3AF;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  color: #4B5563;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  &:hover { background: #F3F4F6; color: #1F2937; }
  &.is-active {
    background: rgba(79, 107, 237, 0.1);
    color: #4F6BED;
    font-weight: 500;
  }
  .nav-text { flex: 1; }
}
</style>
