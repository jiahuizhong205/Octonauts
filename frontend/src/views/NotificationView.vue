<template>
  <section>
    <header class="page-header">
      <div>
        <h1>消息通知</h1>
        <p>{{ unreadCount }} 条未读通知</p>
      </div>
      <div class="header-actions">
        <label class="inline-check">
          <input v-model="unreadOnly" type="checkbox" @change="loadNotifications" />
          只看未读
        </label>
        <button class="primary" @click="readAll">全部已读</button>
      </div>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="list">
      <article
        v-for="item in notifications"
        :key="item.notificationId"
        :class="['notification-row', { unread: !item.read }]"
      >
        <div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.content }}</p>
          <small>{{ item.eventType }} · {{ formatTime(item.createdAt) }}</small>
        </div>
        <button v-if="!item.read" @click="readOne(item.notificationId)">标为已读</button>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead
} from '../api/notifications'

const notifications = ref([])
const unreadCount = ref(0)
const unreadOnly = ref(false)
const message = ref('')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

async function refreshUnreadCount() {
  const data = await getUnreadCount()
  unreadCount.value = data.unreadCount
}

async function loadNotifications() {
  message.value = ''
  try {
    notifications.value = await listNotifications(unreadOnly.value)
    await refreshUnreadCount()
    if (notifications.value.length === 0) {
      message.value = '暂无通知'
    }
  } catch (error) {
    message.value = error.message
  }
}

async function readOne(notificationId) {
  await markNotificationRead(notificationId)
  await loadNotifications()
}

async function readAll() {
  await markAllNotificationsRead()
  await loadNotifications()
}

onMounted(loadNotifications)
</script>
