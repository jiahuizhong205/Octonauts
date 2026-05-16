import { getToken } from '../utils/auth'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export async function request(path, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })

  const payload = await response.json()
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}
