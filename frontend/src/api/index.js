import http from './http'

const data = (p) => p.then((r) => r.data)

export const authApi = {
  register: (payload) => data(http.post('/auth/register', payload)),
  login: (payload) => data(http.post('/auth/login', payload)),
  me: () => data(http.get('/auth/me'))
}

export const userApi = {
  get: (id) => data(http.get(`/users/${id}`)),
  updateProfile: (payload) => data(http.put('/users/profile', payload)),
  follow: (id) => data(http.post(`/users/${id}/follow`)),
  following: (id, page = 0, size = 20) => data(http.get(`/users/${id}/following`, { params: { page, size } })),
  followers: (id, page = 0, size = 20) => data(http.get(`/users/${id}/followers`, { params: { page, size } })),
  bookmarks: (page = 0, size = 20) => data(http.get('/users/me/bookmarks', { params: { page, size } }))
}

export const postApi = {
  create: (payload) => data(http.post('/posts', payload)),
  update: (id, payload) => data(http.put(`/posts/${id}`, payload)),
  remove: (id) => http.delete(`/posts/${id}`),
  list: (params) => data(http.get('/posts', { params })),
  recommended: () => data(http.get('/posts/recommended')),
  detail: (id) => data(http.get(`/posts/${id}`)),
  comments: (id, params) => data(http.get(`/posts/${id}/comments`, { params })),
  addComment: (id, payload) => data(http.post(`/posts/${id}/comments`, payload)),
  like: (id) => data(http.post(`/posts/${id}/like`)),
  dislike: (id) => data(http.post(`/posts/${id}/dislike`)),
  bookmark: (id) => data(http.post(`/posts/${id}/bookmark`))
}

export const notifApi = {
  list: (params) => data(http.get('/notifications', { params })),
  unreadCount: () => data(http.get('/notifications/unread-count')),
  readAll: () => http.post('/notifications/read-all')
}

export const searchApi = {
  search: (params) => data(http.get('/search', { params }))
}

export const tagApi = {
  list: () => data(http.get('/tags'))
}

export const topicApi = {
  list: (params) => data(http.get('/topics', { params })),
  get: (name) => data(http.get(`/topics/${encodeURIComponent(name)}`)),
  posts: (name, params) => data(http.get(`/topics/${encodeURIComponent(name)}/posts`, { params })),
  follow: (name) => data(http.post(`/topics/${encodeURIComponent(name)}/follow`))
}

export const boilingApi = {
  list: (params) => data(http.get('/boiling', { params })),
  create: (payload) => data(http.post('/boiling', payload)),
  like: (id) => data(http.post(`/boiling/${id}/like`)),
  remove: (id) => http.delete(`/boiling/${id}`)
}

export const leaderboardApi = {
  posts: (params) => data(http.get('/leaderboard/posts', { params })),
  users: (params) => data(http.get('/leaderboard/users', { params }))
}
