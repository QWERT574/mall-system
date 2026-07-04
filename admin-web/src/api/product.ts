import request from '@/utils/request'

export interface Product {
  id: number
  name: string
  price: number
  stock: number
  sales: number
  cover: string
  categoryId: number
  status: number
  createdAt: string
}

export interface ProductListParams {
  page: number
  pageSize: number
  keyword?: string
  categoryId?: number
}

export const getProductList = (params: ProductListParams) => {
  return request.get('/product/list', { params })
}

export const getProductDetail = (id: number) => {
  return request.get(`/product/${id}`)
}

export const createProduct = (data: Partial<Product>) => {
  return request.post('/product/create', data)
}

export const updateProduct = (id: number, data: Partial<Product>) => {
  return request.put(`/product/${id}`, data)
}

export const deleteProduct = (id: number) => {
  return request.delete(`/product/${id}`)
}

export const updateProductStock = (id: number, stock: number) => {
  return request.put(`/product/${id}/update-stock`, { stock })
}
