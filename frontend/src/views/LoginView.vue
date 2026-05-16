<template>
  <section>
    <header class="page-header">
      <div>
        <h1>登录</h1>
        <p>登录后即可使用 CampusHub 全部功能</p>
      </div>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <form class="form-grid" @submit.prevent="handleLogin">
      <label>
        用户名
        <input v-model="form.username" maxlength="64" required autofocus />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" maxlength="64" required />
      </label>
      <div class="wide">
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </div>
    </form>

    <p style="margin-top: 16px; color: #607080;">
      还没有账号？<RouterLink to="/register" style="color: #1664d9;">去注册</RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { setToken, setUserId } from '../utils/auth'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  loading.value = true
  message.value = ''
  try {
    const data = await login(form)
    setToken(data.token)
    setUserId(data.userId)
    router.push('/profile')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
