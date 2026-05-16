import { request } from './http'

export function login(data) {
  return request('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function register(data) {
  return request('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
