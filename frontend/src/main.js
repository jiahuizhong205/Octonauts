import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import ProfileView from './views/ProfileView.vue'
import RequirementListView from './views/RequirementListView.vue'
import RequirementDetailView from './views/RequirementDetailView.vue'
import NotificationView from './views/NotificationView.vue'
import './styles.css'

const routes = [
  { path: '/', redirect: '/requirements' },
  { path: '/profile', component: ProfileView },
  { path: '/requirements', component: RequirementListView },
  { path: '/requirements/:reqId', component: RequirementDetailView },
  { path: '/notifications', component: NotificationView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp(App).use(router).use(ElementPlus).mount('#app')
