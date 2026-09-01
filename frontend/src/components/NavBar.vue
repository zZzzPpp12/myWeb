<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Avatar from './Avatar.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const notif = useNotificationStore()

const kw = ref(route.query.q || '')
const menuOpen = ref(false)
const userMenuOpen = ref(false)
const mobileMenuRef = ref(null)
const userMenuRef = ref(null)

const unreadText = computed(() => (notif.unread > 99 ? '99+' : notif.unread))

function doSearch() {
  const q = kw.value.trim()
  if (!q) return
  menuOpen.value = false
  router.push({ path: '/search', query: { q } })
}

function goPublish() {
  menuOpen.value = false
  if (!auth.isLogin) {
    router.push({ name: 'login', query: { redirect: '/editor' } })
  } else {
    router.push('/editor')
  }
}

function goNotif() {
  menuOpen.value = false
  router.push('/notifications')
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function onDocClick(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) userMenuOpen.value = false
  if (mobileMenuRef.value && !mobileMenuRef.value.contains(e.target)) menuOpen.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <header class="navbar">
    <div class="nb-inner">
      <router-link to="/" class="nb-logo">
        <span class="nb-mark">M</span>
        <span class="nb-name">码社区</span>
      </router-link>

      <nav class="nb-links">
        <router-link class="nb-link" :class="{ active: route.path === '/boiling' }" to="/boiling">沸点</router-link>
        <router-link class="nb-link" :class="{ active: route.path === '/leaderboard' }" to="/leaderboard">排行榜</router-link>
      </nav>

      <form class="nb-search" @submit.prevent="doSearch">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
        <input v-model="kw" type="text" placeholder="搜索内容、作者、标签" />
      </form>

      <nav class="nb-actions">
        <button class="nb-publish btn btn-primary" @click="goPublish">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
          写文章
        </button>

        <router-link class="nb-about" :class="{ active: route.path === '/about' }" to="/about">关于作者</router-link>

        <template v-if="auth.isLogin">
          <button class="nb-icon-btn" title="消息" @click="goNotif">
            <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
            <span v-if="notif.unread > 0" class="nb-dot">{{ unreadText }}</span>
          </button>

          <div class="nb-user" ref="userMenuRef">
            <button class="nb-avatar-btn" @click.stop="toggleUserMenu">
              <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="34" />
            </button>
            <div v-if="userMenuOpen" class="nb-dropdown card">
              <router-link class="nb-dd-item" :to="`/user/${auth.user?.id}`" @click="userMenuOpen = false">个人主页</router-link>
              <router-link class="nb-dd-item" to="/settings" @click="userMenuOpen = false">账号设置</router-link>
              <button class="nb-dd-item nb-dd-logout" @click="auth.logout(); userMenuOpen = false; router.push('/')">退出登录</button>
            </div>
          </div>
        </template>
        <router-link v-else class="btn btn-primary nb-login" :to="{ name: 'login', query: { redirect: route.fullPath } }">登录</router-link>
      </nav>

      <button class="nb-burger" :class="{ open: menuOpen }" aria-label="菜单" @click.stop="menuOpen = !menuOpen">
        <span></span><span></span><span></span>
      </button>
    </div>

    <div v-if="menuOpen" ref="mobileMenuRef" class="nb-mobile">
      <form class="nb-search nb-search-mobile" @submit.prevent="doSearch">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
        <input v-model="kw" type="text" placeholder="搜索内容、作者、标签" />
      </form>
      <router-link class="nb-m-item" to="/boiling" @click="menuOpen = false">沸点</router-link>
      <router-link class="nb-m-item" to="/leaderboard" @click="menuOpen = false">排行榜</router-link>
      <router-link class="nb-m-item" to="/about" @click="menuOpen = false">关于作者</router-link>
      <button class="nb-m-item" @click="goPublish">写文章</button>
      <template v-if="auth.isLogin">
        <button class="nb-m-item" @click="goNotif">消息通知<span v-if="notif.unread > 0" class="nb-dot nb-dot-inline">{{ unreadText }}</span></button>
        <router-link class="nb-m-item" :to="`/user/${auth.user?.id}`" @click="menuOpen = false">个人主页</router-link>
        <router-link class="nb-m-item" to="/settings" @click="menuOpen = false">账号设置</router-link>
        <button class="nb-m-item nb-dd-logout" @click="auth.logout(); menuOpen = false; router.push('/')">退出登录</button>
      </template>
      <router-link v-else class="nb-m-item" :to="{ name: 'login', query: { redirect: route.fullPath } }" @click="menuOpen = false">登录 / 注册</router-link>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--nav-h);
  background: #fff;
  border-bottom: 1px solid var(--border);
  z-index: 1000;
}

.nb-inner {
  max-width: 1200px;
  height: var(--nav-h);
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.nb-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nb-mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}

.nb-name {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.nb-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nb-link {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-2);
  transition: all 0.15s;
}

.nb-link:hover,
.nb-link.active {
  color: var(--primary);
}

.nb-link.active {
  background: var(--primary-light);
  font-weight: 600;
}

.nb-about {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-2);
  transition: all 0.15s;
  white-space: nowrap;
}

.nb-about:hover,
.nb-about.active {
  color: var(--primary);
}

.nb-search {
  flex: 1;
  max-width: 420px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f4f5f7;
  border: 1px solid transparent;
  border-radius: 20px;
  padding: 7px 14px;
  color: var(--text-3);
  transition: all 0.15s;
}

.nb-search:focus-within {
  background: #fff;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(30, 128, 255, 0.1);
}

.nb-search input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  min-width: 0;
}

.nb-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.nb-icon-btn {
  position: relative;
  color: var(--text-2);
  display: inline-flex;
  padding: 6px;
  border-radius: 6px;
}

.nb-icon-btn:hover {
  background: var(--bg);
  color: var(--primary);
}

.nb-dot {
  position: absolute;
  top: 0;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

.nb-dot-inline {
  position: static;
  display: inline-block;
  margin-left: 8px;
}

.nb-user {
  position: relative;
}

.nb-avatar-btn {
  display: inline-flex;
  border-radius: 50%;
}

.nb-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 148px;
  padding: 6px;
  display: flex;
  flex-direction: column;
  z-index: 1100;
}

.nb-dd-item {
  display: block;
  text-align: left;
  width: 100%;
  padding: 9px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--text);
}

.nb-dd-item:hover {
  background: var(--bg);
  color: var(--primary);
}

.nb-dd-logout {
  color: var(--danger);
}

.nb-dd-logout:hover {
  background: #fff1f0;
  color: var(--danger);
}

.nb-burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 38px;
  height: 38px;
  padding: 8px;
  border-radius: 6px;
}

.nb-burger span {
  display: block;
  height: 2px;
  background: var(--text-2);
  border-radius: 2px;
  transition: transform 0.2s, opacity 0.2s;
}

.nb-burger.open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.nb-burger.open span:nth-child(2) {
  opacity: 0;
}

.nb-burger.open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

.nb-mobile {
  display: none;
  flex-direction: column;
  background: #fff;
  border-bottom: 1px solid var(--border);
  padding: 8px 16px 14px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}

.nb-m-item {
  display: flex;
  align-items: center;
  text-align: left;
  padding: 12px 4px;
  font-size: 15px;
  border-bottom: 1px solid var(--bg);
  color: var(--text);
}

.nb-m-item:last-child {
  border-bottom: none;
}

.nb-search-mobile {
  max-width: none;
  margin: 6px 0 10px;
}

@media (max-width: 768px) {
  .nb-search:not(.nb-search-mobile),
  .nb-links,
  .nb-publish,
  .nb-login,
  .nb-actions {
    display: none;
  }

  .nb-burger {
    display: flex;
    margin-left: auto;
  }

  .nb-mobile {
    display: flex;
  }
}
</style>
