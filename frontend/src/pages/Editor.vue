<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '@/api'
import { errMsg } from '@/api/http'
import { toast } from '@/utils/toast'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({ id: { type: String, default: '' } })
const router = useRouter()

const isEdit = computed(() => !!props.id)
const title = ref('')
const content = ref('')
const type = ref('ARTICLE')
const tags = ref([])
const tagInput = ref('')
const saving = ref(false)
const loading = ref(false)

const previewHtml = computed(() => renderMarkdown(content.value))

function addTag() {
  const t = tagInput.value.trim().replace(/^#/, '')
  if (!t) return
  if (tags.value.length >= 5) {
    toast('最多添加 5 个标签', 'error')
    return
  }
  if (!tags.value.includes(t)) tags.value.push(t)
  tagInput.value = ''
}

function onTagKeydown(e) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    addTag()
  } else if (e.key === 'Backspace' && !tagInput.value && tags.value.length) {
    tags.value.pop()
  }
}

function removeTag(t) {
  tags.value = tags.value.filter((x) => x !== t)
}

async function loadForEdit() {
  loading.value = true
  try {
    const p = await postApi.detail(props.id)
    title.value = p.title || ''
    content.value = p.content || ''
    type.value = p.type || 'ARTICLE'
    tags.value = Array.isArray(p.tags) ? [...p.tags] : []
  } catch (e) {
    toast(errMsg(e), 'error')
    router.back()
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!title.value.trim()) {
    toast('请填写标题', 'error')
    return
  }
  if (!content.value.trim()) {
    toast('请填写正文内容', 'error')
    return
  }
  if (tagInput.value.trim()) addTag()
  saving.value = true
  const payload = {
    title: title.value.trim(),
    content: content.value,
    type: type.value,
    tags: tags.value
  }
  try {
    if (isEdit.value) {
      await postApi.update(props.id, payload)
      toast('更新成功', 'success')
      router.replace(`/post/${props.id}`)
    } else {
      const created = await postApi.create(payload)
      toast('发布成功', 'success')
      router.replace(created?.id ? `/post/${created.id}` : '/')
    }
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (isEdit.value) loadForEdit()
})

watch(() => props.id, (v) => {
  if (v) loadForEdit()
})
</script>

<template>
  <div class="editor-page card">
    <div v-if="loading" class="loading"><span class="spinner"></span>加载草稿…</div>
    <template v-else>
      <div class="ed-head">
        <h1>{{ isEdit ? '编辑内容' : '创作中心' }}</h1>
        <div class="ed-type">
          <label class="ed-radio">
            <input v-model="type" type="radio" value="ARTICLE" /> 文章
          </label>
          <label class="ed-radio">
            <input v-model="type" type="radio" value="QUESTION" /> 问题
          </label>
        </div>
      </div>

      <input v-model="title" class="ed-title" maxlength="120" placeholder="请输入标题（3-120 字）" />

      <div class="ed-tags">
        <span v-for="t in tags" :key="t" class="tag-badge ed-tag">
          {{ t }}<button class="ed-tag-x" @click="removeTag(t)">✕</button>
        </span>
        <input
          v-model="tagInput"
          class="ed-tag-input"
          placeholder="输入标签后回车，最多 5 个"
          @keydown="onTagKeydown"
          @blur="addTag"
        />
      </div>

      <div class="ed-panes">
        <div class="ed-pane">
          <div class="ed-pane-title">Markdown 编辑</div>
          <textarea
            v-model="content"
            class="ed-content"
            placeholder="支持标准 Markdown 语法，代码块请使用 ``` 包裹并标注语言…"
            spellcheck="false"
          ></textarea>
        </div>
        <div class="ed-pane">
          <div class="ed-pane-title">实时预览</div>
          <div class="ed-preview md-body">
            <div v-if="!content.trim()" class="side-empty">预览区域：开始输入内容即可实时查看渲染效果</div>
            <div v-else v-html="previewHtml"></div>
          </div>
        </div>
      </div>

      <div class="ed-foot">
        <span class="ed-count">{{ content.length }} 字</span>
        <div class="ed-foot-actions">
          <button class="btn btn-ghost" @click="router.back()">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="submit">
            {{ saving ? '提交中…' : isEdit ? '保存修改' : '发布' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.editor-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 28px;
}

.ed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.ed-head h1 {
  font-size: 20px;
}

.ed-type {
  display: flex;
  gap: 16px;
}

.ed-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
}

.ed-radio input {
  accent-color: var(--primary);
}

.ed-title {
  width: 100%;
  border: none;
  border-bottom: 1px solid var(--border);
  padding: 10px 2px;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
}

.ed-title:focus {
  border-bottom-color: var(--primary);
}

.ed-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.ed-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.ed-tag-x {
  font-size: 10px;
  color: var(--primary);
  opacity: 0.7;
}

.ed-tag-input {
  border: 1px dashed var(--border);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  width: 180px;
  line-height: 20px;
}

.ed-tag-input:focus {
  border-color: var(--primary);
}

.ed-panes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  border-top: 1px solid var(--border);
  padding-top: 16px;
}

.ed-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ed-pane-title {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 8px;
}

.ed-content {
  width: 100%;
  min-height: 420px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
}

.ed-content:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(30, 128, 255, 0.12);
}

.ed-preview {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px 18px;
  min-height: 420px;
  max-height: 70vh;
  overflow-y: auto;
  background: #fbfcfd;
}

.ed-foot {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ed-count {
  font-size: 13px;
  color: var(--text-3);
}

.ed-foot-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .editor-page {
    padding: 16px 14px;
  }

  .ed-panes {
    grid-template-columns: 1fr;
  }

  .ed-content,
  .ed-preview {
    min-height: 300px;
  }
}
</style>
