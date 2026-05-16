<template>
  <section>
    <header class="page-header">
      <div>
        <h1>注册</h1>
        <p>创建 CampusHub 账号，开始校园互助</p>
      </div>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <form class="form-grid" @submit.prevent="handleRegister">
      <label>
        用户名
        <input v-model="form.username" maxlength="64" required autofocus />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" maxlength="64" required />
      </label>
      <label>
        学号（选填）
        <input v-model="form.studentId" maxlength="32" />
      </label>
      <label>
        校区（选填）
        <input v-model="form.campus" maxlength="64" />
      </label>
      <div class="wide">
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </div>
    </form>

    <p style="margin-top: 16px; color: #607080;">
      已有账号？<RouterLink to="/login" style="color: #1664d9;">去登录</RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const form = reactive({
  username: '',
  password: '',
  studentId: '',
  campus: ''
})

async function handleRegister() {
  loading.value = true
  message.value = ''
  try {
    await register(form)
    router.push('/login')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
