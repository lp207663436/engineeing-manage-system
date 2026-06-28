import axios, { type InternalAxiosRequestConfig, type AxiosResponse, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// localStorage 中用户相关 key(与 stores/user.ts 保持一致)
const USER_STORAGE_KEYS = ['ems_token', 'ems_user_id', 'ems_username', 'ems_name']

function clearUserStorage() {
  USER_STORAGE_KEYS.forEach((k) => localStorage.removeItem(k))
}

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('ems_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    if (code === 401) {
      ElMessage.error(message || '未登录')
      clearUserStorage()
      // 使用 SPA 路由跳转,避免整页刷新丢失应用状态
      router.push('/login')
      return Promise.reject(new Error(message))
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error(error.response?.data?.message || '未登录')
      clearUserStorage()
      router.push('/login')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

// 拦截器已将 response.data 解包后返回,因此对外的请求方法类型应返回 Promise<T> 而非 Promise<AxiosResponse>。
// 通过类型断言声明解包后的方法签名,消除各处 "AxiosResponse 不可赋值" 的类型错误。
type Request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

export default service as unknown as Request
