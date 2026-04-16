<template>
  <div class="app-root">
    <!-- Auth pages: no shell -->
    <router-view v-if="isPublicRoute" />

    <!-- Authenticated shell -->
    <template v-else>
      <aside class="sidebar">
        <div class="sidebar-logo">
          <span class="logo-icon">⬡</span>
          <span class="logo-text">RescueHub</span>
        </div>

        <nav class="sidebar-nav">
          <div class="nav-section-label">Operations</div>
          <router-link to="/" class="nav-link">
            <span class="nav-icon">◈</span> Dashboard
          </router-link>
          <router-link v-if="can('FRONT_DESK','CLINICIAN','ADMIN')" to="/patients" class="nav-link">
            <span class="nav-icon">◉</span> Patients
          </router-link>
          <router-link to="/visits" class="nav-link">
            <span class="nav-icon">◫</span> Visits
          </router-link>
          <router-link to="/appointments" class="nav-link">
            <span class="nav-icon">◷</span> Appointments
          </router-link>
          <router-link to="/incidents" class="nav-link">
            <span class="nav-icon">◬</span> Incidents
          </router-link>
          <router-link to="/bulletins" class="nav-link">
            <span class="nav-icon">◫</span> Bulletins
          </router-link>
          <router-link to="/search" class="nav-link">
            <span class="nav-icon">◎</span> Search
          </router-link>
          <router-link v-if="can('MODERATOR','ADMIN')" to="/ranking" class="nav-link">
            <span class="nav-icon">★</span> Ranking
          </router-link>

          <template v-if="can('BILLING','ADMIN')">
            <div class="nav-section-label">Finance</div>
            <router-link to="/billing" class="nav-link">
              <span class="nav-icon">◈</span> Billing
            </router-link>
            <router-link to="/daily-close" class="nav-link">
              <span class="nav-icon">◉</span> Daily Close
            </router-link>
            <router-link to="/exports" class="nav-link">
              <span class="nav-icon">◫</span> Exports
            </router-link>
          </template>

          <template v-if="can('QUALITY','ADMIN')">
            <div class="nav-section-label">Quality</div>
            <router-link to="/quality" class="nav-link">
              <span class="nav-icon">◈</span> QA Results
            </router-link>
            <router-link to="/sampling" class="nav-link">
              <span class="nav-icon">◉</span> Sampling
            </router-link>
          </template>

          <template v-if="can('ADMIN')">
            <div class="nav-section-label">Admin</div>
            <router-link to="/backups" class="nav-link">
              <span class="nav-icon">◈</span> Backups
            </router-link>
            <router-link to="/audit" class="nav-link">
              <span class="nav-icon">◉</span> Audit Log
            </router-link>
            <router-link to="/admin/users" class="nav-link">
              <span class="nav-icon">◫</span> Users
            </router-link>
          </template>
        </nav>

        <div class="sidebar-footer">
          <span class="mono text-muted" style="font-size:10px;">v1.0</span>
        </div>
      </aside>

      <div class="main-area">
        <!-- Top bar -->
        <header class="topbar">
          <div class="topbar-left">
            <span class="route-label mono">{{ currentRouteName }}</span>
          </div>
          <div class="topbar-right">
            <span class="user-pill">
              <span class="user-role badge badge-amber">{{ session.user?.role }}</span>
              <span class="user-name">{{ session.user?.displayName || session.user?.username }}</span>
            </span>
            <button class="btn btn-ghost btn-sm" @click="doLogout">Logout</button>
          </div>
        </header>

        <main class="content-area">
          <router-view />
        </main>
      </div>
    </template>

    <!-- Toast container -->
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="t in toastStore.toasts"
          :key="t.id"
          class="toast"
          :class="`toast-${t.type}`"
          @click="toastStore.remove(t.id)"
        >
          <span class="toast-dot" />
          {{ t.message }}
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from './stores/session.js'
import { useToastStore } from './stores/toast.js'

const session = useSessionStore()
const toastStore = useToastStore()
const route = useRoute()
const router = useRouter()

const isPublicRoute = computed(() => route.meta.public === true)

const currentRouteName = computed(() => route.name || 'RescueHub')

function can(...roles) {
  if (!session.user) return false
  return roles.includes(session.user.role)
}

async function doLogout() {
  await session.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-root {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ── Sidebar ──────────────────────────────────────── */
.sidebar {
  width: var(--sidebar-w);
  min-width: var(--sidebar-w);
  background: var(--surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  z-index: 10;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 8px;
}

.logo-icon {
  color: var(--amber);
  font-size: 20px;
  line-height: 1;
}

.logo-text {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 15px;
  letter-spacing: -0.02em;
}

.sidebar-nav {
  flex: 1;
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.nav-section-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-dim);
  padding: 12px 18px 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  border-left: 2px solid transparent;
  transition: all 0.15s;
}

.nav-link:hover {
  color: var(--text);
  background: var(--surface-2);
  text-decoration: none;
}

.nav-link.router-link-active,
.nav-link.router-link-exact-active {
  color: var(--amber);
  border-left-color: var(--amber);
  background: var(--amber-glow);
}

.nav-icon {
  font-size: 11px;
  opacity: 0.7;
  width: 14px;
  text-align: center;
}

.sidebar-footer {
  padding: 12px 18px;
  border-top: 1px solid var(--border);
}

/* ── Main area ────────────────────────────────────── */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── Topbar ───────────────────────────────────────── */
.topbar {
  height: var(--topbar-h);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.topbar-left { display: flex; align-items: center; gap: 12px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }

.route-label {
  font-size: 11px;
  color: var(--text-dim);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text);
}

/* ── Content ──────────────────────────────────────── */
.content-area {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ── Toasts ───────────────────────────────────────── */
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 280px;
  max-width: 400px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  font-family: var(--font-mono);
  font-size: 12px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  cursor: pointer;
  box-shadow: 0 4px 24px rgba(0,0,0,0.4);
}

.toast-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.toast-success { border-color: var(--green); }
.toast-success .toast-dot { background: var(--green); }

.toast-error { border-color: var(--red); }
.toast-error .toast-dot { background: var(--red); }

.toast-warn { border-color: var(--amber); }
.toast-warn .toast-dot { background: var(--amber); }

.toast-info .toast-dot { background: var(--blue); }

/* toast transition */
.toast-enter-active, .toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
