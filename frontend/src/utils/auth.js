const TOKEN_KEY = 'campushub_token'
const USER_ID_KEY = 'campushub_user_id'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
}

export function getUserId() {
  const id = localStorage.getItem(USER_ID_KEY)
  return id ? Number(id) : null
}

export function setUserId(userId) {
  localStorage.setItem(USER_ID_KEY, String(userId))
}

export function isLoggedIn() {
  return !!getToken()
}
