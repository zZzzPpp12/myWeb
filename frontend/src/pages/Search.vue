<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import PostCard from '@/components/PostCard.vue'
import { searchApi } from '@/api'
import { errMsg } from '@/api/http'

const route = useRoute()
const q = ref(route.query.q || '')
const posts = ref([])
const page = ref(0)
const totalPages = ref(0)
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load(reset = true) {
  if (!q.value) {
    posts.value = []
    return
  }
  if (reset) {
    page.value = 0
    posts.value = []
  }
  loading.value = true
  error.value = ''
  try {
    const res = await searchApi.search({ q: q.value, page: page.value, size: 20 })
    posts.value = reset ? res.content || [] : posts.value.concat(res.content || [])
    totalPages.value = res.totalPages || 0
    total.value = res.totalElements || 0
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  load(false)
}

onMounted(load)

watch(() => route.query.q, (v) => {
  q.value = v || ''
  load()
})
</script>

<template>
  <div class="search-page">
    <div class="sp-head card">
      <template v-if="q">
        搜索「<b>{{ q }}</b>」
        <span v-if="!loading && !error" class="sp-count">共 {{ total }} 条结果</span>
      </template>
      <template v-else>请输入关键词开始搜索</template>
    </div>

    <div v-if="loading" class="loading card"><span class="spinner"></span>搜索中…</div>
    <div v-else-if="error" class="empty card">{{ error }}</div>
    <template v-else>
      <PostCard v-for="p in posts" :key="p.id" :post="p" />
      <div v-if="q && !posts.length" class="empty card">没有找到相关内容，换个关键词试试</div>
      <button v-if="page + 1 < totalPages" class="btn btn-ghost sp-more" @click="loadMore">加载更多</button>
    </template>
  </div>
</template>

<style scoped>
.search-page {
  max-width: 760px;
  margin: 0 auto;
}

.sp-head {
  padding: 14px 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text-2);
}

.sp-head b {
  color: var(--primary);
}

.sp-count {
  margin-left: 10px;
  color: var(--text-3);
  font-size: 13px;
}

.sp-more {
  width: 100%;
  padding: 12px 0;
}
</style>
