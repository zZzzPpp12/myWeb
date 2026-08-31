<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '@/components/Avatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'
import PostCard from '@/components/PostCard.vue'
import UserListModal from '@/components/UserListModal.vue'
import { postApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const auth = useAuthStore()

const user = ref(null)
const loading = ref(true)
const error = ref('')
const tab = ref('posts') // posts | bookmarks

const posts = ref([])
const page = ref(0)
const totalPages = ref(0)
const listLoading = ref(false)

const modal = ref(false)
const modalMode = ref('following')

const isMe = computed(() => auth.user && user.value && auth.user.id === user.value.id)
const canSeeBookmarks = computed(() => isMe.value)
const hasMore = computed(() => page.value + 1 < totalPages.value)

async function loadUser() {
  loading.value = true
  error.value = ''
  try {
    user.value = await userApi.get(props.id)
  } catch (e) {
    error.value = errMsg(e, '用户不存在')
  } finally {
    loading.value = false
  }
}

async function loadList(reset = true) {
  if (reset) {
    page.value = 0
    posts.value = []
  }
  listLoading.value = true
  try {
    let res
    if (tab.value === 'posts') {
      res = await postApi.list({ author: props.id, feed: 'latest', page: page.value, size: 20 })
    } else {
      res = await userApi.bookmarks(page.value, 20)
    }
    posts.value = reset ? res.content || [] : posts.value.concat(res.content || [])
    totalPages.value = res.totalPages || 0
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    listLoading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadList(false)
}

function switchTab(t) {
  if (t === 'bookmarks' && !canSeeBookmarks.value) return
  if (tab.value === t) return
  tab.value = t
  loadList()
}

async function toggleFollow() {
  if (!auth.isLogin) {
    toast('请先登录', 'error')
    return router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
  try {
    const res = await userApi.follow(props.id)
    const followed = typeof res === 'object' ? !!res.followed : !user.value.followed
    user.value.followed = followed
    user.value.followersCount = Math.max(0, (user.value.followersCount || 0) + (followed ? 1 : -1))
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

function openModal(mode) {
  modalMode.value = mode
  modal.value = true
}

onMounted(() => {
  loadUser()
  loadList()
})

watch(() => props.id, () => {
  tab.value = 'posts'
  loadUser()
  loadList()
})
</script>

<template>
  <div class="uh-page">
    <div v-if="loading" class="loading card"><span class="spinner"></span>加载中…</div>
    <div v-else-if="error" class="empty card">{{ error }}</div>

    <template v-else-if="user">
      <header class="uh-header card">
        <Avatar :src="user.avatar" :name="user.nickname || user.username" :size="84" />
        <div class="uh-info">
          <div class="uh-name-row">
            <h1>{{ user.nickname || user.username }}</h1>
            <LevelBadge :level="user.level" :level-name="user.levelName" />
            <span class="uh-username">@{{ user.username }}</span>
            <!-- 在线状态：5 分钟内活跃显示绿点在线，否则灰点离线 -->
            <span class="uh-online" :class="{ on: user.online }">
              {{ user.online ? '在线' : '离线' }}
            </span>
          </div>
          <p class="uh-bio">{{ user.bio || '这个作者很低调，什么都没留下～' }}</p>
          <div class="uh-stats">
            <span class="uh-stat"><b>{{ user.reputation || 0 }}</b><span>声望</span></span>
            <span class="uh-stat"><b>{{ user.likesReceived || 0 }}</b><span>收到的赞</span></span>
            <span class="uh-stat"><b>{{ user.boilingsCount || 0 }}</b><span>沸点</span></span>
            <button class="uh-stat" @click="openModal('following')">
              <b>{{ user.followingCount || 0 }}</b><span>关注</span>
            </button>
            <button class="uh-stat" @click="openModal('followers')">
              <b>{{ user.followersCount || 0 }}</b><span>粉丝</span>
            </button>
            <span class="uh-stat"><b>{{ user.postsCount || 0 }}</b><span>内容</span></span>
          </div>
        </div>
        <div class="uh-actions">
          <button v-if="isMe" class="btn btn-ghost" @click="router.push('/settings')">编辑资料</button>
          <button
            v-else-if="auth.user"
            class="btn-follow"
            :class="{ 'is-followed': user.followed }"
            @click="toggleFollow"
          >
            {{ user.followed ? '已关注' : '+ 关注' }}
          </button>
          <button v-else class="btn-follow" @click="toggleFollow">+ 关注</button>
        </div>
      </header>

      <nav class="uh-tabs card">
        <button class="uh-tab" :class="{ active: tab === 'posts' }" @click="switchTab('posts')">文章</button>
        <button
          v-if="canSeeBookmarks"
          class="uh-tab"
          :class="{ active: tab === 'bookmarks' }"
          @click="switchTab('bookmarks')"
        >
          收藏
        </button>
      </nav>

      <section class="uh-list">
        <div v-if="listLoading && !posts.length" class="loading card"><span class="spinner"></span>加载中…</div>
        <template v-else>
          <PostCard v-for="p in posts" :key="p.id" :post="p" />
          <div v-if="!posts.length" class="empty card">
            {{ tab === 'posts' ? '还没有发布过内容' : '还没有收藏内容' }}
          </div>
          <button v-if="hasMore" class="btn btn-ghost uh-more" @click="loadMore">加载更多</button>
        </template>
      </section>

      <UserListModal v-model="modal" :user-id="props.id" :mode="modalMode" />
    </template>
  </div>
</template>

<style scoped>
.uh-page {
  max-width: 860px;
  margin: 0 auto;
}

.uh-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 26px 28px;
  flex-wrap: wrap;
}

.uh-info {
  flex: 1;
  min-width: 220px;
}

.uh-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.uh-name-row h1 {
  font-size: 22px;
}

.uh-username {
  font-size: 13px;
  color: var(--text-3);
}

/* 在线状态点：绿点在线 / 灰点离线 */
.uh-online {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-3);
}

.uh-online::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b8bcc4;
}

.uh-online.on {
  color: var(--success);
}

.uh-online.on::before {
  background: var(--success);
}

.uh-bio {
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-2);
}

.uh-stats {
  margin-top: 14px;
  display: flex;
  gap: 22px;
}

.uh-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  font-size: 13px;
  color: var(--text-3);
}

.uh-stat b {
  font-size: 16px;
  color: var(--text);
}

button.uh-stat:hover b {
  color: var(--primary);
}

.uh-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.uh-tabs {
  margin-top: 14px;
  padding: 0 12px;
  display: flex;
  gap: 4px;
}

.uh-tab {
  padding: 13px 16px;
  font-size: 15px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
}

.uh-tab.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}

.uh-list {
  margin-top: 14px;
}

.uh-more {
  width: 100%;
  padding: 12px 0;
}

@media (max-width: 768px) {
  .uh-header {
    padding: 18px 16px;
    gap: 14px;
  }

  .uh-actions {
    width: 100%;
  }

  .uh-actions .btn-follow,
  .uh-actions .btn {
    flex: 1;
    justify-content: center;
    padding: 8px 0;
  }
}
</style>
