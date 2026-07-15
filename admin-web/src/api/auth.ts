import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

export interface LoginResponse {
  token: string
  user: any
}

export interface CaptchaResponse {
  key: string
  image: string  // data:image/png;base64,...
}

export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return request.post<LoginResponse>('/auth/login', data) as any
}

export const logout = (): Promise<void> => {
  return request.post('/auth/logout') as any
}

export const getUserInfo = (): Promise<any> => {
  return request.get('/auth/user') as any
}

/**
 * 获取图形验证码
 * @param key 已有的 captchaKey（刷新时传入同一个 key 即可）
 */
export const getCaptcha = (key?: string): Promise<CaptchaResponse> => {
  return request.get<CaptchaResponse>('/captcha/image', {
    params: key ? { key } : {}
  }) as any
}

