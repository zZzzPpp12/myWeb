<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import PostCard from '@/components/PostCard.vue'
import { topicApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const props = defineProps({ name: { type: String, required: true } })
const router = useRouter()
const auth = useAuthStore()

const topic = ref(null)
const loading = ref(true)
const error = ref('')

const posts = ref([])
const page = ref(0)
const totalPages = ref(0)
const listLoading = ref(false)
const hasMore = computed(() => page.value + 1 < totalPages.value)

async function loadTopic() {
  loading.value = true
  error.value = ''
  try {
    topic.value = await topicApi.get(props.name)
  } catch (e) {
    error.value = errMsg(e, '话题不存在')
  } finally {
    loading.value = false
  }
}

async function loadPosts(reset = true) {
  if (reset) {
    page.value = 0
    posts.value = []
    listLoading.value = true
  }
  try {
    const res = await topicApi.posts(props.name, { page: page.value, size: 20 })
    const arr = res.content || []
    posts.value = reset ? arr : posts.value.concat(arr)
    totalPages.value = res.totalPages || 0
    page.value = res.number || page.value
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    listLoading.value = false
  }
}

function loadMore() {
  if (listLoading.value || !hasMore.value) return
  page.value += 1
  loadPosts(false)
}

async function toggleFollow() {
  if (!auth.isLogin) {
    toast('请先登录', 'error')
    return router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
  const t = topic.value
  if (!t) return
  const prev = t.followed
  try {
    const res = await topicApi.follow(t.name)
    const followed = typeof res === 'object' ? !!res.followed : !prev
    t.followed = followed
    t.followerCount = Math.max(0, (t.followerCount || 0) + (followed ? 1 : -1))
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(() => {
  loadTopic()
  loadPosts()
})

watch(() => props.name, () => {
  topic.value = null
  loadTopic()
  loadPosts()
})
</script>

<template>
  <div class="topic-page">
    <div v-if="loading && !topic" class="loading card"><span class="spinner"></span>加载中…</div>
    <div v-else-if="error && !topic" class="empty card">{{ error }}</div>

    <template v-else>
      <header v-if="topic" class="topic-head card">
        <div class="th-main">
          <span class="th-emoji">{{ topic.emoji || '#' }}</span>
          <div class="th-info">
            <h1 class="th-name">{{ topic.name }}</h1>
            <p v-if="topic.description" class="th-desc">{{ topic.description }}</p>
          </div>
        </div>
        <div class="th-side">
          <div class="th-stats">
            <span>{{ topic.followerCount || 0 }} 关注</span>
            <span>{{ topic.postCount || 0 }} 内容</span>
          </div>
          <button
            class="btn-follow th-follow"
            :class="{ 'is-followed': topic.followed }"
            @click="toggleFollow"
          >
            {{ topic.followed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </header>

      <section class="topic-posts">
        <div v-if="listLoading && !posts.length" class="loading card"><span class="spinner"></span>加载中…</div>
        <template v-else>
          <PostCard v-for="p in posts" :key="p.id" :post="p" />
          <div v-if="!posts.length" class="empty card">该话题下暂无内容</div>
          <button v-if="hasMore" class="btn btn-ghost tp-more" :disabled="listLoading" @click="loadMore">
            <span v-if="listLoading" class="spinner"></span>{{ listLoading ? '加载中…' : '加载更多' }}
          </button>
        </template>
      </section>
    </template>
  </div>
</template>

<style scoped>
.topic-page {
  max-width: 860px;
  margin: 0 auto;
}

.topic-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.th-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.th-emoji {
  font-size: 44px;
  line-height: 1;
  flex-shrink: 0;
}

.th-info {
  min-width: 0;
}

.th-name {
  font-size: 24px;
  font-weight: 700;
}

.th-desc {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-2);
}

.th-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.th-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-3);
}

.th-follow {
  padding: 6px 20px;
}

.tp-more {
  width: 100%;
  padding: 12px 0;
}

@media (max-width: 768px) {
  .topic-head {
    padding: 18px 16px;
  }

  .th-side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}
</style>