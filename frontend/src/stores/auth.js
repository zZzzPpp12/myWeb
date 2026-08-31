import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getToken, setToken } from '@/api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null,
    ready: false
  }),
  getters: {
    isLogin: (s) => !!s.token
  },
  actions: {
    _apply(token, user) {
      this.token = token || ''
      setToken(token)
      if (user) this.user = user
    },
    async login(payload) {
      const res = await authApi.login(payload)
      this._apply(res.token, res.user)
      if (!this.user) await this.fetchMe()
    },
    async register(payload) {
      const res = await authApi.register(payload)
      this._apply(res.token, res.user)
      if (!this.user) await this.fetchMe()
    },
    async fetchMe() {
      this.user = await authApi.me()
    },
    async restore() {
      if (this.token) {
        try {
          await this.fetchMe()
        } catch (e) {
          this.token = ''
          setToken('')
        }
      }
      this.ready = true
    },
    setUser(user) {
      this.user = user
    },
    logout() {
      this.token = ''
      this.user = null
      setToken('')
    }
  }
})
