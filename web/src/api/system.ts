import request from '@/utils/request'

// ===== 用户 =====
export interface SysUserDTO {
  id?: string
  deptId?: string
  username: string
  password?: string
  name: string
  phone?: string
  email?: string
  status?: number
}

export const userApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; deptId?: string }) =>
    request.get('/sys/user/page', { params }),
  create: (data: SysUserDTO) => request.post('/sys/user', data),
  update: (data: SysUserDTO) => request.put('/sys/user', data),
  delete: (id: string) => request.delete(`/sys/user/${id}`),
  assignRoles: (data: { userId: string; roleIds: string[] }) =>
    request.post('/sys/user/assignRoles', data),
  getRoleIds: (userId: string) => request.get(`/sys/user/roleIds/${userId}`),
}

// ===== 角色 =====
export interface SysRoleDTO {
  id?: string
  name: string
  code: string
  dataScope?: number
  sort?: number
  status?: number
  menuIds?: string[]
}

export const roleApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string }) =>
    request.get('/sys/role/page', { params }),
  list: () => request.get('/sys/role/list'),
  create: (data: SysRoleDTO) => request.post('/sys/role', data),
  update: (data: SysRoleDTO) => request.put('/sys/role', data),
  delete: (id: string) => request.delete(`/sys/role/${id}`),
  getMenuIds: (roleId: string) => request.get(`/sys/role/menuIds/${roleId}`),
}

// ===== 菜单 =====
export const menuApi = {
  list: () => request.get('/sys/menu/list'),
  tree: () => request.get('/sys/menu/tree'),
  userMenus: () => request.get('/sys/menu/userMenus'),
  create: (data: any) => request.post('/sys/menu', data),
  update: (data: any) => request.put('/sys/menu', data),
  delete: (id: string) => request.delete(`/sys/menu/${id}`),
}

// ===== 部门 =====
export const deptApi = {
  list: () => request.get('/sys/dept/list'),
  tree: () => request.get('/sys/dept/tree'),
  create: (data: any) => request.post('/sys/dept', data),
  update: (data: any) => request.put('/sys/dept', data),
  delete: (id: string) => request.delete(`/sys/dept/${id}`),
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

// ===== 数据字典 =====
export interface SysDictDTO {
  id?: string
  code: string
  name: string
  remark?: string
}

export interface SysDictItemDTO {
  id?: string
  dictId: string
  label: string
  value: string
  sort?: number
}

export const dictApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; code?: string }) =>
    request.get('/system/dict/page', { params }),
  list: () => request.get('/system/dict/list'),
  create: (data: SysDictDTO) => request.post('/system/dict', data),
  update: (data: SysDictDTO) => request.put('/system/dict', data),
  delete: (id: string) => request.delete(`/system/dict/${id}`),
  itemsByCode: (code: string) => request.get(`/system/dict/itemsByCode`, { params: { code } }),
  itemsByDictId: (dictId: string) => request.get(`/system/dict/itemsByDictId`, { params: { dictId } }),
  itemCreate: (data: SysDictItemDTO) => request.post('/system/dict/item', data),
  itemUpdate: (data: SysDictItemDTO) => request.put('/system/dict/item', data),
  itemDelete: (id: string) => request.delete(`/system/dict/item/${id}`),
}
