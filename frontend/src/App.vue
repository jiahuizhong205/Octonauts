<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">CampusHub</div>

      <template v-if="loggedIn">
        <RouterLink to="/requirements">需求大厅</RouterLink>
        <RouterLink to="/profile">个人资料</RouterLink>
        <RouterLink to="/notifications">消息通知</RouterLink>
        <a href="#" @click.prevent="handleLogout">退出</a>
      </template>
      <template v-else>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </template>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { isLoggedIn, clearToken } from './utils/auth'

const router = useRouter()
const loggedIn = computed(() => isLoggedIn())

function handleLogout() {
  clearToken()
  router.push('/login')
}
</script>
