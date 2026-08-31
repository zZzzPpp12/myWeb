let container = null

export function toast(msg, type = 'info') {
  if (!container) {
    container = document.createElement('div')
    container.className = 'toast-container'
    document.body.appendChild(container)
  }
  const el = document.createElement('div')
  el.className = `toast toast-${type}`
  el.textContent = msg
  container.appendChild(el)
  requestAnimationFrame(() => el.classList.add('show'))
  setTimeout(() => {
    el.classList.remove('show')
    setTimeout(() => el.remove(), 300)
  }, 2400)
}
