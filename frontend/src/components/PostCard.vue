<script setup>
import { computed } from 'vue'
import Avatar from './Avatar.vue'
import { fromNow } from '@/utils/time'

const props = defineProps({
  post: { type: Object, required: true }
})

const tags = computed(() => {
  const t = props.post.tags
  if (Array.isArray(t)) return t
  if (typeof t === 'string') {
    try {
      const p = JSON.parse(t)
      return Array.isArray(p) ? p : t.split(',')
    } catch (e) {
      return t.split(',').filter(Boolean)
    }
  }
  return []
})
</script>

<template>
  <article class="post-card card">
    <div class="pc-main">
      <router-link class="pc-title" :to="`/post/${post.id}`">
        <span v-if="post.type === 'QUESTION'" class="question-badge">问题</span>{{ post.title }}
      </router-link>
      <p class="pc-excerpt">{{ post.excerpt || (post.content || '').slice(0, 140) }}</p>
      <div class="pc-meta">
        <router-link class="pc-author" :to="`/user/${post.author?.id}`">
          <Avatar :src="post.author?.avatar" :name="post.author?.nickname || post.author?.username" :size="24" />
          <span>{{ post.author?.nickname || post.author?.username }}</span>
        </router-link>
        <span class="pc-time">{{ fromNow(post.createdAt) }}</span>
        <router-link v-for="t in tags" :key="t" class="tag-badge" :to="`/?tag=${encodeURIComponent(t)}`">{{ t }}</router-link>
      </div>
    </div>
    <div class="pc-stats">
      <span title="赞同">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
        赞同 {{ post.likeCount || 0 }}
      </span>
      <span title="反对">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 14V2"/><path d="M9 18.12 10 14H4.17a2 2 0 0 1-1.92-2.56l2.33-8A2 2 0 0 1 6.5 2H20a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-2.76a2 2 0 0 0-1.79 1.11L12 22a3.13 3.13 0 0 1-3-3.88Z"/></svg>
        反对 {{ post.dislikeCount || 0 }}
      </span>
      <span title="评论">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        {{ post.commentCount || 0 }}
      </span>
      <span title="浏览">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
        {{ post.viewCount || 0 }}
      </span>
    </div>
  </article>
</template>

<style scoped>
.post-card {
  padding: 16px 20px;
  margin-bottom: 12px;
  transition: box-shadow 0.15s;
}

.post-card:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.pc-title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.5;
  display: block;
}

.pc-title:hover {
  color: var(--primary);
}

.pc-excerpt {
  margin-top: 6px;
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pc-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: var(--text-3);
}

.pc-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
  font-weight: 500;
}

.pc-author:hover {
  color: var(--primary);
}

.pc-stats {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--text-3);
}

.pc-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
