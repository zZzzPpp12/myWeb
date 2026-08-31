<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '@/components/PostCard.vue'
import Avatar from '@/components/Avatar.vue'
import { postApi, topicApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'following', label: '关注' }
]

const active = ref('recommend')
const tagFilter = ref(route.query.tag || '')
const posts = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')

const topics = ref([])
const authors = ref([])
const authorsLoading = ref(false)

const hasMore = computed(() => page.value + 1 < totalPages.value)

async function loadFirst(reset = true) {
  if (reset) {
    page.value = 0
    posts.value = []
  }
  loading.value = true
  error.value = ''
  try {
    let res
    if (active.value === 'recommend' && !tagFilter.value) {
      res = await postApi.recommended()
      // recommended may return an array or a page
      posts.value = Array.isArray(res) ? res : res.content || []
      totalPages.value = Array.isArray(res) ? 0 : res.totalPages || 0
      page.value = Array.isArray(res) ? 0 : res.number || 0
    } else {
      const params = { page: 0, size: 20 }
      if (tagFilter.value) params.tag = tagFilter.value
      else params.feed = active.value
      res = await postApi.list(params)
      posts.value = res.content || []
      totalPages.value = res.totalPages || 0
      page.value = res.number || 0
    }
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const params = { page: page.value + 1, size: 20 }
    if (tagFilter.value) params.tag = tagFilter.value
    else params.feed = active.value
    const res = await postApi.list(params)
    posts.value.push(...(res.content || []))
    page.value = res.number || page.value + 1
    totalPages.value = res.totalPages || totalPages.value
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    loadingMore.value = false
  }
}

function switchTab(key) {
  if (key === 'following' && !auth.isLogin) {
    toast('登录后查看关注内容', 'error')
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }
  if (active.value === key) return
  active.value = key
  tagFilter.value = ''
  router.replace({ path: '/' })
  loadFirst()
}

function onTagClick(name) {
  if (tagFilter.value === name) {
    tagFilter.value = ''
    router.replace({ path: '/' })
  } else {
    tagFilter.value = name
    router.replace({ path: '/', query: { tag: name } })
  }
  loadFirst()
}

watch(() => route.query.tag, (t) => {
  const v = t || ''
  if (v !== tagFilter.value) {
    tagFilter.value = v
    loadFirst()
  }
})

async function loadSidebar() {
  try {
    topics.value = (await topicApi.list({ limit: 20 })) || []
  } catch (e) {
    /* ignore */
  }
  authorsLoading.value = true
  try {
    // recommended authors: take authors from latest feed, dedupe
    const res = await postApi.list({ feed: 'latest', page: 0, size: 20 })
    const seen = new Set()
    const list = []
    for (const p of res.content || []) {
      const a = p.author
      if (a && !seen.has(a.id)) {
        seen.add(a.id)
        list.push(a)
      }
    }
    authors.value = list.slice(0, 6)
  } catch (e) {
    /* ignore */
  } finally {
    authorsLoading.value = false
  }
}

async function toggleFollowAuthor(u) {
  if (!auth.isLogin) {
    toast('请先登录', 'error')
    return
  }
  try {
    const res = await userApi.follow(u.id)
    u.followed = typeof res === 'object' ? !!res.followed : !u.followed
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

async function toggleFollowTopic(t) {
  if (!auth.isLogin) {
    toast('请先登录', 'error')
    return
  }
  const prev = t.followed
  try {
    const res = await topicApi.follow(t.name)
    t.followed = typeof res === 'object' ? !!res.followed : !prev
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(() => {
  loadFirst()
  loadSidebar()
})
</script>

<template>
  <div class="home-layout">
    <section class="home-feed">
      <div class="feed-head card">
        <div class="feed-tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="feed-tab"
            :class="{ active: active === t.key && !tagFilter }"
            @click="switchTab(t.key)"
          >
            {{ t.label }}
          </button>
        </div>
        <div v-if="tagFilter" class="feed-tagbar">
          <span>标签：<b>{{ tagFilter }}</b></span>
          <button class="feed-tag-clear" @click="onTagClick(tagFilter)">清除 ✕</button>
        </div>
      </div>

      <div v-if="loading" class="loading card feed-loading"><span class="spinner"></span>加载中…</div>
      <div v-else-if="error" class="empty card">{{ error }} <button class="feed-tag-clear" @click="loadFirst()">重试</button></div>
      <template v-else>
        <PostCard v-for="p in posts" :key="p.id" :post="p" />
        <div v-if="!posts.length" class="empty card">
          {{ tagFilter ? '该标签下暂无内容' : active === 'following' ? '关注的作者还没有发布内容' : '还没有内容' }}
        </div>
        <button v-if="hasMore" class="btn btn-ghost load-more" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="spinner"></span>{{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </template>
    </section>

    <aside class="home-side">
      <div class="side-card card">
        <h3 class="side-title">热门话题</h3>
        <ul class="side-topics">
          <li v-for="t in topics" :key="t.id || t.name">
            <router-link class="side-topic" :to="`/topic/${encodeURIComponent(t.name)}`">
              <span class="st-emoji">{{ t.emoji || '#' }}</span>
              <span class="st-info">
                <span class="st-name">{{ t.name }}</span>
                <span class="st-sub">{{ t.followerCount || 0 }} 关注 · {{ t.postCount || 0 }} 内容</span>
              </span>
            </router-link>
            <button
              class="btn-follow st-follow"
              :class="{ 'is-followed': t.followed }"
              @click="toggleFollowTopic(t)"
            >
              {{ t.followed ? '已关注' : '关注' }}
            </button>
          </li>
        </ul>
        <span v-if="!topics.length" class="side-empty">暂无话题</span>
      </div>

      <div class="side-card card">
        <h3 class="side-title">推荐作者</h3>
        <div v-if="authorsLoading" class="loading side-loading"><span class="spinner"></span></div>
        <ul v-else-if="authors.length" class="side-authors">
          <li v-for="u in authors" :key="u.id">
            <router-link class="side-author" :to="`/user/${u.id}`">
              <Avatar :src="u.avatar" :name="u.nickname || u.username" :size="36" />
              <span class="sa-info">
                <span class="sa-nick">{{ u.nickname || u.username }}</span>
                <span class="sa-sub">{{ u.postsCount != null ? u.postsCount + ' 篇内容' : '@' + u.username }}</span>
              </span>
            </router-link>
            <button
              v-if="!auth.user || auth.user.id !== u.id"
              class="btn-follow"
              :class="{ 'is-followed': u.followed }"
              @click="toggleFollowAuthor(u)"
            >
              {{ u.followed ? '已关注' : '关注' }}
            </button>
          </li>
        </ul>
        <div v-else class="side-empty">暂无推荐</div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
}

.feed-head {
  padding: 0 20px;
  margin-bottom: 12px;
}

.feed-tabs {
  display: flex;
  gap: 6px;
}

.feed-tab {
  padding: 14px 14px;
  font-size: 15px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
}

.feed-tab:hover {
  color: var(--primary);
}

.feed-tab.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}

.feed-tagbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid var(--bg);
  font-size: 13px;
  color: var(--text-2);
}

.feed-tag-clear {
  color: var(--primary);
  font-size: 13px;
}

.feed-loading,
.home-feed .empty {
  padding: 48px;
  text-align: center;
}

.home-feed .empty.card {
  padding: 48px 16px;
}

.load-more {
  width: 100%;
  padding: 12px 0;
  margin-top: 4px;
}

.home-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}

.side-card {
  padding: 16px 18px;
}

.side-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.side-topics {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-topics li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.side-topic {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.side-topic:hover .st-name {
  color: var(--primary);
}

.st-emoji {
  font-size: 24px;
  line-height: 1;
  flex-shrink: 0;
}

.st-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.st-name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
}

.st-sub {
  font-size: 12px;
  color: var(--text-3);
}

.st-follow {
  padding: 3px 12px;
  font-size: 12px;
  flex-shrink: 0;
}

.side-authors {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-authors li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.side-author {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.sa-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sa-nick {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sa-sub {
  font-size: 12px;
  color: var(--text-3);
}

.side-empty {
  font-size: 13px;
  color: var(--text-3);
}

.side-loading {
  padding: 12px;
}

@media (max-width: 992px) {
  .home-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .home-side {
    display: none;
  }
}
</style>
