import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, type LoginDTO } from '@/api/auth'
import { menuApi } from '@/api/system'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('ems_token') || '')
  const userId = ref<number | null>(null)
  const username = ref('')
  const name = ref('')
  const menus = ref<any[]>([])

  async function login(loginForm: LoginDTO) {
    const data = await loginApi(loginForm)
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    name.value = data.name
    localStorage.setItem('ems_token', data.token)
  }

  async function loadMenus() {
    menus.value = await menuApi.userMenus()
    return menus.value
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {}
    token.value = ''
    menus.value = []
    localStorage.removeItem('ems_token')
  }

  return { token, userId, username, name, menus, login, loadMenus, logout }
})
