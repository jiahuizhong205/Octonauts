<template>
  <section>
    <header class="page-header">
      <div>
        <h1>我的订单</h1>
        <p>查看我发布的和我接取的订单历史。</p>
      </div>
      <button @click="loadOrders">刷新</button>
    </header>

    <div class="toolbar">
      <button :class="{ primary: tab === 'received' }" @click="switchTab('received')">我接取的</button>
      <button :class="{ primary: tab === 'published' }" @click="switchTab('published')">我发布的</button>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="list">
      <RouterLink
        v-for="item in orders"
        :key="item.reqId + (item.orderId || '')"
        class="requirement-row"
        :to="item.orderId ? `/orders/${item.orderId}` : `/requirements/${item.reqId}`"
      >
        <div>
          <h2>{{ item.reqTitle }}</h2>
          <p>
            <template v-if="item.receiverName">接单者 {{ item.receiverName }}</template>
            <template v-else>暂无接单</template>
            · {{ formatTime(item.createdAt) }}
          </p>
        </div>
        <div class="row-meta">
          <strong>{{ Number(item.amount).toFixed(2) }}</strong>
          <span :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</span>
        </div>
      </RouterLink>
    </div>

    <div class="pagination" v-if="total > 0">
      <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ total }} 条</span>
      <button :disabled="page * pageSize >= total" @click="changePage(page + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOrders } from '../api/orders'

const tab = ref('received')
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const message = ref('')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

function statusLabel(s) {
  const labels = {
    PENDING: '待接单',
    IN_PROGRESS: '进行中',
    TO_CONFIRM: '待确认',
    COMPLETED: '已完成',
    CANCELED: '已取消',
    ACCEPTED: '已接单'
  }
  return labels[s] || s
}

async function loadOrders() {
  message.value = ''
  try {
    const data = await listOrders({ tab: tab.value, page: page.value, pageSize: pageSize.value })
    orders.value = data.list
    total.value = data.total
    if (data.list.length === 0) {
      message.value = '暂无相关订单'
    }
  } catch (error) {
    message.value = error.message
  }
}

function switchTab(t) {
  tab.value = t
  page.value = 1
  loadOrders()
}

function changePage(p) {
  page.value = p
  loadOrders()
}

onMounted(loadOrders)
</script>
