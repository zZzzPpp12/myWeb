<script setup>
import { onMounted, onBeforeUnmount, watch } from 'vue'
import NavBar from './components/NavBar.vue'
import { useAuthStore } from './stores/auth'
import { useNotificationStore } from './stores/notification'

const auth = useAuthStore()
const notif = useNotificationStore()

onMounted(async () => {
  await auth.restore()
  if (auth.isLogin) {
    notif.fetchUnread()
    notif.startPolling()
  }
})

watch(() => auth.isLogin, (v) => {
  if (v) {
    notif.fetchUnread()
    notif.startPolling()
  } else {
    notif.stopPolling()
    notif.clear()
  }
})

onBeforeUnmount(() => notif.stopPolling())
</script>

<template>
  <div class="app-shell">
    <NavBar />
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <component :is="Component" :key="$route.fullPath" />
      </router-view>
    </main>
  </div>
</template>
