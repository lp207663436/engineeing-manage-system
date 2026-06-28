import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  login as loginApi,
  logout as logoutApi,
  getUserInfo,
  type LoginDTO,
} from '@/api/auth'
import { menuApi } from '@/api/system'

const STORAGE_KEYS = {
  token: 'ems_token',
  userId: 'ems_user_id',
  username: 'ems_username',
  name: 'ems_name',
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(STORAGE_KEYS.token) || '')
  // userId 由后端以 String 形式返回(后端 Long 全局序列化为 String),此处用 string 避免精度丢失
  const userId = ref<string | null>(localStorage.getItem(STORAGE_KEYS.userId))
  const username = ref(localStorage.getItem(STORAGE_KEYS.username) || '')
  const name = ref(localStorage.getItem(STORAGE_KEYS.name) || '')
  const menus = ref<any[]>([])

  function persistUserInfo(data: { userId?: string | null; username?: string; name?: string }) {
    if (data.userId != null) {
      userId.value = data.userId
      localStorage.setItem(STORAGE_KEYS.userId, data.userId)
    }
    if (data.username != null) {
      username.value = data.username
      localStorage.setItem(STORAGE_KEYS.username, data.username)
    }
    if (data.name != null) {
      name.value = data.name
      localStorage.setItem(STORAGE_KEYS.name, data.name)
    }
  }

  async function login(loginForm: LoginDTO) {
    const data = await loginApi(loginForm)
    token.value = data.token
    localStorage.setItem(STORAGE_KEYS.token, data.token)
    persistUserInfo({ userId: data.userId, username: data.username, name: data.name })
    return data
  }

  // 当 token 存在但 userId 为空时,调用后端接口拉取用户信息
  async function fetchUserInfo() {
    if (!token.value) return null
    try {
      const info = await getUserInfo()
      persistUserInfo({ userId: info.userId, username: info.username, name: info.name })
      return info
    } catch {
      return null
    }
  }

  async function loadMenus() {
    menus.value = await menuApi.userMenus()
    return menus.value
  }

  function clearStorage() {
    token.value = ''
    userId.value = null
    username.value = ''
    name.value = ''
    menus.value = []
    localStorage.removeItem(STORAGE_KEYS.token)
    localStorage.removeItem(STORAGE_KEYS.userId)
    localStorage.removeItem(STORAGE_KEYS.username)
    localStorage.removeItem(STORAGE_KEYS.name)
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {}
    clearStorage()
  }

  return { token, userId, username, name, menus, login, loadMenus, logout, fetchUserInfo, clearStorage }
})
