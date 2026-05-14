<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchHealth } from '@/api/health';

const loading = ref(false);
const statusText = ref('待连接');
const serviceName = ref('CampusHub Backend');

const loadHealth = async () => {
  loading.value = true;
  try {
    const response = await fetchHealth();
    statusText.value = response.data.data.status;
    serviceName.value = response.data.data.service;
  } catch (error) {
    const message = error instanceof Error ? error.message : '健康检查失败';
    statusText.value = 'ERROR';
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void loadHealth();
});
</script>

<template>
  <div class="page-shell">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">CampusHub · 基础工程</p>
        <h1>校园互助服务平台</h1>
        <p class="description">
          已完成 Spring Boot 后端骨架、Vue 3 前端骨架、MySQL / Redis 接入与统一 API 规范初始搭建。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" :loading="loading" @click="loadHealth">
            连接后端
          </el-button>
          <el-tag :type="statusText === 'UP' ? 'success' : statusText === 'ERROR' ? 'danger' : 'info'" size="large">
            {{ serviceName }} · {{ statusText }}
          </el-tag>
        </div>
      </div>
      <div class="hero-panel">
        <div class="metric-box">
          <span class="metric-label">API 规范</span>
          <strong>code / message / data</strong>
          <small>/api/v1/health 已打通</small>
        </div>
        <div class="metric-box accent">
          <span class="metric-label">数据库</span>
          <strong>campushub_db</strong>
          <small>sys_user / biz_requirement / biz_order / biz_evaluation</small>
        </div>
      </div>
    </section>

    <section class="feature-grid">
      <article class="feature-card">
        <h3>后端基础结构</h3>
        <p>Spring Boot 3、MyBatis-Plus、Redis、统一异常和响应封装已就绪。</p>
      </article>
      <article class="feature-card">
        <h3>前端基础结构</h3>
        <p>Vue 3、Vite、Element Plus、路由、Axios 封装与开发代理已配置。</p>
      </article>
      <article class="feature-card">
        <h3>数据库初始化</h3>
        <p>Compose 挂载 MySQL / Redis，P3 初始化脚本可直接写入目标库。</p>
      </article>
    </section>
  </div>
</template>
