import request from '@/utils/request'

export interface InterventionRecord {
  id: number
  userId: number
  sellerId: number
  orderId: number
  productId: number
  sessionId: number
  issueType: string
  title: string
  description: string
  evidenceImages: string
  status: number
  adminId: number
  adminRemark: string
  result: string
  createdAt: string
  updatedAt: string
  processedAt: string
  buyerName?: string
  sellerName?: string
  sellerShopName?: string
  productName?: string
  orderNo?: string
  orderAmount?: number
  afterSaleId?: number
  serviceType?: number
  amount?: number
  interventionAt?: string
}

export type Intervention = InterventionRecord

export interface InterventionListParams {
  page: number
  size?: number
  pageSize?: number
  status?: number | string
  issueType?: string
}

export function getInterventionList(params: InterventionListParams) {
  return request.get('/admin/intervention', { params }) as Promise<any>
}

export function getInterventionDetail(id: number) {
  return request.get(`/admin/intervention/${id}`) as Promise<any>
}

export function createIntervention(data: Partial<InterventionRecord>) {
  return request.post('/admin/intervention', data)
}

export function processIntervention(
  id: number,
  data: { status: number; remark: string; adminId: number }
) {
  return request.post(`/admin/intervention/${id}/process`, data)
}

export function updateIntervention(id: number, data: Partial<InterventionRecord>) {
  return request.put(`/admin/intervention/${id}`, data)
}

export function deleteIntervention(id: number) {
  return request.delete(`/admin/intervention/${id}`)
}

export function getPendingInterventions(params: { page: number; size: number }) {
  return request.get('/admin/intervention/pending', { params })
}

export function assignAdmin(id: number, adminId: number) {
  return request.post(`/admin/intervention/${id}/assign`, { adminId })
}

export function getInterventionStats() {
  return request.post('/admin/intervention/stats') as Promise<any>
}

export function getInterventionsByUserId(userId: number) {
  return request.get(`/admin/intervention/user/${userId}`)
}

export function getInterventionsBySellerId(sellerId: number) {
  return request.get(`/admin/intervention/seller/${sellerId}`)
}
