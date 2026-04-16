import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './msw/server.js'

// ── uuid() ────────────────────────────────────────────────────────────────
describe('uuid()', () => {
  it('returns a valid UUID v4 string', async () => {
    const { uuid } = await import('../api/client.js')
    const id = uuid()
    expect(typeof id).toBe('string')
    // RFC 4122 UUID v4 pattern
    expect(id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    )
  })

  it('generates unique values on each call', async () => {
    const { uuid } = await import('../api/client.js')
    const ids = new Set([uuid(), uuid(), uuid(), uuid(), uuid()])
    expect(ids.size).toBe(5)
  })

  it('falls back gracefully when crypto.randomUUID is unavailable', async () => {
    const { uuid } = await import('../api/client.js')
    const original = crypto.randomUUID
    try {
      crypto.randomUUID = undefined
      const id = uuid()
      expect(id).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
      )
    } finally {
      crypto.randomUUID = original
    }
  })
})

// ── Axios client interceptor ──────────────────────────────────────────────
describe('client interceptor', () => {
  let client

  beforeEach(async () => {
    localStorage.clear()
    // Fresh import per test group
    const mod = await import('../api/client.js')
    client = mod.default
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('attaches X-Session-Token header when token is in localStorage', async () => {
    const token = 'test-session-token-abc123'
    localStorage.setItem('sessionToken', token)

    // Axios request interceptors run LIFO, so we invoke the auth interceptor directly
    // rather than stacking another interceptor on top.
    const authHandler = client.interceptors.request.handlers.find(h => h !== null)?.fulfilled
    const fakeConfig = { headers: {}, method: 'get' }
    const result = authHandler(fakeConfig)

    expect(result.headers['X-Session-Token']).toBe(token)
  })

  it('does NOT attach X-Session-Token header when no token in localStorage', async () => {
    localStorage.removeItem('sessionToken')

    const authHandler = client.interceptors.request.handlers.find(h => h !== null)?.fulfilled
    const fakeConfig = { headers: {}, method: 'get' }
    const result = authHandler(fakeConfig)

    expect(result.headers['X-Session-Token']).toBeUndefined()
  })

  it('attaches X-CSRF-Token on POST when csrfToken is in localStorage', async () => {
    localStorage.setItem('csrfToken', 'csrf-val-abc')
    const authHandler = client.interceptors.request.handlers.find(h => h !== null)?.fulfilled
    const fakeConfig = { headers: {}, method: 'post' }
    const result = authHandler(fakeConfig)
    expect(result.headers['X-CSRF-Token']).toBe('csrf-val-abc')
  })

  it('does NOT attach X-CSRF-Token on GET even with csrfToken in localStorage', async () => {
    localStorage.setItem('csrfToken', 'csrf-val-abc')
    const authHandler = client.interceptors.request.handlers.find(h => h !== null)?.fulfilled
    const fakeConfig = { headers: {}, method: 'get' }
    const result = authHandler(fakeConfig)
    expect(result.headers['X-CSRF-Token']).toBeUndefined()
  })
})

// ── Response interceptor ──────────────────────────────────────────────────
describe('client response interceptor', () => {
  let client

  beforeEach(async () => {
    localStorage.clear()
    const mod = await import('../api/client.js')
    client = mod.default
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('returns raw body when response has no data envelope key', async () => {
    server.use(http.get('http://localhost/api/health', () =>
      HttpResponse.json({ status: 'raw-body' })
    ))
    const result = await client.get('/health')
    expect(result).toEqual({ status: 'raw-body' })
  })

  it('rejects with the original error when error has no response (network error)', async () => {
    server.use(http.get('http://localhost/api/health', () => HttpResponse.error()))
    await expect(client.get('/health')).rejects.toBeDefined()
  })

  it('clears sessionToken and csrfToken from localStorage on 401 response', async () => {
    server.use(http.get('http://localhost/api/health', () =>
      HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
    ))
    localStorage.setItem('sessionToken', 'old-tok')
    localStorage.setItem('csrfToken', 'old-csrf')
    await expect(client.get('/health')).rejects.toBeDefined()
    expect(localStorage.getItem('sessionToken')).toBeNull()
    expect(localStorage.getItem('csrfToken')).toBeNull()
  })
})
