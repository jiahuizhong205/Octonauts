<template>
  <section>
    <header class="page-header">
      <div>
        <h1>发布需求</h1>
        <p>填写需求信息，等待其他用户接单。</p>
      </div>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <form class="form-grid" @submit.prevent="handlePublish">
      <label>
        标题
        <input
          v-model="form.title"
          maxlength="50"
          placeholder="起个简洁明确的标题"
          required
        />
        <span class="hint">{{ form.title.length }}/50</span>
      </label>

      <label>
        分类
        <select v-model="form.type" required>
          <option value="" disabled>请选择需求分类</option>
          <option value="EXPRESS">快递跑腿</option>
          <option value="TUTORING">学习求助</option>
          <option value="SECOND_HAND">二手交易</option>
          <option value="STUDY_HELP">学业辅导</option>
          <option value="MATERIAL_SHARE">资料共享</option>
          <option value="TEAM_UP">组队招募</option>
          <option value="CARPOOL">拼车出行</option>
          <option value="Q_AND_A">问答求助</option>
          <option value="OTHER">其他</option>
        </select>
      </label>

      <label>
        预算 (元)
        <input
          v-model.number="form.budget"
          type="number"
          min="0"
          step="0.01"
          placeholder="0.00"
          required
        />
      </label>

      <label class="wide">
        描述
        <textarea
          v-model="form.description"
          maxlength="500"
          rows="5"
          placeholder="详细说明需求内容、地点、时间等信息"
          required
        ></textarea>
        <span class="hint">{{ form.description.length }}/500</span>
      </label>

      <div class="wide" style="display: flex; gap: 12px;">
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? '发布中...' : '发布需求' }}
        </button>
        <button type="button" @click="$router.push('/requirements')">取消</button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createRequirement } from '../api/requirements'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const messageType = ref('message')

const form = reactive({
  title: '',
  description: '',
  budget: null,
  type: ''
})

function validate() {
  if (form.title.trim().length < 5) {
    message.value = '标题至少需要 5 个字符'
    messageType.value = 'message error'
    return false
  }
  if (form.budget === null || form.budget === '') {
    message.value = '请填写预算金额'
    messageType.value = 'message error'
    return false
  }
  if (Number(form.budget) < 0) {
    message.value = '预算金额必须大于等于 0'
    messageType.value = 'message error'
    return false
  }
  if (!form.type) {
    message.value = '请选择需求分类'
    messageType.value = 'message error'
    return false
  }
  return true
}

async function handlePublish() {
  message.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const data = await createRequirement({
      title: form.title.trim(),
      description: form.description.trim(),
      budget: Number(form.budget),
      type: form.type
    })
    messageType.value = 'message success'
    message.value = `发布成功！需求编号：${data.reqId}`
    setTimeout(() => router.push(`/requirements/${data.reqId}`), 1500)
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #607080;
}
.success {
  color: #1a9c5e;
}
.error {
  color: #c0392b;
}
</style>
