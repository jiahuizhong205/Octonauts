import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/views/HomeView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue')
    },
    {
      path: '/requirements',
      name: 'requirements',
      component: () => import('@/views/RequirementListView.vue')
    },
    {
      path: '/requirements/publish',
      name: 'requirement-publish',
      component: () => import('@/views/PublishRequirementView.vue')
    },
    {
      path: '/requirements/:reqId',
      name: 'requirement-detail',
      component: () => import('@/views/RequirementDetailView.vue')
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/NotificationView.vue')
    }
  ]
});

export default router;
