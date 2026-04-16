/**
 * Tests for Pinia stores (session + toast).
 *
 * These stores are partially exercised via component tests (login, logout,
 * toast.success/error) but a few paths — session.loadMe(), toast.warn(),
 * toast.info(), toast.remove() — are not reachable that way.
 *
 * MSW is active via the global setup.js.
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from './msw/server.js'
import { flushPromises } from '@vue/test-utils'

// ── sessionStore ─────────────────────────────────────────────────────────────

describe('sessionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('loadMe — sets user when token exists and API succeeds', async () => {
    localStorage.setItem('sessionToken', 'tok-abc')
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.token = 'tok-abc'

    await session.loadMe()
    await flushPromises()

    expect(session.user).not.toBeNull()
    expect(session.user.username).toBe('admin')
  })

  it('loadMe — does nothing when no token', async () => {
    localStorage.removeItem('sessionToken')
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.token = null

    await session.loadMe()
    await flushPromises()

    expect(session.user).toBeNull()
  })

  it('loadMe — clears token and user on API error', async () => {
    server.use(
      http.get('http://localhost/api/auth/me', () =>
        HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      )
    )
    localStorage.setItem('sessionToken', 'expired-tok')
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.token = 'expired-tok'
    session.user = { id: 1, username: 'admin', role: 'ADMIN' }

    await session.loadMe()
    await flushPromises()

    expect(session.user).toBeNull()
    expect(session.token).toBeNull()
    expect(localStorage.getItem('sessionToken')).toBeNull()
  })

  it('hasRole — returns false when user is null', async () => {
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = null
    expect(session.hasRole('ADMIN')).toBe(false)
  })

  it('hasRole — returns true when role matches', async () => {
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'BILLING' }
    expect(session.hasRole('BILLING', 'ADMIN')).toBe(true)
  })

  it('hasRole — returns false when role does not match', async () => {
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'CLINICIAN' }
    expect(session.hasRole('ADMIN')).toBe(false)
  })
})

// ── toastStore ───────────────────────────────────────────────────────────────

describe('toastStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('success() adds a toast with type success', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.success('All good')
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].type).toBe('success')
    expect(toast.toasts[0].message).toBe('All good')
  })

  it('error() adds a toast with type error', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.error('Something broke')
    expect(toast.toasts[0].type).toBe('error')
    expect(toast.toasts[0].message).toBe('Something broke')
  })

  it('info() adds a toast with type info', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.info('FYI')
    expect(toast.toasts[0].type).toBe('info')
    expect(toast.toasts[0].message).toBe('FYI')
  })

  it('warn() adds a toast with type warn', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.warn('Careful now')
    expect(toast.toasts[0].type).toBe('warn')
    expect(toast.toasts[0].message).toBe('Careful now')
  })

  it('remove() removes toast by id', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.success('to remove')
    const id = toast.toasts[0].id
    toast.remove(id)
    expect(toast.toasts).toHaveLength(0)
  })

  it('remove() does nothing for unknown id', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.success('stays')
    toast.remove(9999)
    expect(toast.toasts).toHaveLength(1)
  })

  it('multiple toasts stack independently', async () => {
    const { useToastStore } = await import('../stores/toast.js')
    const toast = useToastStore()
    toast.success('first')
    toast.error('second')
    toast.warn('third')
    expect(toast.toasts).toHaveLength(3)
    const types = toast.toasts.map(t => t.type)
    expect(types).toContain('success')
    expect(types).toContain('error')
    expect(types).toContain('warn')
  })
})
