/**
 * Unit tests for the Vue Router beforeEach guard in src/router/index.js.
 *
 * Strategy:
 *   - vi.resetModules() before each test so module-level state
 *     (bootstrapChecked, systemInitialized) starts fresh.
 *   - MSW handlers control what /bootstrap/status returns.
 *   - session store is seeded via Pinia directly.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { server } from './msw/server.js'
import { http, HttpResponse } from 'msw'

async function loadRouter() {
  // Re-apply setup.js patches on the freshly-reset client module so MSW
  // (which needs an absolute baseURL in jsdom) can intercept API calls.
  const { default: freshClient } = await import('../api/client.js')
  freshClient.defaults.adapter = 'fetch'
  freshClient.defaults.baseURL = 'http://localhost/api'
  const mod = await import('../router/index.js')
  return { router: mod.default, invalidate: mod.invalidateBootstrapCache }
}

// Resolve a navigation and return the final route path
async function navigate(router, path) {
  await router.push(path)
  await router.isReady()
  return router.currentRoute.value.path
}

beforeEach(async () => {
  setActivePinia(createPinia())
  vi.resetModules()
})

describe('router guard — uninitialized system', () => {
  it('redirects any route to /bootstrap when system is not initialized', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: false } })
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/patients')
    expect(path).toBe('/bootstrap')
  })

  it('allows /bootstrap when system is not initialized', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: false } })
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/bootstrap')
    expect(path).toBe('/bootstrap')
  })
})

describe('router guard — initialized system, no token', () => {
  it('redirects /bootstrap to /login when initialized', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: true } })
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/bootstrap')
    expect(path).toBe('/login')
  })

  it('redirects protected route to /login when unauthenticated', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: true } })
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/patients')
    expect(path).toBe('/login')
  })

  it('allows /login when unauthenticated', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: true } })
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/login')
    expect(path).toBe('/login')
  })
})

describe('router guard — authenticated user, role checks', () => {
  async function buildRouter(role) {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.json({ data: { initialized: true } })
      ),
      http.get('http://localhost/api/auth/me', () =>
        HttpResponse.json({ data: { id: 1, username: 'testuser', role } })
      )
    )
    // Seed the session store with a token so the guard skips the login redirect
    const sessionMod = await import('../stores/session.js')
    const session = sessionMod.useSessionStore()
    session.token = 'test-token'
    session.user = { id: 1, username: 'testuser', role }
    const { router } = await loadRouter()
    return router
  }

  it('ADMIN can access /patients', async () => {
    const router = await buildRouter('ADMIN')
    const path = await navigate(router, '/patients')
    expect(path).toBe('/patients')
  })

  it('ADMIN can access /admin/users', async () => {
    const router = await buildRouter('ADMIN')
    const path = await navigate(router, '/admin/users')
    expect(path).toBe('/admin/users')
  })

  it('ADMIN can access /billing', async () => {
    const router = await buildRouter('ADMIN')
    const path = await navigate(router, '/billing')
    expect(path).toBe('/billing')
  })

  it('CLINICIAN visiting /billing is redirected to /', async () => {
    const router = await buildRouter('CLINICIAN')
    const path = await navigate(router, '/billing')
    expect(path).toBe('/')
  })

  it('BILLING visiting /admin/users is redirected to /', async () => {
    const router = await buildRouter('BILLING')
    const path = await navigate(router, '/admin/users')
    expect(path).toBe('/')
  })

  it('FRONT_DESK visiting /quality is redirected to /', async () => {
    const router = await buildRouter('FRONT_DESK')
    const path = await navigate(router, '/quality')
    expect(path).toBe('/')
  })

  it('MODERATOR visiting /billing is redirected to /', async () => {
    const router = await buildRouter('MODERATOR')
    const path = await navigate(router, '/billing')
    expect(path).toBe('/')
  })

  it('QUALITY visiting /backups is redirected to /', async () => {
    const router = await buildRouter('QUALITY')
    const path = await navigate(router, '/backups')
    expect(path).toBe('/')
  })
})

describe('router guard — bootstrap status error', () => {
  it('treats bootstrap error as initialized (fail open) and redirects to /login when no token', async () => {
    server.use(
      http.get('http://localhost/api/bootstrap/status', () =>
        HttpResponse.error()
      )
    )
    const { router } = await loadRouter()
    const path = await navigate(router, '/patients')
    // fail open: system treated as initialized, unauthenticated user → /login
    expect(path).toBe('/login')
  })
})
