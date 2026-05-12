<template>
  <section>
    <header class="page-header">
      <div>
        <h1>个人资料</h1>
        <p>维护校区、专业和联系方式展示偏好。</p>
      </div>
      <button class="primary" @click="saveProfile" :disabled="saving">
        {{ saving ? '保存中' : '保存' }}
      </button>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <form class="form-grid" @submit.prevent="saveProfile">
      <label>
        用户名
        <input v-model="form.username" disabled />
      </label>
      <label>
        昵称
        <input v-model="form.nickname" maxlength="64" />
      </label>
      <label>
        学号
        <input v-model="form.studentId" disabled />
      </label>
      <label>
        校区
        <input v-model="form.campus" maxlength="64" />
      </label>
      <label>
        学院
        <input v-model="form.college" maxlength="64" />
      </label>
      <label>
        专业
        <input v-model="form.major" maxlength="64" />
      </label>
      <label>
        年级
        <input v-model="form.grade" maxlength="32" />
      </label>
      <label>
        信用分
        <input v-model="form.creditScore" disabled />
      </label>
      <label class="wide">
        自我介绍
        <textarea v-model="form.bio" maxlength="255" rows="4" />
      </label>
      <label class="checkbox-row wide">
        <input v-model="form.contactVisible" type="checkbox" />
        允许在需求协作场景中展示联系方式
      </label>
    </form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile } from '../api/profile'

const saving = ref(false)
const message = ref('')
const form = reactive({
  username: '',
  nickname: '',
  studentId: '',
  campus: '',
  college: '',
  major: '',
  grade: '',
  bio: '',
  contactVisible: false,
  creditScore: 100
})

function fillForm(profile) {
  Object.assign(form, profile)
}

async function loadProfile() {
  try {
    fillForm(await getProfile())
  } catch (error) {
    message.value = error.message
  }
}

async function saveProfile() {
  saving.value = true
  message.value = ''
  try {
    // 只提交允许修改的字段，避免误把 userId、creditScore 等服务端字段传回去。
    const saved = await updateProfile({
      nickname: form.nickname,
      campus: form.campus,
      college: form.college,
      major: form.major,
      grade: form.grade,
      bio: form.bio,
      contactVisible: form.contactVisible
    })
    fillForm(saved)
    message.value = '个人资料已更新'
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
