import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
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
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'role',
        name: 'SysRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserCog' },
      },
      {
        path: 'menu',
        name: 'SysMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' },
      },
      {
        path: 'dept',
        name: 'SysDept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'Building2' },
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
        meta: { title: '项目管理', icon: 'FolderKanban' },
      },
      {
        path: 'contract',
        name: 'BusinessContract',
        component: () => import('@/views/business/contract/index.vue'),
        meta: { title: '合同管理', icon: 'FileText' },
      },
      {
        path: 'quote',
        name: 'BusinessQuote',
        component: () => import('@/views/business/quote/index.vue'),
        meta: { title: '报价管理', icon: 'FileSpreadsheet' },
      },
      {
        path: 'equipment',
        name: 'BusinessEquipment',
        component: () => import('@/views/business/equipment/index.vue'),
        meta: { title: '设备台账', icon: 'Server' },
      },
      {
        path: 'progress',
        name: 'BusinessProgress',
        component: () => import('@/views/business/progress/index.vue'),
        meta: { title: '进度管理', icon: 'ListTree' },
      },
      {
        path: 'acceptance',
        name: 'BusinessAcceptance',
        component: () => import('@/views/business/acceptance/index.vue'),
        meta: { title: '验收管理', icon: 'CheckSquare' },
      },
      {
        path: 'maintenance-point',
        name: 'BusinessMaintenancePoint',
        component: () => import('@/views/business/maintenance-point/index.vue'),
        meta: { title: '维护点位', icon: 'MapPin' },
      },
      {
        path: 'maintenance-contract',
        name: 'BusinessMaintenanceContract',
        component: () => import('@/views/business/maintenance-contract/index.vue'),
        meta: { title: '维保主合同', icon: 'FileSignature' },
      },
      {
        path: 'point-settlement',
        name: 'BusinessPointSettlement',
        component: () => import('@/views/business/point-settlement/index.vue'),
        meta: { title: '点位结算', icon: 'Receipt' },
      },
      {
        path: 'quarterly-settlement',
        name: 'BusinessQuarterlySettlement',
        component: () => import('@/views/business/quarterly-settlement/index.vue'),
        meta: { title: '季度结算', icon: 'CalendarClock' },
      },
      {
        path: 'maintenance-task',
        name: 'BusinessMaintenanceTask',
        component: () => import('@/views/business/maintenance-task/index.vue'),
        meta: { title: '维保任务', icon: 'Wrench' },
      },
      {
        path: 'maintenance-record',
        name: 'BusinessMaintenanceRecord',
        component: () => import('@/views/business/maintenance-record/index.vue'),
        meta: { title: '维保记录', icon: 'ClipboardList' },
      },
      {
        path: 'dashboard',
        name: 'BusinessDashboard',
        component: () => import('@/views/business/dashboard/index.vue'),
        meta: { title: '结算看板', icon: 'BarChart3' },
      },
      {
        path: 'attachment',
        name: 'BusinessAttachment',
        component: () => import('@/views/business/attachment/index.vue'),
        meta: { title: '附件管理', icon: 'Paperclip' },
      },
      {
        path: 'contract-payment',
        name: 'BusinessContractPayment',
        component: () => import('@/views/business/contract-payment/index.vue'),
        meta: { title: '合同收付款', icon: 'Banknote' },
      },
      {
        path: 'approval',
        name: 'BusinessApproval',
        component: () => import('@/views/business/approval/index.vue'),
        meta: { title: '审批中心', icon: 'ClipboardCheck' },
      },
      {
        path: 'approval/flow-config',
        name: 'BusinessApprovalFlowConfig',
        component: () => import('@/views/business/approval/flow-config.vue'),
        meta: { title: '审批流配置', icon: 'Workflow' },
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

// 全局前置守卫:无 token → /login;有 token 访问 /login → /
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    if (userStore.token) {
      next('/')
    } else {
      next()
    }
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }
  // 已登录但未加载菜单,加载一次
  if (userStore.menus.length === 0) {
    userStore.loadMenus().then(() => next()).catch(() => next())
    return
  }
  next()
})

export default router
