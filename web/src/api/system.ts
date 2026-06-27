import request from '@/utils/request'

// ===== 用户 =====
export interface SysUserDTO {
  id?: number
  deptId?: number
  username: string
  password?: string
  name: string
  phone?: string
  email?: string
  status?: number
}

export const userApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; deptId?: number }) =>
    request.get('/sys/user/page', { params }),
  create: (data: SysUserDTO) => request.post('/sys/user', data),
  update: (data: SysUserDTO) => request.put('/sys/user', data),
  delete: (id: number) => request.delete(`/sys/user/${id}`),
  assignRoles: (data: { userId: number; roleIds: number[] }) =>
    request.post('/sys/user/assignRoles', data),
  getRoleIds: (userId: number) => request.get(`/sys/user/roleIds/${userId}`),
}

// ===== 角色 =====
export interface SysRoleDTO {
  id?: number
  name: string
  code: string
  dataScope?: number
  sort?: number
  status?: number
  menuIds?: number[]
}

export const roleApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string }) =>
    request.get('/sys/role/page', { params }),
  list: () => request.get('/sys/role/list'),
  create: (data: SysRoleDTO) => request.post('/sys/role', data),
  update: (data: SysRoleDTO) => request.put('/sys/role', data),
  delete: (id: number) => request.delete(`/sys/role/${id}`),
  getMenuIds: (roleId: number) => request.get(`/sys/role/menuIds/${roleId}`),
}

// ===== 菜单 =====
export const menuApi = {
  list: () => request.get('/sys/menu/list'),
  tree: () => request.get('/sys/menu/tree'),
  userMenus: () => request.get('/sys/menu/userMenus'),
  create: (data: any) => request.post('/sys/menu', data),
  update: (data: any) => request.put('/sys/menu', data),
  delete: (id: number) => request.delete(`/sys/menu/${id}`),
}

// ===== 部门 =====
export const deptApi = {
  list: () => request.get('/sys/dept/list'),
  tree: () => request.get('/sys/dept/tree'),
  create: (data: any) => request.post('/sys/dept', data),
  update: (data: any) => request.put('/sys/dept', data),
  delete: (id: number) => request.delete(`/sys/dept/${id}`),
}

// ===== 消息通知 =====
export type NotificationType = 'SETTLEMENT' | 'APPROVAL' | 'OVERDUE' | 'WARRANTY' | 'ACCEPTANCE'

export interface SysNotification {
  id: string
  userId: string
  title: string
  content: string
  type: NotificationType
  businessType?: string
  businessId?: string
  isRead: number
  createTime?: string
}

export const notificationApi = {
  page: (params: { pageNum: number; pageSize: number; isRead?: number }) =>
    request.get('/sys/notification/page', { params }),
  unread: () => request.get('/sys/notification/unread'),
  unreadCount: () => request.get('/sys/notification/unread/count'),
  markRead: (id: string) => request.put(`/sys/notification/${id}/read`),
  markAllRead: () => request.put('/sys/notification/read-all'),
  delete: (id: string) => request.delete(`/sys/notification/${id}`),
}
