import { ref } from 'vue'

const visible = ref(false)
const message = ref('Carregando...')

let activeCount = 0

export function showLoading(msg = 'Carregando...') {
  if (activeCount === 0) {
    message.value = msg
    visible.value = true
  }
  activeCount++
}

export function hideLoading() {
  activeCount = Math.max(0, activeCount - 1)
  if (activeCount === 0) visible.value = false
}

export function useLoading() {
  return { visible, message, showLoading, hideLoading }
}
