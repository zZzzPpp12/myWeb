<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref('login') // login | register
const form = ref({ username: '', password: '', nickname: '' })
const loading = ref(false)
const error = ref('')

const isLogin = computed(() => mode.value === 'login')
const redirect = computed(() => route.query.redirect || '/')

function switchMode(m) {
  mode.value = m
  error.value = ''
}

async function submit() {
  error.value = ''
  const { username, password, nickname } = form.value
  if (!username.trim() || !password) {
    error.value = '请填写用户名和密码'
    return
  }
  if (!isLogin.value && !nickname.trim()) {
    error.value = '请填写昵称'
    return
  }
  loading.value = true
  try {
    if (isLogin.value) {
      await auth.login({ username: username.trim(), password })
    } else {
      await auth.register({ username: username.trim(), password, nickname: nickname.trim() })
    }
    toast(isLogin.value ? '登录成功' : '注册成功，欢迎加入', 'success')
    router.replace(redirect.value)
  } catch (e) {
    error.value = errMsg(e, isLogin.value ? '登录失败' : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-brand">
        <span class="lb-mark">M</span>
        <h1>码社区</h1>
        <p>与百万开发者一起，发现更好的技术内容</p>
      </div>

      <div class="login-tabs">
        <button :class="{ active: isLogin }" @click="switchMode('login')">登录</button>
        <button :class="{ active: !isLogin }" @click="switchMode('register')">注册</button>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div v-if="!isLogin" class="field">
          <label>昵称</label>
          <input v-model="form.nickname" type="text" placeholder="请输入昵称" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" :placeholder="isLogin ? '请输入密码' : '至少 6 位密码'" autocomplete="current-password" />
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="btn btn-primary login-submit" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ isLogin ? '登 录' : '注 册' }}
        </button>
      </form>

      <p v-if="isLogin" class="login-demo">演示账号：alice / 123456</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: calc(100vh - var(--nav-h));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 36px 36px 28px;
}

.login-brand {
  text-align: center;
  margin-bottom: 24px;
}

.lb-mark {
  display: inline-flex;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--primary);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

.login-brand h1 {
  font-size: 22px;
  margin-bottom: 4px;
}

.login-brand p {
  font-size: 13px;
  color: var(--text-3);
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  margin-bottom: 22px;
}

.login-tabs button {
  flex: 1;
  padding: 10px 0;
  font-size: 15px;
  color: var(--text-2);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.15s;
}

.login-tabs button.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}

.login-submit {
  width: 100%;
  padding: 11px 0;
  font-size: 15px;
  margin-top: 4px;
}

.login-error {
  color: var(--danger);
  font-size: 13px;
  margin-bottom: 12px;
}

.login-demo {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-3);
  background: var(--bg);
  border-radius: 6px;
  padding: 8px;
}
</style>
