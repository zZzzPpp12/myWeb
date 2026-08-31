<script setup>
import Avatar from './Avatar.vue'
import { fromNow } from '@/utils/time'

defineProps({
  comment: { type: Object, required: true }
})
const emit = defineEmits(['reply'])
</script>

<template>
  <div class="ci">
    <router-link :to="`/user/${comment.author?.id}`">
      <Avatar :src="comment.author?.avatar" :name="comment.author?.nickname" :size="36" />
    </router-link>
    <div class="ci-body">
      <div class="ci-head">
        <router-link class="ci-nick" :to="`/user/${comment.author?.id}`">{{ comment.author?.nickname || comment.author?.username }}</router-link>
        <span class="ci-time">{{ fromNow(comment.createdAt) }}</span>
      </div>
      <p class="ci-content">{{ comment.content }}</p>
      <button class="ci-reply" @click="emit('reply', comment)">回复</button>
    </div>
  </div>
</template>

<style scoped>
.ci {
  display: flex;
  gap: 12px;
}

.ci-body {
  flex: 1;
  min-width: 0;
}

.ci-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ci-nick {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-2);
}

.ci-nick:hover {
  color: var(--primary);
}

.ci-time {
  font-size: 12px;
  color: var(--text-3);
}

.ci-content {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
}

.ci-reply {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}

.ci-reply:hover {
  color: var(--primary);
}
</style>
