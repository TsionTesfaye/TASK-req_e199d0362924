import { defineStore } from 'pinia'
import { ref } from 'vue'
import client from '../api/client.js'

export const useSessionStore = defineStore('session', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('sessionToken') || null)

  async function login(username, password) {
    const res = await client.post('/auth/login', { username, password })
    const { sessionToken, csrfToken, user: u } = res
    token.value = sessionToken
    user.value = u
    localStorage.setItem('sessionToken', sessionToken)
    if (csrfToken) localStorage.setItem('csrfToken', csrfToken)
  }

  async function logout() {
    try {
      await client.post('/auth/logout')
    } catch (_) {
      // best-effort
    }
    token.value = null
    user.value = null
    localStorage.removeItem('sessionToken')
    localStorage.removeItem('csrfToken')
  }

  async function loadMe() {
    if (!token.value) return
    try {
      const res = await client.get('/auth/me')
      user.value = res
    } catch (_) {
      token.value = null
      user.value = null
      localStorage.removeItem('sessionToken')
    }
  }

  function hasRole(...roles) {
    if (!user.value) return false
    return roles.includes(user.value.role)
  }

  return { user, token, login, logout, loadMe, hasRole }
})
