import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const Layout = () => import('@/layout/index.vue')

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { hidden: true },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Workbench',
        component: () => import('@/views/dashboard/workbench.vue'),
        meta: { title: '工作台', icon: 'Home' },
      },
    ],
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Settings' },
    children: [
      {
        path: 'user',
        name: 'SysUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'system:user:list' },
      },
      {
        path: 'role',
        name: 'SysRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserCog', permission: 'system:role:list' },
      },
      {
        path: 'menu',
        name: 'SysMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', permission: 'system:menu:list' },
      },
      {
        path: 'dept',
        name: 'SysDept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'Building2', permission: 'system:dept:list' },
      },
      {
        path: 'dict',
        name: 'SysDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '数据字典', icon: 'BookMarked', permission: 'system:dict:list' },
      },
    ],
  },
  {
    path: '/business',
    component: Layout,
    redirect: '/business/project',
    meta: { title: '业务管理', icon: 'Briefcase' },
    children: [
      {
        path: 'project',
        name: 'BusinessProject',
        component: () => import('@/views/business/project/index.vue'),
        meta: { title: '项目管理', icon: 'FolderKanban', permission: 'business:project:list' },
      },
      {
        path: 'contract',
        name: 'BusinessContract',
        component: () => import('@/views/business/contract/index.vue'),
        meta: { title: '合同管理', icon: 'FileText', permission: 'business:contract:list' },
      },
      {
        path: 'quote',
        name: 'BusinessQuote',
        component: () => import('@/views/business/quote/index.vue'),
        meta: { title: '报价管理', icon: 'FileSpreadsheet', permission: 'business:quote:list' },
      },
      {
        path: 'equipment',
        name: 'BusinessEquipment',
        component: () => import('@/views/business/equipment/index.vue'),
        meta: { title: '设备台账', icon: 'Server', permission: 'business:equipment:list' },
      },
      {
        path: 'progress',
        name: 'BusinessProgress',
        component: () => import('@/views/business/progress/index.vue'),
        meta: { title: '进度管理', icon: 'ListTree', permission: 'business:progress:list' },
      },
      {
        path: 'acceptance',
        name: 'BusinessAcceptance',
        component: () => import('@/views/business/acceptance/index.vue'),
        meta: { title: '验收管理', icon: 'CheckSquare', permission: 'business:acceptance:list' },
      },
      {
        path: 'maintenance-point',
        name: 'BusinessMaintenancePoint',
        component: () => import('@/views/business/maintenance-point/index.vue'),
        meta: { title: '维护点位', icon: 'MapPin', permission: 'business:maintenance-point:list' },
      },
      {
        path: 'maintenance-contract',
        name: 'BusinessMaintenanceContract',
        component: () => import('@/views/business/maintenance-contract/index.vue'),
        meta: { title: '维保主合同', icon: 'FileSignature', permission: 'business:maintenance-contract:list' },
      },
      {
        path: 'point-settlement',
        name: 'BusinessPointSettlement',
        component: () => import('@/views/business/point-settlement/index.vue'),
        meta: { title: '点位结算', icon: 'Receipt', permission: 'business:point-settlement:list' },
      },
      {
        path: 'quarterly-settlement',
        name: 'BusinessQuarterlySettlement',
        component: () => import('@/views/business/quarterly-settlement/index.vue'),
        meta: { title: '季度结算', icon: 'CalendarClock', permission: 'business:quarterly-settlement:list' },
      },
      {
        path: 'maintenance-task',
        name: 'BusinessMaintenanceTask',
        component: () => import('@/views/business/maintenance-task/index.vue'),
        meta: { title: '维保任务', icon: 'Wrench', permission: 'business:maintenance-task:list' },
      },
      {
        path: 'maintenance-record',
        name: 'BusinessMaintenanceRecord',
        component: () => import('@/views/business/maintenance-record/index.vue'),
        meta: { title: '维保记录', icon: 'ClipboardList', permission: 'business:maintenance-record:list' },
      },
      {
        path: 'dashboard',
        name: 'BusinessDashboard',
        component: () => import('@/views/business/dashboard/index.vue'),
        meta: { title: '结算看板', icon: 'BarChart3', permission: 'business:dashboard:list' },
      },
      {
        path: 'attachment',
        name: 'BusinessAttachment',
        component: () => import('@/views/business/attachment/index.vue'),
        meta: { title: '附件管理', icon: 'Paperclip', permission: 'business:attachment:list' },
      },
      {
        path: 'contract-payment',
        name: 'BusinessContractPayment',
        component: () => import('@/views/business/contract-payment/index.vue'),
        meta: { title: '合同收付款', icon: 'Banknote', permission: 'business:contract-payment:list' },
      },
      {
        path: 'approval',
        name: 'BusinessApproval',
        component: () => import('@/views/business/approval/index.vue'),
        meta: { title: '审批中心', icon: 'ClipboardCheck', permission: 'business:approval:list' },
      },
      {
        path: 'approval/flow-config',
        name: 'BusinessApprovalFlowConfig',
        component: () => import('@/views/business/approval/flow-config.vue'),
        meta: { title: '审批流配置', icon: 'Workflow', permission: 'business:approval:flow-config' },
      },
      {
        path: 'report',
        name: 'BusinessReport',
        component: () => import('@/views/business/report/index.vue'),
        meta: { title: '报表中心', icon: 'FileSpreadsheet', permission: 'business:report:list' },
      },
      {
        path: 'maintenance-stat',
        name: 'BusinessMaintenanceStat',
        component: () => import('@/views/business/maintenance-stat/index.vue'),
        meta: { title: '维保统计', icon: 'BarChart3', permission: 'business:maintenance-stat:list' },
      },
      {
        path: 'customer',
        name: 'BusinessCustomer',
        component: () => import('@/views/business/customer/index.vue'),
        meta: { title: '客户档案', icon: 'Users', permission: 'business:customer:list' },
      },
      {
        path: 'supplier',
        name: 'BusinessSupplier',
        component: () => import('@/views/business/supplier/index.vue'),
        meta: { title: '供应商档案', icon: 'Truck', permission: 'business:supplier:list' },
      },
      {
        path: 'contract-change',
        name: 'BusinessContractChange',
        component: () => import('@/views/business/contract-change/index.vue'),
        meta: { title: '合同变更', icon: 'FilePenLine', permission: 'business:contract-change:list' },
      },
      {
        path: 'quote/version-compare',
        name: 'BusinessQuoteVersionCompare',
        component: () => import('@/views/business/quote/version-compare.vue'),
        meta: { title: '报价版本对比', icon: 'GitCompare', permission: 'business:quote:list' },
      },
      {
        path: 'progress/gantt',
        name: 'BusinessProgressGantt',
        component: () => import('@/views/business/progress/gantt.vue'),
        meta: { title: '进度甘特图', icon: 'CalendarRange', permission: 'business:progress:list' },
      },
      {
        path: 'progress/dashboard',
        name: 'BusinessProgressDashboard',
        component: () => import('@/views/business/progress/dashboard.vue'),
        meta: { title: '进度看板', icon: 'KanbanSquare', permission: 'business:progress:list' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
})

// 从用户菜单树中递归收集所有权限标识
function collectPermissions(menus: any[]): Set<string> {
  const perms = new Set<string>()
  const walk = (list: any[]) => {
    list.forEach((m) => {
      if (m.permission) perms.add(m.permission)
      if (Array.isArray(m.children) && m.children.length > 0) walk(m.children)
    })
  }
  walk(menus)
  return perms
}

// 全局前置守卫:无 token → /login;有 token 访问 /login → /;
// token 存在但用户信息缺失时自动拉取;路由 meta.permission 校验权限
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login') {
    if (userStore.token) next('/')
    else next()
    return
  }
  // 403/404 等错误页直接放行
  if (to.path === '/403') {
    next()
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }

  // token 存在但 userId 为空,自动拉取用户信息
  if (!userStore.userId) {
    const info = await userStore.fetchUserInfo()
    if (!info) {
      // 拉取失败(token 失效等),回到登录页
      userStore.clearStorage()
      next('/login')
      return
    }
  }

  // 已登录但未加载菜单,加载一次
  if (userStore.menus.length === 0) {
    try {
      await userStore.loadMenus()
    } catch {
      next()
      return
    }
  }

  // 权限校验:路由 meta.permission 必须命中用户权限集合
  const requiredPerm = to.meta?.permission as string | undefined
  if (requiredPerm) {
    const perms = collectPermissions(userStore.menus)
    if (!perms.has(requiredPerm)) {
      ElMessage.warning('您没有权限访问该页面')
      next('/403')
      return
    }
  }

  next()
})

export default router
