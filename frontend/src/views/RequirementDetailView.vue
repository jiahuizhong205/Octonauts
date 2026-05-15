<template>
  <section>
    <header class="page-header">
      <div>
        <h1>{{ requirement?.title || '需求详情' }}</h1>
        <p v-if="requirement">{{ requirement.publisherName || '匿名用户' }} · {{ formatTime(requirement.createdAt) }}</p>
      </div>
      <RouterLink class="button" to="/requirements">返回列表</RouterLink>
    </header>

    <p v-if="message" class="message">{{ message }}</p>

    <article v-if="requirement" class="detail-panel">
      <div class="detail-meta">
        <span>分类：{{ requirement.type }}</span>
        <span>状态：{{ requirement.status }}</span>
        <span>预算：{{ Number(requirement.budget).toFixed(2) }}</span>
      </div>
      <p class="description">{{ requirement.description }}</p>

      <button class="primary" :disabled="!requirement.acceptable">
        {{ requirement.acceptable ? '可接单' : '当前不可接单' }}
      </button>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getRequirement } from '../api/requirements'

const route = useRoute()
const requirement = ref(null)
const message = ref('')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

async function loadRequirement() {
  try {
    requirement.value = await getRequirement(route.params.reqId)
  } catch (error) {
    message.value = error.message
  }
}

onMounted(loadRequirement)
</script>
