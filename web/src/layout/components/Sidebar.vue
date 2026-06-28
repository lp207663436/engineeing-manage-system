<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import * as LucideIcons from 'lucide-vue-next'
import { ChevronDown } from 'lucide-vue-next'

const props = defineProps<{ collapsed: boolean; isMobile?: boolean; mobileOpen?: boolean }>()

const route = useRoute()
const userStore = useUserStore()

// 菜单类型:1=目录,2=菜单,3=按钮(不在侧边栏展示)
const MENU_TYPE_DIR = 1
const MENU_TYPE_MENU = 2

// 仅展示目录(1)与菜单(2),过滤掉按钮(3)与禁用项
const visibleMenus = computed<any[]>(() => {
  const filter = (list: any[]): any[] =>
    (list || [])
      .filter((m) => (m.type === MENU_TYPE_DIR || m.type === MENU_TYPE_MENU) && m.status !== 0)
      .map((m) => ({ ...m, children: m.children ? filter(m.children) : [] }))
  return filter(userStore.menus)
})

// 展开状态:记录已展开的目录 id
const expandedKeys = ref<Set<string | number>>(new Set())

function toggleExpand(menu: any) {
  const key = menu.id ?? menu.path
  if (expandedKeys.value.has(key)) expandedKeys.value.delete(key)
  else expandedKeys.value.add(key)
  // 触发响应式更新
  expandedKeys.value = new Set(expandedKeys.value)
}

function isExpanded(menu: any) {
  return expandedKeys.value.has(menu.id ?? menu.path)
}

// 当前激活路径包含判断(用于自动展开父级)
const activePath = computed(() => route.path)

function isActive(menu: any) {
  return activePath.value === normalizePath(menu.path)
}

function normalizePath(p?: string) {
  if (!p) return ''
  return p.startsWith('/') ? p : `/${p}`
}

// 菜单是否需要展开图标(目录且有可见子项)
function hasVisibleChildren(menu: any) {
  return Array.isArray(menu.children) && menu.children.length > 0
}

function getIcon(name?: string) {
  if (!name) return null
  return (LucideIcons as any)[name] || null
}

// 路由切换或菜单加载时,自动展开当前激活菜单的父级目录
function autoExpandActive(menus: any[], parents: any[] = []) {
  for (const m of menus) {
    if (m.type === MENU_TYPE_MENU && isActive(m)) {
      parents.forEach((p) => expandedKeys.value.add(p.id ?? p.path))
      return true
    }
    if (m.children?.length) {
      if (autoExpandActive(m.children, [...parents, m])) return true
    }
  }
  return false
}

watch(
  () => [visibleMenus.value, activePath.value],
  () => {
    if (visibleMenus.value.length) {
      autoExpandActive(visibleMenus.value)
      expandedKeys.value = new Set(expandedKeys.value)
    }
  },
  { immediate: true }
)
</script>

<template>
  <aside class="sidebar" :class="{ 'is-collapsed': props.collapsed, 'mobile-open': props.isMobile && props.mobileOpen }">
    <div class="sidebar-logo">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#4F6BED" stroke-width="2">
        <path d="M12 2L2 7l10 5 10-5-10-5z" />
        <path d="M2 17l10 5 10-5" />
        <path d="M2 12l10 5 10-5" />
      </svg>
      <span v-show="!props.collapsed" class="logo-text">EMS</span>
    </div>
    <nav class="sidebar-nav">
      <template v-for="menu in visibleMenus" :key="menu.id ?? menu.path">
        <!-- 目录(type=1):可展开子菜单 -->
        <div v-if="menu.type === MENU_TYPE_DIR && hasVisibleChildren(menu)" class="nav-group">
          <div
            class="nav-group-title"
            :class="{ 'is-collapsed-title': props.collapsed }"
            @click="props.collapsed ? null : toggleExpand(menu)"
          >
            <component :is="getIcon(menu.icon)" v-if="getIcon(menu.icon)" :size="18" class="nav-group-icon" />
            <span v-show="!props.collapsed" class="nav-text">{{ menu.name }}</span>
            <ChevronDown
              v-show="!props.collapsed"
              :size="14"
              class="nav-arrow"
              :class="{ 'is-open': isExpanded(menu) }"
            />
          </div>
          <div v-show="!props.collapsed && isExpanded(menu)" class="nav-group-children">
            <template v-for="child in menu.children" :key="child.id ?? child.path">
              <!-- 嵌套目录(暂按平铺处理:仅渲染叶子菜单) -->
              <router-link
                v-if="child.type === MENU_TYPE_MENU"
                :to="normalizePath(child.path)"
                class="nav-item nav-item-child"
                :class="{ 'is-active': isActive(child) }"
              >
                <component :is="getIcon(child.icon)" v-if="getIcon(child.icon)" :size="16" />
                <span class="nav-text">{{ child.name }}</span>
              </router-link>
            </template>
          </div>
        </div>
        <!-- 菜单(type=2):直接路由链接 -->
        <router-link
          v-else-if="menu.type === MENU_TYPE_MENU"
          :to="normalizePath(menu.path)"
          class="nav-item"
          :class="{ 'is-active': isActive(menu) }"
        >
          <component :is="getIcon(menu.icon)" v-if="getIcon(menu.icon)" :size="18" />
          <span v-show="!props.collapsed" class="nav-text">{{ menu.name }}</span>
        </router-link>
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
  transition: width 0.2s ease, transform 0.3s ease;
  z-index: 100;
  &.is-collapsed { width: 64px; }
}
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    &.mobile-open { transform: translateX(0); }
  }
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
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  color: #4B5563;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  .nav-group-icon { flex-shrink: 0; }
  &:hover { background: #F3F4F6; color: #1F2937; }
  &.is-collapsed-title { justify-content: center; }
  .nav-text { flex: 1; font-weight: 500; }
  .nav-arrow { transition: transform 0.2s ease; flex-shrink: 0; &.is-open { transform: rotate(180deg); } }
}
.nav-group-children { padding-left: 12px; }
.nav-group-title:not(.is-collapsed-title) {
  font-size: 11px;
  color: #9CA3AF;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
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
.nav-item-child { padding-left: 14px; font-size: 13px; }
</style>
