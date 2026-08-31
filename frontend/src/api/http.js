import axios from 'axios'

const TOKEN_KEY = '***'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response && err.response.status === 401) {
      setToken('')
      try {
        const { default: router } = await import('@/router')
        const cur = router.currentRoute.value
        if (cur.name !== 'login') {
          router.replace({ name: 'login', query: { redirect: cur.fullPath } })
        }
      } catch (e) {
        /* ignore */
      }
    }
    return Promise.reject(err)
  }
)

export function errMsg(e, fallback = '请求失败，请稍后重试') {
  return (
    (e && e.response && e.response.data && (e.response.data.message || e.response.data.error)) ||
    (e && e.message) ||
    fallback
  )
}

export default http
