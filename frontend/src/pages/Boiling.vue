<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '@/components/Avatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'
import { boilingApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'
import { fromNow } from '@/utils/time'

const router = useRouter()
const auth = useAuthStore()

const feeds = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'following', label: '关注' }
]

const feed = ref('latest')
const list = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = computed(() => page.value + 1 < totalPages.value)

// 发布框
const content = ref('')
const imageUrl = ref('')
const publishPending = ref(false)
const canPublish = computed(() => content.value.trim().length > 0 && content.value.length <= 500)

async function load(reset = true) {
  if (reset) {
    page.value = 0
    list.value = []
    loading.value = true
  }
  try {
    const res = await boilingApi.list({ feed: feed.value, page: page.value, size: 20 })
    const arr = res.content || []
    list.value = reset ? arr : list.value.concat(arr)
    totalPages.value = res.totalPages || 0
    page.value = res.number || page.value
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function switchFeed(key) {
  if (key === 'following' && !auth.isLogin) {
    toast('登录后查看关注的沸点', 'error')
    router.push({ name: 'login', query: { redirect: '/boiling' } })
    return
  }
  if (feed.value === key) return
  feed.value = key
  load()
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value += 1
  load(false)
}

function requireLogin() {
  toast('请先登录', 'error')
  router.push({ name: 'login', query: { redirect: '/boiling' } })
}

async function publish() {
  if (!auth.isLogin) return requireLogin()
  const text = content.value.trim()
  if (!text) return
  if (text.length > 500) {
    toast('内容不能超过 500 字', 'error')
    return
  }
  publishPending.value = true
  try {
    const payload = { content: text }
    if (imageUrl.value.trim()) payload.imageUrl = imageUrl.value.trim()
    await boilingApi.create(payload)
    content.value = ''
    imageUrl.value = ''
    toast('发布成功', 'success')
    // 回到最新并刷新列表
    if (feed.value !== 'latest') {
      feed.value = 'latest'
    }
    load()
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    publishPending.value = false
  }
}

async function toggleLike(b) {
  if (!auth.isLogin) return requireLogin()
  const prev = { liked: b.liked, likeCount: b.likeCount }
  b.liked = !prev.liked
  b.likeCount = Math.max(0, (prev.likeCount || 0) + (b.liked ? 1 : -1))
  try {
    const res = await boilingApi.like(b.id)
    if (res && typeof res === 'object') {
      if ('liked' in res) b.liked = !!res.liked
      if ('likeCount' in res) b.likeCount = res.likeCount
    }
  } catch (e) {
    b.liked = prev.liked
    b.likeCount = prev.likeCount
    toast(errMsg(e), 'error')
  }
}

function isAuthor(b) {
  return auth.user && b.author && b.author.id === auth.user.id
}

async function removeBoiling(b) {
  if (!confirm('确定删除这条沸点吗？')) return
  try {
    await boilingApi.remove(b.id)
    list.value = list.value.filter((x) => x.id !== b.id)
    toast('已删除', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(() => load())
</script>

<template>
  <div class="boiling-page">
    <section class="bp-publish card">
      <div v-if="!auth.isLogin" class="bp-login-tip">
        <router-link :to="{ name: 'login', query: { redirect: '/boiling' } }">登录</router-link>
        后即可发布沸点，分享你的灵感与日常
      </div>
      <template v-else>
        <textarea
          v-model="content"
          class="bp-textarea"
          rows="3"
          maxlength="500"
          placeholder="这一刻的想法…（不超过 500 字）"
        ></textarea>
        <input v-model="imageUrl" class="bp-img-input" type="text" placeholder="可选：图片链接 URL" />
        <div class="bp-publish-foot">
          <span class="bp-count" :class="{ over: content.length > 500 }">{{ content.length }}/500</span>
          <button class="btn btn-primary" :disabled="!canPublish || publishPending" @click="publish">
            {{ publishPending ? '发布中…' : '发布' }}
          </button>
        </div>
      </template>
    </section>

    <div class="bp-tabs card">
      <button
        v-for="f in feeds"
        :key="f.key"
        class="bp-tab"
        :class="{ active: feed === f.key }"
        @click="switchFeed(f.key)"
      >
        {{ f.label }}
      </button>
    </div>

    <div v-if="loading" class="loading card"><span class="spinner"></span>加载中…</div>
    <template v-else>
      <article v-for="b in list" :key="b.id" class="bp-item card">
        <div class="bp-head">
          <router-link class="bp-author" :to="`/user/${b.author?.id}`">
            <Avatar :src="b.author?.avatar" :name="b.author?.nickname || b.author?.username" :size="34" />
            <span class="bp-nick">{{ b.author?.nickname || b.author?.username }}</span>
            <LevelBadge :level="b.author?.level" :level-name="b.author?.levelName" size="sm" />
          </router-link>
          <span class="bp-time">{{ fromNow(b.createdAt) }}</span>
        </div>
        <p class="bp-content">{{ b.content }}</p>
        <img
          v-if="b.imageUrl"
          class="bp-image"
          :src="b.imageUrl"
          alt="沸点图片"
          @error="$event.target.style.display = 'none'"
        />
        <div class="bp-foot">
          <button class="bp-like" :class="{ active: b.liked }" @click="toggleLike(b)">
            <svg viewBox="0 0 24 24" width="16" height="16" :fill="b.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
            {{ b.likeCount || 0 }}
          </button>
          <button v-if="isAuthor(b)" class="bp-del" @click="removeBoiling(b)">删除</button>
        </div>
      </article>
      <div v-if="!list.length" class="empty card">
        {{ feed === 'following' ? '关注的作者还没有发布沸点' : '还没有沸点，来发布第一条吧' }}
      </div>
      <button v-if="hasMore" class="btn btn-ghost bp-more" :disabled="loadingMore" @click="loadMore">
        <span v-if="loadingMore" class="spinner"></span>{{ loadingMore ? '加载中…' : '加载更多' }}
      </button>
    </template>
  </div>
</template>

<style scoped>
.boiling-page {
  max-width: 720px;
  margin: 0 auto;
}

.bp-publish {
  padding: 16px 18px;
  margin-bottom: 12px;
}

.bp-login-tip {
  padding: 14px 0;
  color: var(--text-2);
  font-size: 14px;
  text-align: center;
}

.bp-login-tip a {
  color: var(--primary);
  font-weight: 600;
}

.bp-textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.7;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.bp-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(30, 128, 255, 0.12);
}

.bp-img-input {
  width: 100%;
  margin-top: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
}

.bp-publish-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.bp-count {
  font-size: 12px;
  color: var(--text-3);
}

.bp-count.over {
  color: var(--danger);
}

.bp-tabs {
  padding: 0 12px;
  margin-bottom: 12px;
  display: flex;
  gap: 4px;
}

.bp-tab {
  padding: 13px 16px;
  font-size: 15px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
}

.bp-tab.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}

.bp-item {
  padding: 16px 18px;
  margin-bottom: 12px;
}

.bp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.bp-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.bp-nick {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-2);
}

.bp-nick:hover {
  color: var(--primary);
}

.bp-time {
  font-size: 12px;
  color: var(--text-3);
  flex-shrink: 0;
}

.bp-content {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.bp-image {
  margin-top: 12px;
  max-width: 100%;
  border-radius: 8px;
}

.bp-foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.bp-like {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-3);
}

.bp-like:hover,
.bp-like.active {
  color: var(--primary);
}

.bp-del {
  font-size: 13px;
  color: var(--danger);
  margin-left: auto;
}

.bp-more {
  width: 100%;
  padding: 12px 0;
}

@media (max-width: 768px) {
  .bp-publish,
  .bp-item {
    padding: 14px 14px;
  }
}
</style>