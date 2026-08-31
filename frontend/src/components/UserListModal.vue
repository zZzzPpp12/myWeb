<script setup>
import { ref, watch } from 'vue'
import Avatar from './Avatar.vue'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: [Number, String], default: null },
  mode: { type: String, default: 'following' } // following | followers
})
const emit = defineEmits(['update:modelValue'])

const auth = useAuthStore()
const users = ref([])
const loading = ref(false)
const error = ref('')

const title = () => (props.mode === 'following' ? '关注列表' : '粉丝列表')

async function load() {
  if (!props.userId) return
  loading.value = true
  error.value = ''
  try {
    const fn = props.mode === 'following' ? userApi.following : userApi.followers
    const page = await fn(props.userId, 0, 50)
    users.value = page.content || []
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.mode, props.userId],
  ([v]) => {
    if (v) load()
  }
)

async function toggleFollow(u) {
  if (!auth.isLogin) {
    toast('请先登录', 'error')
    return
  }
  if (auth.user && u.id === auth.user.id) return
  try {
    const res = await userApi.follow(u.id)
    const followed = typeof res === 'object' ? !!res.followed : !u.followed
    u.followed = followed
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="ulm-mask" @click.self="close">
      <div class="ulm-panel card">
        <div class="ulm-head">
          <h3>{{ title() }}</h3>
          <button class="ulm-close" @click="close">✕</button>
        </div>
        <div class="ulm-body">
          <div v-if="loading" class="loading"><span class="spinner"></span>加载中…</div>
          <div v-else-if="error" class="empty">{{ error }}</div>
          <div v-else-if="!users.length" class="empty">暂无用户</div>
          <ul v-else class="ulm-list">
            <li v-for="u in users" :key="u.id">
              <router-link class="ulm-user" :to="`/user/${u.id}`" @click="close">
                <Avatar :src="u.avatar" :name="u.nickname || u.username" :size="40" />
                <span class="ulm-info">
                  <span class="ulm-nick">{{ u.nickname || u.username }}</span>
                  <span class="ulm-bio">{{ u.bio || '@' + u.username }}</span>
                </span>
              </router-link>
              <button
                v-if="!auth.user || u.id !== auth.user.id"
                class="btn-follow"
                :class="{ 'is-followed': u.followed }"
                @click="toggleFollow(u)"
              >
                {{ u.followed ? '已关注' : '关注' }}
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.ulm-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.ulm-panel {
  width: 100%;
  max-width: 420px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.ulm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
}

.ulm-head h3 {
  font-size: 16px;
}

.ulm-close {
  font-size: 16px;
  color: var(--text-3);
  padding: 4px 8px;
}

.ulm-body {
  overflow-y: auto;
  padding: 6px 0;
}

.ulm-list {
  list-style: none;
}

.ulm-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
}

.ulm-list li:hover {
  background: var(--bg);
}

.ulm-user {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.ulm-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ulm-nick {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ulm-bio {
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
