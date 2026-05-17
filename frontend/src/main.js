import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import ProfileView from './views/ProfileView.vue'
import RequirementListView from './views/RequirementListView.vue'
import RequirementDetailView from './views/RequirementDetailView.vue'
import PublishRequirementView from './views/PublishRequirementView.vue'
import NotificationView from './views/NotificationView.vue'
import { isLoggedIn } from './utils/auth'
import './styles.css'

const routes = [
  { path: '/', redirect: '/requirements' },
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  { path: '/profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/requirements', component: RequirementListView, meta: { requiresAuth: true } },
  { path: '/requirements/publish', component: PublishRequirementView, meta: { requiresAuth: true } },
  { path: '/requirements/:reqId', component: RequirementDetailView, meta: { requiresAuth: true } },
  { path: '/notifications', component: NotificationView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn()) {
    next('/profile')
  } else {
    next()
  }
})

createApp(App).use(router).use(ElementPlus).mount('#app')
