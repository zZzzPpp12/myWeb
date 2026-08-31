<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '@/components/Avatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'
import { boilingApi, topicApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'
import { fromNow } from '@/utils/time'
import { track } from '@/utils/analytics'

const router = useRouter()
const auth = useAuthStore()

/* ===================== 左侧导航 ===================== */
// 三种 feed 流：最新 / 热门 / 关注
const feeds = [
  { key: 'latest', label: '最新', icon: 'clock' },
  { key: 'hot', label: '热门', icon: 'fire' },
  { key: 'following', label: '关注', icon: 'user-plus' }
]

const feed = ref('latest')
const circleFilter = ref('') // 圈子过滤（空 = 不过滤）

/* ===================== 列表状态 ===================== */
const list = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = computed(() => page.value + 1 < totalPages.value)

/* ===================== 发布框状态 ===================== */
const content = ref('')
const imageUrl = ref('')
const showImgInput = ref(false) // 是否显示图片/链接输入框
const publishCircle = ref('') // 发布时选择的话题
const publishPending = ref(false)
const canPublish = computed(() => content.value.trim().length > 0 && content.value.length <= 1000)

/* ===================== 侧栏数据 ===================== */
const circles = ref([]) // 我的圈子（我关注的话题名）
const topics = ref([]) // 推荐话题列表
const featured = ref([]) // 精选沸点
const meStats = ref(null) // 登录用户详细统计（沸点数/关注/粉丝等）

// 发布框话题下拉选项：我的圈子 + 推荐话题去重合并
const circleOptions = computed(() => {
  const set = []
  const seen = new Set()
  for (const name of [...circles.value, ...topics.value.map((t) => t.name)]) {
    if (name && !seen.has(name)) {
      seen.add(name)
      set.push(name)
    }
  }
  return set
})

// 左栏「推荐圈子」取前 6
const recTopics = computed(() => topics.value.slice(0, 6))
// 右栏「推荐话题」取前 8
const rightTopics = computed(() => topics.value.slice(0, 8))

/* ===================== 数据加载 ===================== */
async function load(reset = true) {
  if (reset) {
    page.value = 0
    list.value = []
    loading.value = true
  }
  try {
    const params = { feed: feed.value, page: page.value, size: 20 }
    if (circleFilter.value) params.circle = circleFilter.value
    const res = await boilingApi.list(params)
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

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value += 1
  load(false)
}

function switchFeed(key) {
  if (key === 'following' && !auth.isLogin) {
    toast('登录后查看关注的沸点', 'error')
    router.push({ name: 'login', query: { redirect: '/boiling' } })
    return
  }
  if (feed.value === key && !circleFilter.value) return
  feed.value = key
  load()
}

// 我的圈子列表（需登录）
async function loadCircles() {
  if (!auth.isLogin) return
  try {
    circles.value = (await boilingApi.circles()) || []
  } catch (e) {
    circles.value = []
  }
}

// 推荐话题（左栏 + 右栏共用一次请求）
async function loadTopics() {
  try {
    topics.value = (await topicApi.list()) || []
  } catch (e) {
    topics.value = []
  }
}

// 精选沸点（右栏）
async function loadFeatured() {
  try {
    featured.value = (await boilingApi.featured(5)) || []
  } catch (e) {
    featured.value = []
  }
}

// 登录用户详细统计（沸点数 / 关注 / 粉丝）
async function loadMeStats() {
  if (!auth.isLogin || !auth.user) return
  try {
    meStats.value = await userApi.get(auth.user.id)
  } catch (e) {
    meStats.value = null
  }
}

// auth.user 异步恢复后（token 已有但 user 尚未拉取），补充加载统计
watch(
  () => auth.user,
  (u) => {
    if (u && !meStats.value) loadMeStats()
  }
)

// 登录态变化时刷新侧栏数据
watch(
  () => auth.isLogin,
  (v) => {
    if (v) {
      loadCircles()
      loadMeStats()
    } else {
      circles.value = []
      meStats.value = null
    }
  }
)

/* ===================== 圈子过滤 / 话题关注 ===================== */
// 点击圈子项：再点一次取消过滤
function toggleCircleFilter(name) {
  circleFilter.value = circleFilter.value === name ? '' : name
  load()
}

// 关注 / 取关推荐圈子（乐观更新）
async function followTopic(t) {
  if (!auth.isLogin) return requireLogin()
  const prev = t.followed
  t.followed = !prev
  try {
    const res = await topicApi.follow(t.name)
    if (res && typeof res === 'object' && 'followed' in res) t.followed = !!res.followed
    toast(t.followed ? `已关注话题「${t.name}」` : '已取消关注', 'success')
  } catch (e) {
    t.followed = prev
    toast(errMsg(e), 'error')
  }
}

/* ===================== 发布沸点 ===================== */
function requireLogin() {
  toast('请先登录', 'error')
  router.push({ name: 'login', query: { redirect: '/boiling' } })
}

async function publish() {
  if (!auth.isLogin) return requireLogin()
  const text = content.value.trim()
  if (!text) return
  if (text.length > 1000) {
    toast('内容不能超过 1000 字', 'error')
    return
  }
  publishPending.value = true
  try {
    const payload = { content: text }
    if (imageUrl.value.trim()) payload.imageUrl = imageUrl.value.trim()
    if (publishCircle.value) payload.circle = publishCircle.value
    await boilingApi.create(payload)
    content.value = ''
    imageUrl.value = ''
    showImgInput.value = false
    toast('发布成功', 'success')
    // 回到最新并刷新列表
    if (feed.value !== 'latest' || circleFilter.value) {
      feed.value = 'latest'
      circleFilter.value = ''
    }
    load()
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    publishPending.value = false
  }
}

// 插入 @ 文本，提示手动输入用户名
function insertAt() {
  content.value = (content.value || '') + '@'
  toast('在 @ 后输入用户名，被提及的用户会收到通知哦～', 'info')
}

/* ===================== 沸点卡片操作 ===================== */
function isAuthor(b) {
  return auth.user && b.author && b.author.id === auth.user.id
}

// 点赞 / 取消点赞（乐观更新 + 埋点）
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
    track('boiling_like', 'BOILING', b.id)
  } catch (e) {
    b.liked = prev.liked
    b.likeCount = prev.likeCount
    toast(errMsg(e), 'error')
  }
}

// 收藏 / 取消收藏（乐观更新 + 埋点）
async function toggleBookmark(b) {
  if (!auth.isLogin) return requireLogin()
  const prev = { bookmarked: b.bookmarked, bookmarkCount: b.bookmarkCount }
  b.bookmarked = !prev.bookmarked
  b.bookmarkCount = Math.max(0, (prev.bookmarkCount || 0) + (b.bookmarked ? 1 : -1))
  try {
    const res = await boilingApi.bookmark(b.id)
    if (res && typeof res === 'object' && 'bookmarked' in res) {
      b.bookmarked = !!res.bookmarked
      if ('bookmarkCount' in res) b.bookmarkCount = res.bookmarkCount
    }
    track('boiling_bookmark', 'BOILING', b.id)
    toast(b.bookmarked ? '已收藏' : '已取消收藏', 'success')
  } catch (e) {
    b.bookmarked = prev.bookmarked
    b.bookmarkCount = prev.bookmarkCount
    toast(errMsg(e), 'error')
  }
}

// 分享 / 转发：乐观计数 + 复制链接；剪贴板失败也算分享成功
async function shareBoiling(b) {
  const prev = b.shareCount || 0
  b.shareCount = prev + 1
  let copied = false
  try {
    await navigator.clipboard.writeText(location.origin + '/boiling')
    copied = true
  } catch (e) {
    // 剪贴板不可用时静默，仍按已分享处理
  }
  try {
    const res = await boilingApi.share(b.id)
    if (res && typeof res === 'object' && 'shareCount' in res) b.shareCount = res.shareCount
    track('boiling_share', 'BOILING', b.id)
  } catch (e) {
    b.shareCount = prev
    toast(errMsg(e), 'error')
    return
  }
  toast(copied ? '已复制链接' : '已分享', 'success')
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

// 举报沸点（prompt 输入理由）
async function reportBoiling(b) {
  if (!auth.isLogin) return requireLogin()
  const reason = prompt(`举报这条沸点，请输入举报理由：`)
  if (!reason || !reason.trim()) return
  try {
    await boilingApi.report('BOILING', b.id, reason.trim())
    toast('举报已提交，感谢你的反馈', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

/* ===================== 评论面板 ===================== */
// 展开 / 收起评论面板（首次展开时加载评论）
function toggleComments(b) {
  b.cOpen = !b.cOpen
  if (b.cOpen && !b.comments) loadComments(b, true)
}

// 加载评论列表（sort: default | latest | hot）
async function loadComments(b, reset = true) {
  if (reset) {
    b.commentPage = 0
    b.comments = []
  }
  b.commentLoading = true
  try {
    const res = await boilingApi.comments(b.id, {
      sort: b.commentSort || 'default',
      page: b.commentPage,
      size: 10
    })
    const arr = res.content || []
    b.comments = reset ? arr : b.comments.concat(arr)
    b.commentTotalPages = res.totalPages || 0
    b.commentPage = res.number || b.commentPage
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    b.commentLoading = false
  }
}

// 切换评论排序
function switchCommentSort(b, sort) {
  if ((b.commentSort || 'default') === sort) return
  b.commentSort = sort
  loadComments(b, true)
}

function hasMoreComments(b) {
  return (b.commentPage || 0) + 1 < (b.commentTotalPages || 0)
}

// 发布主评论 / 回复（带 parentId 时为回复）
async function submitComment(b) {
  if (!auth.isLogin) return requireLogin()
  const text = (b.commentText || '').trim()
  if (!text) return
  if (text.length > 1000) {
    toast('评论不能超过 1000 字', 'error')
    return
  }
  b.commentSending = true
  try {
    const res = await boilingApi.addComment(b.id, { content: text })
    b.comments = b.comments || []
    b.comments.unshift(res)
    b.commentText = ''
    b.commentCount = (b.commentCount || 0) + 1
    toast('评论成功', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    b.commentSending = false
  }
}

// 评论赞 / 踩投票（乐观更新，再次点击同向取消）
async function voteComment(c, up) {
  if (!auth.isLogin) return requireLogin()
  const prev = { myVote: c.myVote, likeCount: c.likeCount, dislikeCount: c.dislikeCount }
  const nextVote = c.myVote === up ? null : up
  let like = c.likeCount || 0
  let dislike = c.dislikeCount || 0
  if (c.myVote === true) like -= 1
  if (c.myVote === false) dislike -= 1
  if (nextVote === true) like += 1
  if (nextVote === false) dislike += 1
  c.myVote = nextVote
  c.likeCount = Math.max(0, like)
  c.dislikeCount = Math.max(0, dislike)
  try {
    const res = await boilingApi.voteComment(c.id, up)
    if (res && typeof res === 'object') {
      if ('myVote' in res) c.myVote = res.myVote === undefined ? nextVote : res.myVote
      if ('likeCount' in res) c.likeCount = res.likeCount
      if ('dislikeCount' in res) c.dislikeCount = res.dislikeCount
    }
  } catch (e) {
    c.myVote = prev.myVote
    c.likeCount = prev.likeCount
    c.dislikeCount = prev.dislikeCount
    toast(errMsg(e), 'error')
  }
}

// 展开 / 收起内联回复输入框（支持主评论与楼中楼回复）
function toggleReplyBox(b, target, parent) {
  target.replyOpen = !target.replyOpen
  if (target.replyOpen) {
    // 回复「楼中楼」时预填 @ 对方昵称，便于后端解析提及
    const at = target !== parent && target.author ? '@' + (target.author.nickname || target.author.username) + ' ' : ''
    target.replyText = target.replyOpen && !target.replyText ? at : target.replyText || ''
  }
}

// 提交回复：parentId 统一指向所属主评论 id
async function submitReply(b, parent, target) {
  if (!auth.isLogin) return requireLogin()
  const text = (target.replyText || '').trim()
  if (!text) return
  target.replySending = true
  try {
    const res = await boilingApi.addComment(b.id, { content: text, parentId: parent.id })
    parent.replies = parent.replies || []
    parent.replies.push(res)
    target.replyText = ''
    target.replyOpen = false
    b.commentCount = (b.commentCount || 0) + 1
    toast('回复成功', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    target.replySending = false
  }
}

// 举报评论（prompt 输入理由）
async function reportComment(c) {
  if (!auth.isLogin) return requireLogin()
  c.menuOpen = false
  const reason = prompt('举报这条评论，请输入举报理由：')
  if (!reason || !reason.trim()) return
  try {
    await boilingApi.report('BOILING_COMMENT', c.id, reason.trim())
    toast('举报已提交，感谢你的反馈', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

/* ===================== 渲染辅助 ===================== */
// @提及高亮：把内容拆分为普通文本 / @提及 两类片段
function mentionParts(text) {
  const t = String(text || '')
  const re = /@[\w\u4e00-\u9fa5]+/g
  const parts = []
  let last = 0
  let m
  while ((m = re.exec(t))) {
    if (m.index > last) parts.push({ type: 'text', text: t.slice(last, m.index) })
    parts.push({ type: 'mention', text: m[0] })
    last = m.index + m[0].length
  }
  if (last < t.length) parts.push({ type: 'text', text: t.slice(last) })
  return parts.length ? parts : [{ type: 'text', text: t }]
}

// 数字缩写：≥1000 显示 1.2k
function fmtCount(n) {
  const v = Number(n) || 0
  if (v >= 1000) return (v / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(v)
}

// 内容截断（精选沸点 40 字）
function clip(s, n = 40) {
  const t = String(s || '')
  return t.length > n ? t.slice(0, n) + '…' : t
}

// 右栏用户卡统计：优先接口详情，回退 auth.user 字段
function myStat(key) {
  return (meStats.value && meStats.value[key]) ?? auth.user?.[key] ?? 0
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/boiling' } })
}

/* ===================== 初始化 ===================== */
onMounted(() => {
  track('boiling_view') // 页面曝光埋点
  load()
  loadTopics()
  loadFeatured()
  loadCircles()
  loadMeStats()
})
</script>

<template>
  <div class="bp-layout">
    <!-- ========== 左侧导航栏 ========== -->
    <aside class="bp-side card">
      <nav class="bp-menu">
        <button
          v-for="f in feeds"
          :key="f.key"
          class="bp-menu-item"
          :class="{ active: feed === f.key }"
          @click="switchFeed(f.key)"
        >
          <!-- 最新：时钟 -->
          <svg v-if="f.icon === 'clock'" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
          <!-- 热门：火焰 -->
          <svg v-else-if="f.icon === 'fire'" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>
          <!-- 关注：用户+ -->
          <svg v-else viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/></svg>
          {{ f.label }}
        </button>
      </nav>

      <!-- 我的圈子 -->
      <div class="bp-section">
        <h4 class="bp-section-title">我的圈子</h4>
        <template v-if="auth.isLogin">
          <button
            v-for="name in circles"
            :key="name"
            class="bp-menu-item bp-circle-item"
            :class="{ active: circleFilter === name }"
            @click="toggleCircleFilter(name)"
          >
            # {{ name }}
          </button>
          <p v-if="!circles.length" class="bp-hint">关注话题后就会显示在这里</p>
        </template>
        <p v-else class="bp-hint">
          <a class="bp-link" @click="goLogin">登录</a> 后查看我的圈子
        </p>
      </div>

      <!-- 推荐圈子 -->
      <div class="bp-section" v-if="recTopics.length">
        <h4 class="bp-section-title">推荐圈子</h4>
        <div v-for="t in recTopics" :key="t.name" class="bp-topic-row">
          <button
            class="bp-topic-main"
            :class="{ active: circleFilter === t.name }"
            @click="toggleCircleFilter(t.name)"
          >
            {{ t.emoji }} {{ t.name }}
          </button>
          <button
            class="bp-topic-follow"
            :class="{ on: t.followed }"
            @click="followTopic(t)"
          >
            {{ t.followed ? '已关' : '+ 关注' }}
          </button>
        </div>
      </div>
    </aside>

    <!-- ========== 中间内容流 ========== -->
    <main class="bp-center">
      <!-- 移动端顶部横向 tab（≤768px 显示，替代左栏） -->
      <div class="bp-mtabs card">
        <button
          v-for="f in feeds"
          :key="f.key"
          class="bp-mtab"
          :class="{ active: feed === f.key }"
          @click="switchFeed(f.key)"
        >
          {{ f.label }}
        </button>
      </div>

      <!-- 发布框 -->
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
            maxlength="1000"
            placeholder="快和码友一起分享新鲜事！发布时 @用户 会被通知哦～"
          ></textarea>
          <!-- 图片 / 链接输入框（点击按钮切换显示） -->
          <input
            v-if="showImgInput"
            v-model="imageUrl"
            class="bp-img-input"
            type="text"
            placeholder="可选：图片 / 链接 URL"
          />
          <div class="bp-bar">
            <div class="bp-tools">
              <!-- 图片按钮 -->
              <button
                class="bp-tool"
                :class="{ on: showImgInput }"
                title="添加图片"
                @click="showImgInput = !showImgInput"
              >
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
              </button>
              <!-- 链接按钮 -->
              <button
                class="bp-tool"
                :class="{ on: showImgInput }"
                title="添加链接"
                @click="showImgInput = !showImgInput"
              >
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
              </button>
              <!-- 话题下拉 -->
              <select v-model="publishCircle" class="bp-circle-select" title="选择话题">
                <option value=""># 选择话题</option>
                <option v-for="c in circleOptions" :key="c" :value="c"># {{ c }}</option>
              </select>
              <!-- @ 提及 -->
              <button class="bp-tool" title="@ 用户" @click="insertAt">
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-4 8"/></svg>
              </button>
            </div>
            <div class="bp-pub-right">
              <span class="bp-count" :class="{ over: content.length > 1000 }">{{ content.length }}/1000</span>
              <button
                class="btn btn-primary bp-pub-btn"
                :class="{ dim: !publishCircle }"
                :disabled="!canPublish || publishPending"
                @click="publish"
              >
                {{ publishPending ? '发布中…' : '发布' }}
              </button>
            </div>
          </div>
        </template>
      </section>

      <!-- 沸点列表 -->
      <div v-if="loading" class="loading card"><span class="spinner"></span>加载中…</div>
      <template v-else>
        <article v-for="b in list" :key="b.id" class="bp-item card">
          <!-- 头部：头像 + 昵称 + 等级 + 在线状态 + 时间 + 圈子 -->
          <div class="bp-head">
            <router-link :to="`/user/${b.author?.id}`">
              <Avatar :src="b.author?.avatar" :name="b.author?.nickname || b.author?.username" :size="40" />
            </router-link>
            <div class="bp-head-info">
              <div class="bp-head-line">
                <router-link class="bp-nick" :to="`/user/${b.author?.id}`">{{ b.author?.nickname || b.author?.username }}</router-link>
                <LevelBadge :level="b.author?.level" :level-name="b.author?.levelName" size="sm" />
                <span v-if="b.author?.online" class="bp-online">在线</span>
              </div>
              <div class="bp-meta">
                <span class="bp-time">{{ fromNow(b.createdAt) }}</span>
                <button
                  v-if="b.circle"
                  class="bp-circle-badge"
                  :class="{ on: circleFilter === b.circle }"
                  @click="toggleCircleFilter(b.circle)"
                >
                  # {{ b.circle }}
                </button>
              </div>
            </div>
          </div>

          <!-- 内容：@提及高亮 -->
          <p class="bp-content">
            <template v-for="(seg, i) in mentionParts(b.content)" :key="i">
              <span v-if="seg.type === 'mention'" class="bp-mention">{{ seg.text }}</span>
              <template v-else>{{ seg.text }}</template>
            </template>
          </p>

          <!-- 可选图片 -->
          <img
            v-if="b.imageUrl"
            class="bp-image"
            :src="b.imageUrl"
            alt="沸点图片"
            @error="$event.target.style.display = 'none'"
          />

          <!-- 底部操作栏：三等分主操作 + 右侧次级操作 -->
          <div class="bp-foot">
            <div class="bp-ops">
              <button class="bp-op" @click="shareBoiling(b)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.59 13.51 6.83 3.98M15.41 6.51l-6.82 3.98"/></svg>
                分享 <b v-if="b.shareCount" class="bp-op-num">{{ b.shareCount }}</b>
              </button>
              <button class="bp-op" :class="{ on: b.cOpen }" @click="toggleComments(b)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
                评论 <b class="bp-op-num">{{ b.commentCount || 0 }}</b>
              </button>
              <button class="bp-op like" :class="{ active: b.liked }" @click="toggleLike(b)">
                <svg viewBox="0 0 24 24" width="16" height="16" :fill="b.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
                <b class="bp-op-num">{{ b.likeCount || 0 }}</b>
              </button>
            </div>
            <div class="bp-sub">
              <!-- 收藏 -->
              <button class="bp-subop" :class="{ bookmark: b.bookmarked }" @click="toggleBookmark(b)">
                <svg viewBox="0 0 24 24" width="15" height="15" :fill="b.bookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
                <b v-if="b.bookmarkCount" class="bp-op-num">{{ b.bookmarkCount }}</b>
              </button>
              <!-- 转发（与分享合并调 share） -->
              <button class="bp-subop" @click="shareBoiling(b)">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/></svg>
              </button>
              <!-- 举报 -->
              <button class="bp-subop" title="举报" @click="reportBoiling(b)">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><path d="M4 22v-7"/></svg>
              </button>
              <!-- 作者本人可删除 -->
              <button v-if="isAuthor(b)" class="bp-subop del" title="删除" @click="removeBoiling(b)">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
          </div>

          <!-- 评论面板（卡片内展开） -->
          <Transition name="bp-slide">
            <div v-if="b.cOpen" class="bp-comments">
              <!-- 排序 tabs -->
              <div class="bp-c-tabs">
                <button
                  v-for="s in [{ k: 'default', t: '默认' }, { k: 'latest', t: '最新' }, { k: 'hot', t: '最热' }]"
                  :key="s.k"
                  class="bp-c-tab"
                  :class="{ active: (b.commentSort || 'default') === s.k }"
                  @click="switchCommentSort(b, s.k)"
                >
                  {{ s.t }}
                </button>
              </div>

              <!-- 评论输入框 -->
              <div v-if="auth.isLogin" class="bp-c-input-row">
                <textarea
                  v-model="b.commentText"
                  class="bp-c-input"
                  rows="2"
                  maxlength="1000"
                  placeholder="友善评论，理性表达～"
                ></textarea>
                <button
                  class="btn btn-primary bp-c-send"
                  :disabled="!(b.commentText || '').trim() || b.commentSending"
                  @click="submitComment(b)"
                >
                  {{ b.commentSending ? '发送中…' : '发布' }}
                </button>
              </div>
              <p v-else class="bp-hint bp-c-login">
                <a class="bp-link" @click="goLogin">登录</a> 后参与评论
              </p>

              <!-- 评论列表 -->
              <div v-if="b.commentLoading" class="bp-c-loading"><span class="spinner"></span>加载中…</div>
              <template v-else>
                <div v-if="!b.comments || !b.comments.length" class="bp-c-empty">还没有评论，来抢沙发～</div>
                <div v-for="c in b.comments" :key="c.id" class="bp-c">
                  <router-link :to="`/user/${c.author?.id}`">
                    <Avatar :src="c.author?.avatar" :name="c.author?.nickname || c.author?.username" :size="34" />
                  </router-link>
                  <div class="bp-c-main">
                    <div class="bp-c-head">
                      <router-link class="bp-c-nick" :to="`/user/${c.author?.id}`">{{ c.author?.nickname || c.author?.username }}</router-link>
                      <LevelBadge :level="c.author?.level" :level-name="c.author?.levelName" size="sm" />
                    </div>
                    <p class="bp-c-content">{{ c.content }}</p>
                    <div class="bp-c-foot">
                      <span class="bp-c-time">{{ fromNow(c.createdAt) }}</span>
                      <button class="bp-c-act" @click="toggleReplyBox(b, c, c)">回复</button>
                      <div class="bp-c-more-wrap">
                        <button class="bp-c-act" @click="c.menuOpen = !c.menuOpen">···</button>
                        <!-- ··· 菜单：举报 -->
                        <div v-if="c.menuOpen" class="bp-c-menu">
                          <button @click="reportComment(c)">举报</button>
                        </div>
                      </div>
                    </div>

                    <!-- 内联回复输入框 -->
                    <div v-if="c.replyOpen" class="bp-c-replybox">
                      <input
                        v-model="c.replyText"
                        class="bp-c-reply-input"
                        :placeholder="`回复 @${c.author?.nickname || c.author?.username}：`"
                        maxlength="1000"
                        @keyup.enter="submitReply(b, c, c)"
                      />
                      <button
                        class="btn btn-primary bp-c-send sm"
                        :disabled="!(c.replyText || '').trim() || c.replySending"
                        @click="submitReply(b, c, c)"
                      >
                        回复
                      </button>
                    </div>

                    <!-- 楼中楼回复列表（缩进缩窄） -->
                    <div v-if="c.replies && c.replies.length" class="bp-c-replies">
                      <div v-for="r in c.replies" :key="r.id" class="bp-c-reply">
                        <router-link :to="`/user/${r.author?.id}`">
                          <Avatar :src="r.author?.avatar" :name="r.author?.nickname || r.author?.username" :size="24" />
                        </router-link>
                        <div class="bp-c-main">
                          <div class="bp-c-head">
                            <router-link class="bp-c-nick sm" :to="`/user/${r.author?.id}`">{{ r.author?.nickname || r.author?.username }}</router-link>
                            <span v-if="r.replyToUser" class="bp-c-replyto">
                              回复 <b>@{{ r.replyToUser.nickname || r.replyToUser.username }}</b>
                            </span>
                          </div>
                          <p class="bp-c-content sm">{{ r.content }}</p>
                          <div class="bp-c-foot">
                            <span class="bp-c-time">{{ fromNow(r.createdAt) }}</span>
                            <button class="bp-c-act" @click="toggleReplyBox(b, r, c)">回复</button>
                            <button class="bp-c-act" @click="reportComment(r)">···</button>
                          </div>
                          <!-- 楼中楼内联回复框 -->
                          <div v-if="r.replyOpen" class="bp-c-replybox">
                            <input
                              v-model="r.replyText"
                              class="bp-c-reply-input"
                              :placeholder="`回复 @${r.author?.nickname || r.author?.username}：`"
                              maxlength="1000"
                              @keyup.enter="submitReply(b, c, r)"
                            />
                            <button
                              class="btn btn-primary bp-c-send sm"
                              :disabled="!(r.replyText || '').trim() || r.replySending"
                              @click="submitReply(b, c, r)"
                            >
                              回复
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 赞 / 踩 -->
                  <div class="bp-c-vote">
                    <button :class="{ on: c.myVote === true }" title="赞" @click="voteComment(c, true)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
                      {{ c.likeCount || 0 }}
                    </button>
                    <button :class="{ on: c.myVote === false }" title="踩" @click="voteComment(c, false)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" style="transform: rotate(180deg)"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"/></svg>
                      {{ c.dislikeCount || 0 }}
                    </button>
                  </div>
                </div>
                <!-- 评论分页加载更多 -->
                <button
                  v-if="hasMoreComments(b)"
                  class="btn btn-ghost bp-c-more-btn"
                  :disabled="b.commentLoading"
                  @click="b.commentPage += 1; loadComments(b, false)"
                >
                  加载更多评论
                </button>
              </template>
            </div>
          </Transition>
        </article>

        <div v-if="!list.length" class="empty card">
          {{ circleFilter ? `「# ${circleFilter}」圈子下还没有沸点` : feed === 'following' ? '关注的作者还没有发布沸点' : '还没有沸点，来发布第一条吧' }}
        </div>
        <button v-if="hasMore" class="btn btn-ghost bp-more" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="spinner"></span>{{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </template>
    </main>

    <!-- ========== 右侧栏（≤1200px 隐藏） ========== -->
    <aside class="bp-right">
      <!-- 用户卡 -->
      <section class="card bp-panel">
        <template v-if="!auth.isLogin">
          <div class="bp-u-guest">
            <Avatar :src="''" name="?" :size="52" />
            <p class="bp-u-links">
              <router-link class="bp-link" :to="{ name: 'login', query: { redirect: '/boiling' } }">登录</router-link>
              <span class="bp-u-sep">/</span>
              <router-link class="bp-link" :to="{ name: 'login', query: { redirect: '/boiling' } }">注册</router-link>
            </p>
            <p class="bp-u-slogan">即刻玩转沸点</p>
          </div>
        </template>
        <template v-else-if="auth.user">
          <router-link class="bp-u-head" :to="`/user/${auth.user.id}`">
            <Avatar :src="auth.user.avatar" :name="auth.user.nickname || auth.user.username" :size="52" />
            <div class="bp-u-info">
              <span class="bp-u-nick">{{ auth.user.nickname || auth.user.username }}</span>
              <LevelBadge :level="auth.user.level" :level-name="auth.user.levelName" size="sm" />
            </div>
          </router-link>
          <div class="bp-u-stats">
            <div class="bp-u-stat"><b>{{ myStat('boilingsCount') }}</b><span>沸点</span></div>
            <div class="bp-u-stat"><b>{{ circles.length }}</b><span>圈子</span></div>
            <div class="bp-u-stat"><b>{{ myStat('followingCount') }}</b><span>关注</span></div>
            <div class="bp-u-stat"><b>{{ myStat('followersCount') }}</b><span>粉丝</span></div>
          </div>
        </template>
      </section>

      <!-- 精选沸点 -->
      <section class="card bp-panel">
        <h4 class="bp-panel-title">精选沸点</h4>
        <div v-if="!featured.length" class="bp-hint">暂无精选内容</div>
        <ul v-else class="bp-feat-list">
          <li v-for="f in featured" :key="f.id" class="bp-feat-item">
            <p class="bp-feat-text">{{ clip(f.content, 40) }}</p>
            <span class="bp-feat-meta">{{ f.likeCount || 0 }}赞 · {{ f.commentCount || 0 }}评论</span>
          </li>
        </ul>
      </section>

      <!-- 推荐话题 -->
      <section class="card bp-panel" v-if="rightTopics.length">
        <h4 class="bp-panel-title">推荐话题</h4>
        <ul class="bp-topic-list">
          <li v-for="t in rightTopics" :key="t.name">
            <router-link class="bp-topic-link" :to="`/topic/${encodeURIComponent(t.name)}`">
              <span class="bp-topic-emoji">{{ t.emoji }}</span>
              <span class="bp-topic-name">{{ t.name }}</span>
              <span class="bp-topic-count">{{ fmtCount(t.postCount) }}</span>
            </router-link>
          </li>
        </ul>
      </section>
    </aside>
  </div>
</template>

<style scoped>
/* ===================== 三栏布局 ===================== */
.bp-layout {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

/* 左侧导航栏（240px，滚动时吸顶） */
.bp-side {
  width: 240px;
  flex-shrink: 0;
  padding: 12px;
  position: sticky;
  top: calc(var(--nav-h) + 20px);
  max-height: calc(100vh - var(--nav-h) - 40px);
  overflow-y: auto;
}

/* 中间内容流 */
.bp-center {
  flex: 1;
  min-width: 0;
}

/* 右侧栏（300px） */
.bp-right {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}

/* ===================== 左侧菜单 ===================== */
.bp-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bp-menu-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-2);
  text-align: left;
  transition: all 0.15s;
}

.bp-menu-item:hover {
  background: var(--bg);
  color: var(--text);
}

.bp-menu-item.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.bp-section {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.bp-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-3);
  padding: 0 12px 6px;
}

.bp-circle-item {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bp-topic-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 6px 2px 0;
}

.bp-topic-main {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-2);
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all 0.15s;
}

.bp-topic-main:hover {
  background: var(--bg);
  color: var(--text);
}

.bp-topic-main.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.bp-topic-follow {
  flex-shrink: 0;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid var(--primary);
  color: var(--primary);
  transition: all 0.15s;
}

.bp-topic-follow:hover {
  background: var(--primary-light);
}

.bp-topic-follow.on {
  border-color: var(--border);
  color: var(--text-3);
}

.bp-hint {
  font-size: 12px;
  color: var(--text-3);
  padding: 4px 12px;
  line-height: 1.6;
}

.bp-link {
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
}

.bp-link:hover {
  color: var(--primary-hover);
}

/* ===================== 移动端 tab（默认隐藏） ===================== */
.bp-mtabs {
  display: none;
  padding: 0 12px;
  margin-bottom: 12px;
  gap: 4px;
}

.bp-mtab {
  flex: 1;
  padding: 11px 0;
  font-size: 14px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
}

.bp-mtab.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}

/* ===================== 发布框 ===================== */
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

.bp-img-input:focus {
  border-color: var(--primary);
}

.bp-bar {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.bp-tools {
  display: flex;
  align-items: center;
  gap: 6px;
}

.bp-tool {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  transition: all 0.15s;
}

.bp-tool:hover,
.bp-tool.on {
  background: var(--primary-light);
  color: var(--primary);
}

.bp-circle-select {
  max-width: 130px;
  padding: 5px 6px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-2);
  background: #fff;
  transition: border-color 0.15s;
}

.bp-circle-select:focus {
  border-color: var(--primary);
}

.bp-pub-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bp-count {
  font-size: 12px;
  color: var(--text-3);
}

.bp-count.over {
  color: var(--danger);
}

/* 发布按钮：未选话题时半透明，选中后实蓝 */
.bp-pub-btn.dim {
  opacity: 0.5;
}

.bp-pub-btn.dim:hover:not(:disabled) {
  opacity: 0.75;
  background: var(--primary);
}

/* ===================== 沸点卡片 ===================== */
.bp-item {
  padding: 16px 18px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s;
}

.bp-item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.bp-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.bp-head-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bp-head-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bp-nick {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.bp-nick:hover {
  color: var(--primary);
}

/* 在线状态绿点 */
.bp-online {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--success);
}

.bp-online::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success);
}

.bp-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bp-time {
  font-size: 12px;
  color: var(--text-3);
}

/* 圈子徽章（#circle，可点击过滤） */
.bp-circle-badge {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--primary-light);
  color: var(--primary);
  transition: all 0.15s;
}

.bp-circle-badge:hover,
.bp-circle-badge.on {
  background: #d6e9ff;
}

.bp-content {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* @提及高亮 */
.bp-mention {
  color: var(--primary);
  font-weight: 500;
}

.bp-image {
  margin-top: 12px;
  max-width: 100%;
  border-radius: 8px;
}

/* ===================== 底部操作栏 ===================== */
.bp-foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 主操作三等分（分享 / 评论 / 点赞） */
.bp-ops {
  flex: 1;
  display: flex;
  min-width: 0;
}

.bp-op {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 7px 0;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-3);
  transition: all 0.15s;
}

.bp-op:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.bp-op.like.active {
  color: var(--primary);
  font-weight: 600;
}

.bp-op.on {
  color: var(--primary);
}

.bp-op-num {
  font-weight: 600;
}

/* 右侧次级操作（收藏 / 转发 / 举报 / 删除） */
.bp-sub {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.bp-subop {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-3);
  transition: all 0.15s;
}

.bp-subop:hover {
  background: var(--bg);
  color: var(--text);
}

.bp-subop.bookmark {
  color: #f7b500;
}

.bp-subop.del:hover {
  color: var(--danger);
}

/* ===================== 评论面板 ===================== */
.bp-comments {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

/* 展开动画：淡入 + 上移 */
.bp-slide-enter-active,
.bp-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.bp-slide-enter-from,
.bp-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.bp-c-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.bp-c-tab {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--text-3);
  background: var(--bg);
  transition: all 0.15s;
}

.bp-c-tab:hover {
  color: var(--primary);
}

.bp-c-tab.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.bp-c-input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-bottom: 12px;
}

.bp-c-input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  resize: vertical;
  font-size: 13px;
  line-height: 1.6;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.bp-c-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(30, 128, 255, 0.12);
}

.bp-c-send {
  flex-shrink: 0;
  padding: 7px 16px;
  font-size: 13px;
}

.bp-c-send.sm {
  padding: 4px 12px;
  font-size: 12px;
}

.bp-c-login {
  margin-bottom: 12px;
}

.bp-c-loading {
  padding: 16px 0;
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
}

.bp-c-empty {
  padding: 16px 0;
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
}

/* 单条评论 */
.bp-c {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--bg);
}

.bp-c-main {
  flex: 1;
  min-width: 0;
}

.bp-c-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bp-c-nick {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.bp-c-nick.sm {
  font-size: 12px;
}

.bp-c-nick:hover {
  color: var(--primary);
}

.bp-c-content {
  margin-top: 3px;
  font-size: 13.5px;
  color: var(--text);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.bp-c-content.sm {
  font-size: 12.5px;
}

.bp-c-foot {
  margin-top: 3px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.bp-c-time {
  font-size: 11px;
  color: var(--text-3);
}

.bp-c-act {
  font-size: 11px;
  color: var(--text-3);
  transition: color 0.15s;
}

.bp-c-act:hover {
  color: var(--primary);
}

/* ··· 菜单（举报） */
.bp-c-more-wrap {
  position: relative;
}

.bp-c-menu {
  position: absolute;
  right: 0;
  top: 100%;
  z-index: 10;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 72px;
  overflow: hidden;
}

.bp-c-menu button {
  display: block;
  width: 100%;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--text-2);
  text-align: left;
}

.bp-c-menu button:hover {
  background: var(--bg);
  color: var(--danger);
}

/* 内联回复输入框 */
.bp-c-replybox {
  margin-top: 6px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.bp-c-reply-input {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  transition: border-color 0.15s;
}

.bp-c-reply-input:focus {
  border-color: var(--primary);
}

/* 楼中楼回复（缩进缩窄） */
.bp-c-replies {
  margin-top: 8px;
  margin-left: 14px;
  padding-left: 12px;
  border-left: 2px solid var(--bg);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bp-c-reply {
  display: flex;
  gap: 8px;
}

.bp-c-replyto {
  font-size: 12px;
  color: var(--text-3);
}

.bp-c-replyto b {
  color: var(--primary);
  font-weight: 500;
}

/* 赞 / 踩竖排 */
.bp-c-vote {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding-top: 2px;
}

.bp-c-vote button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text-3);
  padding: 3px 6px;
  border-radius: 6px;
  transition: all 0.15s;
}

.bp-c-vote button:hover {
  background: var(--bg);
  color: var(--primary);
}

.bp-c-vote button.on {
  color: var(--primary);
  font-weight: 600;
}

.bp-c-more-btn {
  width: 100%;
  margin-top: 10px;
  padding: 8px 0;
  font-size: 13px;
}

/* ===================== 右侧栏面板 ===================== */
.bp-panel {
  padding: 16px;
}

.bp-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 10px;
}

/* 用户卡：未登录 */
.bp-u-guest {
  text-align: center;
  padding: 6px 0;
}

.bp-u-links {
  margin-top: 10px;
  font-size: 15px;
}

.bp-u-sep {
  color: var(--text-3);
  margin: 0 6px;
}

.bp-u-slogan {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}

/* 用户卡：已登录 */
.bp-u-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bp-u-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}

.bp-u-nick {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bp-u-head:hover .bp-u-nick {
  color: var(--primary);
}

.bp-u-stats {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
}

.bp-u-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bp-u-stat b {
  font-size: 15px;
  color: var(--text);
}

.bp-u-stat span {
  font-size: 11px;
  color: var(--text-3);
}

/* 精选沸点 */
.bp-feat-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.bp-feat-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--bg);
}

.bp-feat-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.bp-feat-text {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
}

.bp-feat-meta {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-3);
}

/* 推荐话题 */
.bp-topic-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bp-topic-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 6px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-2);
  transition: all 0.15s;
}

.bp-topic-link:hover {
  background: var(--bg);
  color: var(--primary);
}

.bp-topic-emoji {
  flex-shrink: 0;
}

.bp-topic-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bp-topic-count {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-3);
}

/* 加载更多 */
.bp-more {
  width: 100%;
  padding: 12px 0;
}

/* ===================== 响应式 ===================== */
/* ≤1200px 隐藏右栏 */
@media (max-width: 1200px) {
  .bp-right {
    display: none;
  }
}

/* ≤768px 隐藏左栏，显示顶部横向 tab */
@media (max-width: 768px) {
  .bp-side {
    display: none;
  }

  .bp-mtabs {
    display: flex;
  }

  .bp-publish,
  .bp-item {
    padding: 14px;
  }

  /* 窄屏下主操作与次级操作换行 */
  .bp-foot {
    flex-wrap: wrap;
  }

  .bp-ops {
    width: 100%;
  }

  .bp-sub {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
