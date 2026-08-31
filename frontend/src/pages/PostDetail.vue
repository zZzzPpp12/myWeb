<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '@/components/Avatar.vue'
import CommentItem from '@/components/CommentItem.vue'
import { postApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'
import { renderMarkdown } from '@/utils/markdown'
import { fromNow } from '@/utils/time'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const auth = useAuthStore()

const post = ref(null)
const loading = ref(true)
const error = ref('')
const contentHtml = computed(() => (post.value ? renderMarkdown(post.value.content) : ''))

const comments = ref([])
const cPage = ref(0)
const cTotalPages = ref(0)
const cLoading = ref(false)
const newComment = ref('')
const submitting = ref(false)
const replyTo = ref(null) // comment being replied to

const isAuthor = computed(() => auth.user && post.value && post.value.author?.id === auth.user.id)
const likePending = ref(false)
const dislikePending = ref(false)
const bmPending = ref(false)

// 认同值 = 赞同 - 反对
const score = computed(() => {
  if (!post.value) return 0
  return (Number(post.value.likeCount) || 0) - (Number(post.value.dislikeCount) || 0)
})

async function loadPost() {
  loading.value = true
  error.value = ''
  try {
    post.value = await postApi.detail(props.id)
  } catch (e) {
    error.value = errMsg(e, '文章加载失败')
  } finally {
    loading.value = false
  }
}

async function loadComments(reset = true) {
  if (cLoading.value) return
  if (reset) {
    cPage.value = 0
    comments.value = []
  }
  cLoading.value = true
  try {
    const res = await postApi.comments(props.id, { page: cPage.value, size: 20 })
    const list = res.content || []
    comments.value = reset ? list : comments.value.concat(list)
    cTotalPages.value = res.totalPages || 0
  } catch (e) {
    /* comments optional */
  } finally {
    cLoading.value = false
  }
}

function loadMoreComments() {
  cPage.value += 1
  loadComments(false)
}

function requireLogin() {
  toast('请先登录', 'error')
  router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
}

async function toggleLike() {
  if (!auth.isLogin) return requireLogin()
  if (likePending.value || !post.value) return
  likePending.value = true
  const snapshot = {
    liked: post.value.liked,
    likeCount: post.value.likeCount,
    downvoted: post.value.downvoted,
    dislikeCount: post.value.dislikeCount
  }
  post.value.liked = !snapshot.liked
  post.value.likeCount = Math.max(0, (snapshot.likeCount || 0) + (post.value.liked ? 1 : -1))
  // 赞同与反对互斥：点赞同需取消已有的反对
  if (post.value.liked && snapshot.downvoted) {
    post.value.downvoted = false
    post.value.dislikeCount = Math.max(0, (snapshot.dislikeCount || 0) - 1)
  }
  try {
    const res = await postApi.like(props.id)
    if (res && typeof res === 'object') {
      if ('liked' in res) post.value.liked = !!res.liked
      if ('likeCount' in res) post.value.likeCount = res.likeCount
      if ('downvoted' in res) post.value.downvoted = !!res.downvoted
      if ('dislikeCount' in res) post.value.dislikeCount = res.dislikeCount
    }
  } catch (e) {
    post.value.liked = snapshot.liked
    post.value.likeCount = snapshot.likeCount
    post.value.downvoted = snapshot.downvoted
    post.value.dislikeCount = snapshot.dislikeCount
    toast(errMsg(e), 'error')
  } finally {
    likePending.value = false
  }
}

async function toggleDislike() {
  if (!auth.isLogin) return requireLogin()
  if (dislikePending.value || !post.value) return
  dislikePending.value = true
  const snapshot = {
    liked: post.value.liked,
    likeCount: post.value.likeCount,
    downvoted: post.value.downvoted,
    dislikeCount: post.value.dislikeCount
  }
  post.value.downvoted = !snapshot.downvoted
  post.value.dislikeCount = Math.max(0, (snapshot.dislikeCount || 0) + (post.value.downvoted ? 1 : -1))
  // 赞同与反对互斥：点反对需取消已有的赞同
  if (post.value.downvoted && snapshot.liked) {
    post.value.liked = false
    post.value.likeCount = Math.max(0, (snapshot.likeCount || 0) - 1)
  }
  try {
    const res = await postApi.dislike(props.id)
    if (res && typeof res === 'object') {
      if ('downvoted' in res) post.value.downvoted = !!res.downvoted
      if ('dislikeCount' in res) post.value.dislikeCount = res.dislikeCount
      if ('liked' in res) post.value.liked = !!res.liked
      if ('likeCount' in res) post.value.likeCount = res.likeCount
    }
  } catch (e) {
    post.value.liked = snapshot.liked
    post.value.likeCount = snapshot.likeCount
    post.value.downvoted = snapshot.downvoted
    post.value.dislikeCount = snapshot.dislikeCount
    toast(errMsg(e), 'error')
  } finally {
    dislikePending.value = false
  }
}

async function toggleBookmark() {
  if (!auth.isLogin) return requireLogin()
  if (bmPending.value || !post.value) return
  bmPending.value = true
  const snapshot = { bookmarked: post.value.bookmarked, bookmarkCount: post.value.bookmarkCount }
  post.value.bookmarked = !snapshot.bookmarked
  post.value.bookmarkCount = Math.max(0, snapshot.bookmarkCount + (post.value.bookmarked ? 1 : -1))
  try {
    const res = await postApi.bookmark(props.id)
    if (res && typeof res === 'object') {
      if ('bookmarked' in res) post.value.bookmarked = !!res.bookmarked
      if ('bookmarkCount' in res) post.value.bookmarkCount = res.bookmarkCount
    }
    toast(post.value.bookmarked ? '已收藏' : '已取消收藏', 'success')
  } catch (e) {
    post.value.bookmarked = snapshot.bookmarked
    post.value.bookmarkCount = snapshot.bookmarkCount
    toast(errMsg(e), 'error')
  } finally {
    bmPending.value = false
  }
}

async function toggleFollowAuthor() {
  if (!auth.isLogin) return requireLogin()
  const author = post.value?.author
  if (!author) return
  try {
    const res = await userApi.follow(author.id)
    author.followed = typeof res === 'object' ? !!res.followed : !author.followed
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

function submitComment() {
  const text = newComment.value.trim()
  if (!text) return
  if (!auth.isLogin) return requireLogin()
  doSubmit(text)
}

async function doSubmit(text) {
  submitting.value = true
  try {
    const payload = { content: text }
    if (replyTo.value) payload.parentId = replyTo.value.id
    const created = await postApi.addComment(props.id, payload)
    if (replyTo.value) {
      const parent = findComment(comments.value, replyTo.value.parentId || replyTo.value.id)
      const target = replyTo.value.parentId ? parent : replyTo.value
      if (target && !target.replies) target.replies = []
      if (target) target.replies.push(created)
      else comments.value.unshift(created)
      replyTo.value = null
    } else {
      comments.value.unshift(created)
    }
    newComment.value = ''
    post.value.commentCount = (post.value.commentCount || 0) + 1
    toast('评论成功', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    submitting.value = false
  }
}

function findComment(list, id) {
  for (const c of list) {
    if (c.id === id) return c
    if (c.replies) {
      const r = findComment(c.replies, id)
      if (r) return r
    }
  }
  return null
}

function startReply(c) {
  if (!auth.isLogin) return requireLogin()
  replyTo.value = c
  newComment.value = ''
}

function cancelReply() {
  replyTo.value = null
}

async function removePost() {
  if (!confirm('确定删除这篇内容吗？')) return
  try {
    await postApi.remove(props.id)
    toast('已删除', 'success')
    router.replace('/')
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(() => {
  loadPost()
  loadComments()
})

watch(() => props.id, () => {
  loadPost()
  loadComments()
})
</script>

<template>
  <div class="detail-layout">
    <div v-if="loading" class="loading card detail-loading"><span class="spinner"></span>加载中…</div>
    <div v-else-if="error" class="empty card">{{ error }}</div>

    <template v-else-if="post">
      <article class="detail-main card">
        <header class="dm-head">
          <h1 class="dm-title">
            <span v-if="post.type === 'QUESTION'" class="question-badge">问题</span>{{ post.title }}
          </h1>
          <div class="dm-meta">
            <router-link class="dm-author" :to="`/user/${post.author?.id}`">
              <Avatar :src="post.author?.avatar" :name="post.author?.nickname" :size="28" />
              <span>{{ post.author?.nickname || post.author?.username }}</span>
            </router-link>
            <span>{{ fromNow(post.createdAt) }}</span>
            <span v-if="post.viewCount != null">浏览 {{ post.viewCount }}</span>
            <template v-if="isAuthor">
              <router-link class="dm-edit" :to="`/editor/${post.id}`">编辑</router-link>
              <button class="dm-del" @click="removePost">删除</button>
            </template>
          </div>
          <div v-if="post.tags && post.tags.length" class="dm-tags">
            <router-link v-for="t in post.tags" :key="t" class="tag-badge" :to="`/?tag=${encodeURIComponent(t)}`">{{ t }}</router-link>
          </div>
        </header>

        <div class="md-body" v-html="contentHtml"></div>

        <footer class="dm-actions">
          <button class="act-btn" :class="{ active: post.liked }" @click="toggleLike">
            <svg viewBox="0 0 24 24" width="18" height="18" :fill="post.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
            赞同 {{ post.likeCount || 0 }}
          </button>
          <button class="act-btn" :class="{ active: post.downvoted }" @click="toggleDislike">
            <svg viewBox="0 0 24 24" width="18" height="18" :fill="post.downvoted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 14V2"/><path d="M9 18.12 10 14H4.17a2 2 0 0 1-1.92-2.56l2.33-8A2 2 0 0 1 6.5 2H20a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-2.76a2 2 0 0 0-1.79 1.11L12 22a3.13 3.13 0 0 1-3-3.88Z"/></svg>
            反对 {{ post.dislikeCount || 0 }}
          </button>
          <button class="act-btn" :class="{ active: post.bookmarked }" @click="toggleBookmark">
            <svg viewBox="0 0 24 24" width="18" height="18" :fill="post.bookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
            收藏 {{ post.bookmarkCount || 0 }}
          </button>
          <span class="dm-score" :class="{ 'pos': score > 0, 'neg': score < 0 }">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20"/><path d="m5 9 7-7 7 7"/></svg>
            认同 {{ score > 0 ? '+' : '' }}{{ score }}
          </span>
        </footer>
      </article>

      <section class="detail-side">
        <div class="author-card card">
          <router-link class="ac-head" :to="`/user/${post.author?.id}`">
            <Avatar :src="post.author?.avatar" :name="post.author?.nickname" :size="52" />
            <div class="ac-info">
              <div class="ac-nick">{{ post.author?.nickname || post.author?.username }}</div>
              <div class="ac-bio">{{ post.author?.bio || '这个作者很低调' }}</div>
            </div>
          </router-link>
          <button
            v-if="!isAuthor"
            class="btn-follow ac-follow"
            :class="{ 'is-followed': post.author?.followed }"
            @click="toggleFollowAuthor"
          >
            {{ post.author?.followed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </section>

      <section class="comment-section card">
        <h2 class="cs-title">全部评论 {{ post.commentCount ? `(${post.commentCount})` : '' }}</h2>

        <div class="cs-form">
          <p v-if="replyTo" class="cs-replying">
            回复 <b>@{{ replyTo.author?.nickname }}</b>
            <button @click="cancelReply">取消</button>
          </p>
          <textarea
            v-model="newComment"
            rows="3"
            :placeholder="auth.isLogin ? '友善的评论是交流的起点…' : '登录后参与讨论'"
            @focus="!auth.isLogin && requireLogin()"
          ></textarea>
          <div class="cs-form-foot">
            <button class="btn btn-primary" :disabled="submitting || !newComment.trim()" @click="submitComment">
              {{ submitting ? '提交中…' : '发表评论' }}
            </button>
          </div>
        </div>

        <div v-if="cLoading && !comments.length" class="loading"><span class="spinner"></span>加载评论…</div>
        <div v-else-if="!comments.length" class="empty">还没有评论，来抢沙发吧</div>
        <ul v-else class="cs-list">
          <li v-for="c in comments" :key="c.id" class="cs-item">
            <CommentItem :comment="c" @reply="startReply" />
            <ul v-if="c.replies && c.replies.length" class="cs-replies">
              <li v-for="r in c.replies" :key="r.id" class="cs-reply-item">
                <CommentItem :comment="r" @reply="startReply" />
              </li>
            </ul>
          </li>
        </ul>
        <button v-if="cPage + 1 < cTotalPages" class="btn btn-ghost cs-more" @click="loadMoreComments">加载更多评论</button>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  grid-template-areas:
    'main side'
    'comments comments';
  gap: 20px;
  align-items: start;
}

.detail-main {
  grid-area: main;
  padding: 28px 32px;
}

.detail-side {
  grid-area: side;
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}

.comment-section {
  grid-area: comments;
  padding: 22px 28px;
}

.detail-loading {
  grid-column: 1 / -1;
  padding: 60px;
  text-align: center;
}

.dm-title {
  font-size: 26px;
  line-height: 1.45;
  font-weight: 700;
}

.dm-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: var(--text-3);
}

.dm-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
  font-weight: 500;
}

.dm-author:hover {
  color: var(--primary);
}

.dm-edit {
  color: var(--primary);
}

.dm-del {
  color: var(--danger);
  font-size: 13px;
}

.dm-tags {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.md-body {
  margin-top: 22px;
}

.dm-actions {
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 14px;
}

.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 22px;
  border-radius: 20px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text-2);
  background: #fff;
  transition: all 0.15s;
}

.act-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.act-btn.active {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.dm-score {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-3);
}

.dm-score.pos {
  color: var(--primary);
}

.dm-score.neg {
  color: var(--danger);
}

.author-card {
  padding: 18px;
}

.ac-head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.ac-info {
  min-width: 0;
}

.ac-nick {
  font-weight: 600;
  font-size: 15px;
}

.ac-bio {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ac-follow {
  width: 100%;
  margin-top: 14px;
  padding: 7px 0;
}

.cs-title {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 16px;
}

.cs-form textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.7;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.cs-form textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(30, 128, 255, 0.12);
}

.cs-replying {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.cs-replying button {
  color: var(--primary);
  margin-left: 8px;
  font-size: 13px;
}

.cs-form-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  margin-bottom: 8px;
}

.cs-list {
  list-style: none;
}

.cs-item {
  padding: 14px 0;
  border-top: 1px solid var(--bg);
}

.cs-replies {
  list-style: none;
  margin: 6px 0 0 46px;
  padding-left: 14px;
  border-left: 2px solid var(--bg);
}

.cs-reply-item {
  padding: 8px 0;
}

.cs-more {
  width: 100%;
  margin-top: 12px;
}

@media (max-width: 992px) {
  .detail-layout {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas:
      'main'
      'side'
      'comments';
  }

  .detail-side {
    position: static;
  }

  .detail-main {
    padding: 20px 16px;
  }

  .comment-section {
    padding: 18px 16px;
  }

  .dm-title {
    font-size: 21px;
  }

  .cs-replies {
    margin-left: 20px;
  }
}
</style>
