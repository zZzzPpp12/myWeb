import { marked } from 'marked'
import hljs from 'highlight.js/lib/common'
import DOMPurify from 'dompurify'

marked.setOptions({
  gfm: true,
  breaks: true
})

const renderer = new marked.Renderer()
renderer.code = (code, lang) => {
  let html
  if (lang && hljs.getLanguage(lang)) {
    try {
      html = hljs.highlight(code, { language: lang }).value
    } catch (e) {
      html = code.replace(/</g, '&lt;')
    }
  } else {
    try {
      html = hljs.highlightAuto(code).value
    } catch (e) {
      html = code.replace(/</g, '&lt;')
    }
  }
  const cls = `hljs${lang ? ` language-${lang}` : ''}`
  return `<pre><code class="${cls}">${html}</code></pre>`
}
marked.use({ renderer })

export function renderMarkdown(src) {
  if (!src) return ''
  const raw = marked.parse(String(src))
  return DOMPurify.sanitize(raw, { ADD_ATTR: ['target'] })
}
