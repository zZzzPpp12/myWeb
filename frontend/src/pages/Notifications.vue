<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '@/components/Avatar.vue'
import { notifApi } from '@/api'
import { useNotificationStore } from '@/stores/notification'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'
import { fromNow } from '@/utils/time'

const notif = useNotificationStore()

const items = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(true)

const icons = {
  LIKE: { cls: 'nt-like', label: '赞了你的内容' },
  BOILING_LIKE: { cls: 'nt-boiling', label: '赞了你的沸点' },
  COMMENT: { cls: 'nt-comment', label: '评论了你的内容' },
  // 沸点评论通知（post 为 null，无跳转链接）
  BOILING_COMMENT: { cls: 'nt-comment', label: '评论了你的沸点' },
  // @提及通知（post 为 null，无跳转链接）
  MENTION: { cls: 'nt-mention', label: '@提及了你' },
  FOLLOW: { cls: 'nt-follow', label: '关注了你' },
  POST: { cls: 'nt-post', label: '发布了新内容' }
}

function iconOf(type) {
  return icons[type] || { cls: 'nt-post', label: '' }
}

async function load(reset = true) {
  if (reset) {
    page.value = 0
    items.value = []
  }
  loading.value = true
  try {
    const res = await notifApi.list({ page: page.value, size: 20 })
    const list = res.content || []
    items.value = reset ? list : items.value.concat(list)
    totalPages.value = res.totalPages || 0
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  load(false)
}

async function markAllRead() {
  try {
    await notifApi.readAll()
    items.value.forEach((n) => (n.read = true))
    notif.unread = 0
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(async () => {
  await load()
  // 进入页面标记全部已读
  markAllRead()
})
</script>

<template>
  <div class="notif-page card">
    <div class="np-head">
      <h1>消息通知</h1>
      <button class="np-readall" @click="markAllRead">全部已读</button>
    </div>

    <div v-if="loading" class="loading"><span class="spinner"></span>加载中…</div>
    <div v-else-if="!items.length" class="empty">暂无通知消息</div>
    <ul v-else class="np-list">
      <li v-for="n in items" :key="n.id" class="np-item" :class="{ unread: !n.read }">
        <span class="np-icon" :class="iconOf(n.type).cls">
          <svg v-if="n.type === 'LIKE'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
          <svg v-else-if="n.type === 'BOILING_LIKE'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>
          <svg v-else-if="n.type === 'COMMENT' || n.type === 'BOILING_COMMENT'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <!-- @ 提及：蓝色 @ 图标 -->
          <span v-else-if="n.type === 'MENTION'" class="np-at">@</span>
          <svg v-else-if="n.type === 'FOLLOW'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/></svg>
          <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
        </span>
        <div class="np-body">
          <div class="np-line">
            <router-link class="np-actor" :to="n.actor?.id ? `/user/${n.actor.id}` : ''">
              <Avatar v-if="n.actor" :src="n.actor.avatar" :name="n.actor.nickname" :size="22" />
              <span>{{ n.actor?.nickname || '系统' }}</span>
            </router-link>
            <span class="np-action">{{ iconOf(n.type).label }}</span>
            <router-link v-if="n.post" class="np-post" :to="`/post/${n.post.id}`">《{{ n.post.title }}》</router-link>
          </div>
          <span class="np-time">{{ fromNow(n.createdAt) }}</span>
        </div>
        <span v-if="!n.read" class="np-dot"></span>
      </li>
    </ul>
    <button v-if="page + 1 < totalPages" class="btn btn-ghost np-more" @click="loadMore">加载更多</button>
  </div>
</template>

<style scoped>
.notif-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 20px 24px;
}

.np-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.np-head h1 {
  font-size: 18px;
}

.np-readall {
  font-size: 13px;
  color: var(--primary);
}

.np-list {
  list-style: none;
}

.np-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 6px;
  border-bottom: 1px solid var(--bg);
}

.np-item.unread {
  background: #f7faff;
}

.np-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.nt-like {
  background: #ffe9e9;
  color: #f0413f;
}

.nt-boiling {
  background: #fff0f6;
  color: #eb5b92;
}

.nt-comment {
  background: #e8f3ff;
  color: #1e80ff;
}

/* @ 提及图标：蓝色 @ 字符 */
.np-at {
  font-size: 15px;
  font-weight: 700;
  color: #1e80ff;
  line-height: 1;
}

.nt-mention {
  background: #e8f3ff;
  color: #1e80ff;
}

.nt-follow {
  background: #e6f7f0;
  color: #00b07f;
}

.nt-post {
  background: #fff3e5;
  color: #ff7d00;
}

.np-body {
  flex: 1;
  min-width: 0;
}

.np-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 14px;
}

.np-actor {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  color: var(--text);
}

.np-actor:hover {
  color: var(--primary);
}

.np-action {
  color: var(--text-2);
}

.np-post {
  color: var(--primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
}

.np-time {
  font-size: 12px;
  color: var(--text-3);
}

.np-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--danger);
  flex-shrink: 0;
  margin-top: 12px;
}

.np-more {
  width: 100%;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .notif-page {
    padding: 14px 12px;
  }

  .np-post {
    max-width: 100%;
  }
}
</style>
