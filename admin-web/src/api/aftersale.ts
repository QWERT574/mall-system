import request from '@/utils/request'

export interface AfterSale {
  id: number
  orderId: number
  userId: number
  productId: number
  serviceType: number
  reason: string
  images: string[]
  status: number
  serviceResult: string
  refundAmount?: number
  contactPhone?: string
  returnLogistics?: string
  returnLogisticsCompany?: string
  supplementaryEvidence?: string
  closeReason?: string
  processedBy?: number
  processedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface AfterSaleListParams {
  page: number
  pageSize: number
  status?: number | string
  serviceType?: number | string
}

export interface ProcessAfterSaleParams {
  afterSaleId: number
  result: number
  remark: string
  refundAmount?: number
}

export interface InterventionParams {
  afterSaleId?: number
  decision: number
  refundAmount: number
  penalty: string
  fineAmount: number
  reason: string
  evidenceImages: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const getAfterSaleList = (params: AfterSaleListParams): Promise<ApiResponse<{ records: AfterSale[]; total: number }>> => {
  return request.get('/aftersale/list', { params }) as Promise<ApiResponse<{ records: AfterSale[]; total: number }>>
}

export const getAfterSaleDetail = (id: number): Promise<ApiResponse<AfterSale>> => {
  return request.get(`/aftersale/${id}`) as Promise<ApiResponse<AfterSale>>
}

export const updateAfterSaleStatus = (id: number, status: number, serviceResult?: string): Promise<ApiResponse<void>> => {
  return request.put(`/aftersale/${id}`, { status, serviceResult }) as Promise<ApiResponse<void>>
}

export const processAfterSale = (data: { afterSaleId: number; result: number; remark: string; refundAmount?: number }): Promise<ApiResponse<any>> => {
  const status = data.result === 1 ? 2 : 3
  return request.post(`/aftersale/${data.afterSaleId}/process`, {
    status,
    serviceResult: data.remark,
    refundAmount: data.refundAmount || 0
  }) as Promise<ApiResponse<any>>
}

export const submitIntervention = (id: number, data: Omit<InterventionParams, 'afterSaleId'>): Promise<ApiResponse<void>> => {
  return request.post(`/admin/intervention/${id}/submit`, data) as Promise<ApiResponse<void>>
}

export const getServiceRecords = (id: number): Promise<ApiResponse<any[]>> => {
  return request.get(`/aftersale/${id}/records`) as Promise<ApiResponse<any[]>>
}
