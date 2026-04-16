<template>
  <div class="login-page">
    <!-- Decorative grid lines -->
    <div class="grid-overlay" aria-hidden="true" />

    <div class="login-card fade-in">
      <div class="login-brand">
        <span class="brand-icon">⬡</span>
        <span class="brand-name">RescueHub</span>
      </div>
      <p class="login-tagline mono">Emergency Operations Platform</p>

      <form @submit.prevent="submit" class="login-form">
        <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>

        <div class="field">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="your.username"
            autocomplete="username"
            required
          />
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            required
          />
        </div>

        <button type="submit" class="btn btn-primary login-btn" :disabled="loading">
          <span v-if="loading" class="spinner-sm" />
          {{ loading ? 'Authenticating...' : 'Login' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../../stores/session.js'

const session = useSessionStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    await session.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message || 'Login failed. Check credentials.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(245,158,11,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(245,158,11,0.04) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 40px 36px;
  position: relative;
  z-index: 1;
  box-shadow: 0 0 60px rgba(245,158,11,0.05), 0 20px 40px rgba(0,0,0,0.4);
}

/* Top amber accent line */
.login-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--amber), transparent);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.brand-icon {
  font-size: 28px;
  color: var(--amber);
}

.brand-name {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.login-tagline {
  font-size: 11px;
  color: var(--text-dim);
  letter-spacing: 0.06em;
  margin-bottom: 28px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-error {
  background: var(--red-dim);
  border: 1px solid var(--red);
  border-radius: var(--radius);
  padding: 10px 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--red);
}

.login-btn {
  width: 100%;
  justify-content: center;
  padding: 11px;
  font-size: 13px;
  margin-top: 4px;
}

.spinner-sm {
  display: inline-block;
  width: 13px;
  height: 13px;
  border: 2px solid rgba(0,0,0,0.3);
  border-top-color: #000;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
</style>
