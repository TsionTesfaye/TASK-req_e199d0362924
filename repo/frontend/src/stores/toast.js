import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])
  let _id = 0

  function add(message, type = 'info', duration = 4000) {
    const id = ++_id
    toasts.value.push({ id, message, type })
    setTimeout(() => remove(id), duration)
  }

  function success(msg) { add(msg, 'success') }
  function error(msg) { add(msg, 'error', 6000) }
  function info(msg) { add(msg, 'info') }
  function warn(msg) { add(msg, 'warn') }

  function remove(id) {
    const idx = toasts.value.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  return { toasts, success, error, info, warn, remove }
})
