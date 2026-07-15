import request from '@/utils/request'

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
}

export interface DashboardStats {
  totalProducts: number
  totalOrders: number
  totalUsers: number
  totalSales: number
  productTrend: number
  orderTrend: number
  userTrend: number
  salesTrend: number
}

export const getSystemConfigList = () => {
  return request.get('/system/config/list')
}

export const getSystemConfig = (key: string) => {
  return request.get(`/system/config/${key}`)
}

export const updateSystemConfig = (id: number, configValue: string) => {
  return request.put(`/system/config/${id}`, { configValue })
}

export const getAiConfig = () => {
  return request.get('/system/ai/config')
}

export const updateAiConfig = (apiKey: string, apiUrl: string, enabled: boolean) => {
  return request.put('/system/ai/config', { apiKey, apiUrl, enabled })
}

export const getDashboardStats = () => {
  return request.get('/system/dashboard/stats')
}

export interface DashboardChartData {
  salesTrend: Array<{
    date: string
    dayName: string
    sales: number
    orderCount: number
  }>
  orderStatusDist: {
    data: Array<{
      name: string
      value: number
      itemStyle: { color: string }
    }>
    total: number
  }
  categoryStats: Array<{
    categoryName: string
    sales: number
  }>
  topProducts: Array<{
    productId: number
    productName: string
    cover: string | null
    sales: number
    totalAmount: number
  }>
}

export const getDashboardChartData = () => {
  return request.get('/system/dashboard/chart')
}
