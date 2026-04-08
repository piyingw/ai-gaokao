import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/universities',
      name: 'universities',
      component: () => import('@/views/UniversitiesView.vue')
    },
    {
      path: '/university/:id',
      name: 'university-detail',
      component: () => import('@/views/UniversityDetailView.vue'),
      props: true
    },
    {
      path: '/majors',
      name: 'majors',
      component: () => import('@/views/MajorsView.vue')
    },
    {
      path: '/major/:id',
      name: 'major-detail',
      component: () => import('@/views/MajorDetailView.vue'),
      props: true
    },
    {
      path: '/ai-assistant',
      name: 'ai-assistant',
      component: () => import('@/views/AIAssistantView.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/views/UserView.vue')
    }
  ]
})

export default router