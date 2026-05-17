import { request } from './http'

export function listOrders(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  return request(`/api/v1/orders?${search.toString()}`)
}

export function getOrderDetail(orderId) {
  return request(`/api/v1/orders/${orderId}`)
}
