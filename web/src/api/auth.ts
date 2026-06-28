import request from '@/utils/request'

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: string
  username: string
  name: string
}

export interface UserInfoVO {
  userId: string
  username: string
  name: string
}

export function login(data: LoginDTO): Promise<LoginVO> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

// 获取当前登录用户信息(token 存在但 userId 为空时调用)
export function getUserInfo(): Promise<UserInfoVO> {
  return request.get('/sys/user/info')
}
