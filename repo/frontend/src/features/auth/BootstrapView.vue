<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import client from '../../api/client.js'
import { useSessionStore } from '../../stores/session.js'
import { invalidateBootstrapCache } from '../../router/index.js'

const router = useRouter()
const session = useSessionStore()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const displayName = ref('')
const organizationName = ref('')
const submitting = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value.trim()) { error.value = 'Username required'; return }
  if (password.value.length < 8) { error.value = 'Password must be at least 8 characters'; return }
  if (password.value !== confirmPassword.value) { error.value = 'Passwords do not match'; return }

  submitting.value = true
  try {
    const resp = await client.post('/bootstrap', {
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
      displayName: displayName.value || null,
      organizationName: organizationName.value || null,
    })
    const { sessionToken, csrfToken, user: u } = resp
    localStorage.setItem('sessionToken', sessionToken)
    if (csrfToken) localStorage.setItem('csrfToken', csrfToken)
    session.token = sessionToken
    if (u) session.user = u
    else await session.loadMe()
    invalidateBootstrapCache()
    router.push('/')
  } catch (e) {
    error.value = e.message || 'Bootstrap failed'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="bootstrap">
    <h1>System Setup</h1>
    <p>No users exist yet. Create the first administrator account to initialize the system.</p>
    <form @submit.prevent="submit">
      <label>Admin username<input v-model="username" required autocomplete="username" /></label>
      <label>Password<input v-model="password" type="password" required minlength="8" autocomplete="new-password" /></label>
      <label>Confirm password<input v-model="confirmPassword" type="password" required autocomplete="new-password" /></label>
      <label>Display name (optional)<input v-model="displayName" /></label>
      <label>Organization name (optional)<input v-model="organizationName" placeholder="Default Org" /></label>
      <button :disabled="submitting" type="submit">{{ submitting ? 'Creating…' : 'Initialize system' }}</button>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </div>
</template>

<style scoped>
.bootstrap { max-width: 440px; margin: 4rem auto; padding: 2rem; border: 1px solid #444; border-radius: 6px; }
label { display: block; margin-bottom: 0.75rem; font-size: 0.9rem; }
input { display: block; width: 100%; padding: 0.5rem; margin-top: 0.25rem; }
button { padding: 0.6rem 1.2rem; margin-top: 0.5rem; }
.error { color: #d9534f; margin-top: 0.5rem; }
</style>
