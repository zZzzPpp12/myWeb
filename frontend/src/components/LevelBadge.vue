<script setup>
import { computed } from 'vue'

// 声望等级徽章：level 1-6 对应 见习/新锐/进阶/资深/专家/大师
const props = defineProps({
  level: { type: Number, default: 0 },
  levelName: { type: String, default: '' },
  size: { type: String, default: 'md' } // sm | md
})

const show = computed(() => !!props.level && props.level > 0)

// 将等级归一到 1-6，用不同颜色区分
const cls = computed(() => {
  const lv = Math.min(6, Math.max(1, Number(props.level) || 1))
  return `lv-${lv}`
})
</script>

<template>
  <span v-if="show" class="level-badge" :class="[cls, `lb-${size}`]">
    Lv{{ level }}<template v-if="levelName"> {{ levelName }}</template>
  </span>
</template>

<style scoped>
.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border-radius: 4px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  vertical-align: middle;
}

.lb-sm {
  font-size: 11px;
  padding: 2px 5px;
}

.lb-md {
  font-size: 12px;
  padding: 3px 7px;
}

.lv-1 {
  background: #eef0f3;
  color: #8a919f;
}

.lv-2 {
  background: #e6f7f0;
  color: #00b07f;
}

.lv-3 {
  background: var(--primary-light);
  color: var(--primary);
}

.lv-4 {
  background: #f3ecff;
  color: #7b5cf0;
}

.lv-5 {
  background: #fff3e5;
  color: #ff7d00;
}

.lv-6 {
  background: #ffeceb;
  color: #f0413f;
}
</style>