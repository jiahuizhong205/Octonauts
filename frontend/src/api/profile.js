import { request } from './http'

export function getProfile() {
  return request('/api/v1/users/me')
}

export function updateProfile(profile) {
  return request('/api/v1/users/me', {
    method: 'PUT',
    body: JSON.stringify(profile)
  })
}
