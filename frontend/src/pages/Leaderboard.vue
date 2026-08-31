<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '@/components/Avatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'
import { leaderboardApi } from '@/api'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const tabs = [
  { key: 'posts', label: '热文榜' },
  { key: 'users', label: '创作者榜' }
]

const active = ref('posts')
const posts = ref([])
const users = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    if (active.value === 'posts') {
      posts.value = (await leaderboardApi.posts({ limit: 10 })) || []
    } else {
      users.value = (await leaderboardApi.users({ limit: 10 })) || []
    }
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    loading.value = false
  }
}

// 名次样式：前三名分别用金银铜
function rankClass(i) {
  if (i === 1) return 'rank-1'
  if (i === 2) return 'rank-2'
  if (i === 3) return 'rank-3'
  return ''
}

function switchTab(key) {
  if (active.value === key) return
  active.value = key
  load()
}

onMounted(() => load())
</script>

<template>
  <div class="lb-page card">
    <div class="lb-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="lb-tab"
        :class="{ active: active === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="loading"><span class="spinner"></span>加载中…</div>

    <!-- 热文榜 -->
    <ul v-else-if="active === 'posts'" class="lb-list">
      <li v-for="(p, i) in posts" :key="p.id" class="lb-row">
        <span class="lb-rank" :class="rankClass(i + 1)">{{ i + 1 }}</span>
        <div class="lb-main">
          <router-link class="lb-title" :to="`/post/${p.id}`">{{ p.title }}</router-link>
          <div class="lb-meta">
            <router-link class="lb-author" :to="`/user/${p.author?.id}`">{{ p.author?.nickname || p.author?.username }}</router-link>
            <span>赞同 {{ p.likeCount || 0 }}</span>
            <span v-if="p.dislikeCount != null">反对 {{ p.dislikeCount }}</span>
            <span>评论 {{ p.commentCount || 0 }}</span>
          </div>
        </div>
      </li>
      <li v-if="!posts.length" class="empty">暂无热文</li>
    </ul>

    <!-- 创作者榜 -->
    <ul v-else class="lb-list">
      <li v-for="(u, i) in users" :key="u.id" class="lb-row" :class="i < 3 ? 'lb-top' : ''">
        <span class="lb-rank" :class="rankClass(i + 1)">{{ i + 1 }}</span>
        <router-link class="lb-user" :to="`/user/${u.id}`">
          <Avatar :src="u.avatar" :name="u.nickname || u.username" :size="42" />
          <span class="lb-u-info">
            <span class="lb-u-nick">
              {{ u.nickname || u.username }}
              <LevelBadge :level="u.level" :level-name="u.levelName" size="sm" />
            </span>
            <span class="lb-u-sub">@{{ u.username }}</span>
          </span>
        </router-link>
        <span class="lb-reputation">声望 {{ u.reputation || 0 }}</span>
      </li>
      <li v-if="!users.length" class="empty">暂无创作者</li>
    </ul>
  </div>
</template>

<style scoped>
.lb-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 0 16px 16px;
}

.lb-tabs {
  padding: 0 8px;
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--bg);
}

.lb-tab {
  padding: 16px 18px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
}

.lb-tab.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.lb-list {
  list-style: none;
}

.lb-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 8px;
  border-bottom: 1px solid var(--bg);
}

.lb-row.lb-top {
  background: #fbfdff;
}

.lb-rank {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-3);
  background: #f4f5f7;
  flex-shrink: 0;
}

.lb-rank.rank-1 {
  background: #fff3d6;
  color: #d48806;
}

.lb-rank.rank-2 {
  background: #f0f2f5;
  color: #6b7280;
}

.lb-rank.rank-3 {
  background: #fdeedd;
  color: #c46a1b;
}

.lb-main {
  min-width: 0;
  flex: 1;
}

.lb-title {
  font-size: 15px;
  font-weight: 600;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lb-title:hover {
  color: var(--primary);
}

.lb-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--text-3);
}

.lb-author {
  color: var(--text-2);
}

.lb-author:hover {
  color: var(--primary);
}

.lb-user {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.lb-u-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.lb-u-nick {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
}

.lb-u-sub {
  font-size: 12px;
  color: var(--text-3);
}

.lb-reputation {
  font-size: 13px;
  color: #ff7d00;
  font-weight: 600;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .lb-page {
    padding: 0 8px 12px;
  }

  .lb-row {
    gap: 10px;
  }
}
</style>