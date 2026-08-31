import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/pages/Login.vue') },
  { path: '/', name: 'home', component: () => import('@/pages/Home.vue') },
  { path: '/post/:id', name: 'post', component: () => import('@/pages/PostDetail.vue'), props: true },
  { path: '/editor', name: 'editor', component: () => import('@/pages/Editor.vue'), meta: { requiresAuth: true } },
  { path: '/editor/:id', name: 'edit-post', component: () => import('@/pages/Editor.vue'), props: true, meta: { requiresAuth: true } },
  { path: '/user/:id', name: 'user', component: () => import('@/pages/UserHome.vue'), props: true },
  { path: '/notifications', name: 'notifications', component: () => import('@/pages/Notifications.vue'), meta: { requiresAuth: true } },
  { path: '/search', name: 'search', component: () => import('@/pages/Search.vue') },
  { path: '/settings', name: 'settings', component: () => import('@/pages/Settings.vue'), meta: { requiresAuth: true } },
  { path: '/boiling', name: 'boiling', component: () => import('@/pages/Boiling.vue') },
  { path: '/topic/:name', name: 'topic', component: () => import('@/pages/Topic.vue'), props: true },
  { path: '/leaderboard', name: 'leaderboard', component: () => import('@/pages/Leaderboard.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
