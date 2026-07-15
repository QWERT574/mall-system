import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    const userInfo = localStorage.getItem('admin_user')
    if (userInfo && userInfo !== 'undefined') {
      try {
        const user = JSON.parse(userInfo)
        if (user && user.userType !== undefined) {
          config.headers['User-Type'] = user.userType
        }
      } catch (e) {
        localStorage.removeItem('admin_user')
      }
    }

    // 过滤掉 params 中值为 null/undefined/空字符串 的字段
    // 避免后端类型转换失败 (如 categoryId=null 被当成字符串 "null")
    if (config.params) {
      const cleaned: Record<string, any> = {}
      Object.keys(config.params).forEach((key) => {
        const v = config.params[key]
        if (v !== null && v !== undefined && v !== '') {
          cleaned[key] = v
        }
      })
      config.params = cleaned
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data as { code: number; message: string; data: any }

    if (code === 0 || code === 200) {
      return data
    } else {
      if (response.status === 401) {
        console.log('未授权，请重新登录')
        localStorage.removeItem('admin_token')
        window.location.href = '/login'
        return Promise.reject(new Error('未授权'))
      }
      
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
  },
  (error: any) => {
    if (error?.response?.status === 401) {
      console.log('Token已过期，跳转登录页')
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
      return Promise.reject(new Error('未授权'))
    }
    
    const msg = error?.message || '网络错误，请检查连接'
    if (!window.location.pathname.includes('/login')) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default service
