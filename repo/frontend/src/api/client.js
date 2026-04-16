import axios from 'axios'

const BASE_URL = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE) || '/api'

export const client = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: attach session token + CSRF token on mutating requests
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('sessionToken')
  if (token) {
    config.headers['X-Session-Token'] = token
  }
  const method = (config.method || 'get').toLowerCase()
  if (['post', 'put', 'patch', 'delete'].includes(method)) {
    const csrf = localStorage.getItem('csrfToken')
    if (csrf) {
      config.headers['X-CSRF-Token'] = csrf
    }
  }
  return config
})

// Response interceptor: unwrap `{data, meta?}` envelope so callers always get the payload directly.
// Pagination meta (if present) is attached as a non-enumerable property via `response.meta`,
// and is also available via the `getMeta()` helper exported below.
const lastMeta = new WeakMap()

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'data' in body) {
      const payload = body.data
      if (body.meta !== undefined && payload && typeof payload === 'object') {
        try { lastMeta.set(payload, body.meta) } catch { /* primitives ignored */ }
      }
      return payload
    }
    return body
  },
  (error) => {
    if (error.response) {
      const body = error.response.data
      const err = new Error(body?.message || 'Request failed')
      err.code = body?.code || error.response.status
      err.details = body?.details || null
      err.status = error.response.status
      // If session is invalid, clear token so router redirects to login cleanly.
      if (error.response.status === 401) {
        try {
          localStorage.removeItem('sessionToken')
          localStorage.removeItem('csrfToken')
        } catch { /* noop */ }
      }
      return Promise.reject(err)
    }
    return Promise.reject(error)
  }
)

export function getMeta(payload) {
  return lastMeta.get(payload) || null
}

/**
 * Generate a UUID v4 for idempotency keys.
 * Uses crypto.randomUUID() available in Node 18+ and modern browsers.
 */
export function uuid() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export default client
