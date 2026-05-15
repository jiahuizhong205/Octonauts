import { request } from './http'

export function listNotifications(unreadOnly = false) {
  return request(`/api/v1/notifications?unreadOnly=${unreadOnly}`)
}

export function getUnreadCount() {
  return request('/api/v1/notifications/unread-count')
}

export function markNotificationRead(notificationId) {
  return request(`/api/v1/notifications/${notificationId}/read`, {
    method: 'PATCH'
  })
}

export function markAllNotificationsRead() {
  return request('/api/v1/notifications/read-all', {
    method: 'PATCH'
  })
}
