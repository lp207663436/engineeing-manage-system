import request from '@/utils/request'

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: number
  username: string
  name: string
}

export function login(data: LoginDTO): Promise<LoginVO> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}
