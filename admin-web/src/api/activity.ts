import request from '@/utils/request'

export interface Activity {
  id: number
  name: string
  description: string
  startTime: string
  endTime: string
  activityType: number
  location: string
  maxParticipants: number
  currentParticipants: number
  status: number
  coverImage: string
  images?: string
  isRecommended?: number
  recommendOrder?: number
  createdAt: string
}

export interface ActivityListParams {
  page: number
  pageSize: number
  status?: number
  activityType?: number
}

export const getActivityList = (params: ActivityListParams) => {
  return request.get('/activity/list', { params })
}

export const getActivityDetail = (id: number) => {
  return request.get(`/activity/${id}`)
}

export const createActivity = (data: Partial<Activity>) => {
  return request.post('/activity/create', data)
}

export const updateActivity = (id: number, data: Partial<Activity>) => {
  return request.post(`/activity/${id}/update`, data)
}

export const deleteActivity = (id: number) => {
  return request.delete(`/activity/${id}`)
}

export const getActivityParticipants = (id: number) => {
  return request.get(`/activity/${id}/participants`)
}

export const approveParticipant = (id: number, status: number) => {
  return request.put(`/activity/participant/${id}/approve`, { status })
}
