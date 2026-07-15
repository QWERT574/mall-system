import request from '@/utils/request'

export interface User {
  id: number
  openid: string
  phone: string
  nickname: string
  avatar: string
  userType: number
  isVerified: number
  status: number
  createdAt: string
}

export interface UserListParams {
  page: number
  pageSize: number
  keyword?: string
  userType?: number | null
  status?: number | null
}

export const getUserList = (params: UserListParams) => {
  return request.get('/user/list', { params })
}

export const getUserDetail = (id: number) => {
  return request.get(`/user/${id}`)
}

export const updateUserStatus = (id: number, status: number) => {
  return request.put(`/user/${id}/status`, { status })
}

export const verifySupplier = (id: number, status: number, rejectReason?: string) => {
  return request.put(`/user/${id}/verify`, { status, rejectReason })
}

export const deactivateUser = (id: number) => {
  return request.post(`/user/${id}/deactivate`)
}
