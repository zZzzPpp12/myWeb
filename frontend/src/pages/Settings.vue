<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '@/components/Avatar.vue'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'

const auth = useAuthStore()

const nickname = ref('')
const avatar = ref('')
const bio = ref('')
const saving = ref(false)

onMounted(() => {
  const u = auth.user || {}
  nickname.value = u.nickname || ''
  avatar.value = u.avatar || ''
  bio.value = u.bio || ''
})

async function save() {
  if (!nickname.value.trim()) {
    toast('昵称不能为空', 'error')
    return
  }
  saving.value = true
  try {
    const updated = await userApi.updateProfile({
      nickname: nickname.value.trim(),
      avatar: avatar.value.trim(),
      bio: bio.value.trim()
    })
    auth.setUser(updated && updated.id ? updated : { ...auth.user, nickname: nickname.value.trim(), avatar: avatar.value.trim(), bio: bio.value.trim() })
    toast('保存成功', 'success')
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="settings-page card">
    <h1>账号设置</h1>
    <div class="st-avatar-row">
      <Avatar :src="avatar" :name="nickname" :size="64" />
      <p class="st-avatar-hint">当前头像（在下方填写头像图片 URL 更换）</p>
    </div>

    <div class="field">
      <label>昵称</label>
      <input v-model="nickname" maxlength="30" placeholder="请输入昵称" />
    </div>
    <div class="field">
      <label>头像地址</label>
      <input v-model="avatar" placeholder="https://example.com/avatar.png" />
    </div>
    <div class="field">
      <label>个人简介</label>
      <textarea v-model="bio" rows="3" maxlength="200" placeholder="一句话介绍自己（最多 200 字）"></textarea>
    </div>

    <button class="btn btn-primary st-save" :disabled="saving" @click="save">
      {{ saving ? '保存中…' : '保存修改' }}
    </button>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 26px 30px;
}

.settings-page h1 {
  font-size: 19px;
  margin-bottom: 20px;
}

.st-avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.st-avatar-hint {
  font-size: 12px;
  color: var(--text-3);
}

.st-save {
  width: 100%;
  padding: 11px 0;
  margin-top: 6px;
}
</style>
