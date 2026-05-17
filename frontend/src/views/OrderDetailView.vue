<template>
  <section>
    <header class="page-header">
      <div>
        <h1>订单详情</h1>
        <p v-if="order">{{ formatTime(order.createdAt) }}</p>
      </div>
      <RouterLink class="button" to="/orders">返回列表</RouterLink>
    </header>

    <p v-if="message" :class="messageType">{{ message }}</p>

    <article v-if="order" class="detail-panel">
      <div class="detail-meta">
        <span>订单号：{{ order.orderId }}</span>
        <span>状态：{{ order.status }}</span>
        <span>金额：{{ Number(order.amount).toFixed(2) }}</span>
      </div>
      <div class="detail-meta" style="margin-top: 8px;">
        <span>需求：{{ order.reqTitle }}</span>
        <span>发布者：{{ order.publisherName }}</span>
        <span>接单者：{{ order.receiverName }}</span>
      </div>
      <p class="description">{{ order.reqDescription }}</p>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '../api/orders'

const route = useRoute()
const order = ref(null)
const message = ref('')
const messageType = ref('message')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

async function loadOrder() {
  try {
    order.value = await getOrderDetail(route.params.orderId)
  } catch (error) {
    messageType.value = 'message error'
    message.value = error.message
  }
}

onMounted(loadOrder)
</script>
