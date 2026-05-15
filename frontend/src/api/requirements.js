import { request } from './http'

export function listRequirements(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  return request(`/api/v1/requirements?${search.toString()}`)
}

export function getRequirement(reqId) {
  return request(`/api/v1/requirements/${reqId}`)
}
