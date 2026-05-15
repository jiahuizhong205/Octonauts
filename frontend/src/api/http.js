const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const DEMO_USER_ID = import.meta.env.VITE_DEMO_USER_ID || '10001'

export async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // 当前庞彬负责模块先用演示用户联调，登录模块完成后改为真实 token。
      'X-User-Id': DEMO_USER_ID,
      ...(options.headers || {})
    }
  })

  const payload = await response.json()
  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}
