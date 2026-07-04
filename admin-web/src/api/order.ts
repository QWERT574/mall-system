import request from '@/utils/request'

export interface Order {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  status: number
  paymentMethod?: number
  payStatus: number
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  logisticsCompany?: string
  trackingNumber?: string
  shippedAt?: string
  expectedDeliveryAt?: string
  remark?: string
  createdAt: string
  items?: OrderItem[]
  buyer?: BuyerInfo
}

export interface BuyerInfo {
  id: number
  username: string
  nickname: string
  phone: string
  avatar?: string
  userType: number
}

export interface SellerInfo {
  id: number
  username: string
  nickname: string
  phone: string
  shopName?: string
  userType: number
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  imageUrl: string
  specName?: string
  price: number
  quantity: number
  seller?: SellerInfo
}

export interface OrderListParams {
  page: number
  pageSize: number
  status?: number | null
  orderNo?: string
  keyword?: string
  startDate?: string
  endDate?: string
}

export interface ShipOrderParams {
  orderId: number
  logisticsCompany: string
  trackingNumber: string
}

export const getOrderList = (params: OrderListParams) => {
  return request.get('/order/admin/list', { params })
}

export const getOrderDetail = (id: number) => {
  return request.get(`/order/detail/${id}`)
}

export const updateOrderStatus = (id: number, status: number) => {
  return request.put(`/order/${id}/status`, { status })
}

export const shipOrder = (data: ShipOrderParams) => {
  return request.post(`/order/ship/${data.orderId}`, data)
}

export const cancelOrder = (id: number) => {
  return request.put(`/order/${id}/cancel`)
}
