import { defineStore } from 'pinia'
import { notifApi } from '@/api'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unread: 0,
    timer: null
  }),
  actions: {
    async fetchUnread() {
      try {
        const res = await notifApi.unreadCount()
        this.unread = (res && (res.count ?? res.unreadCount)) || 0
      } catch (e) {
        /* keep last value */
      }
    },
    startPolling() {
      if (this.timer) return
      this.timer = setInterval(() => this.fetchUnread(), 30000)
    },
    stopPolling() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    clear() {
      this.unread = 0
    }
  }
})
