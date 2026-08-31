<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 40 }
})

const initial = computed(() => {
  const n = (props.name || '?').trim()
  return n ? n[0].toUpperCase() : '?'
})

const style = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  fontSize: Math.max(12, Math.round(props.size * 0.42)) + 'px'
}))
</script>

<template>
  <span class="avatar" :style="style">
    <img v-if="src" :src="src" :alt="name" @error="$event.target.style.display = 'none'" />
    <span v-else>{{ initial }}</span>
  </span>
</template>
