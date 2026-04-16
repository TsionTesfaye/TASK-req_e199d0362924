import { setupServer } from 'msw/node'
import { handlers } from './handlers.js'

export const server = setupServer(...handlers)

/**
 * Ordered log of every HTTP request intercepted in the current test.
 * Cleared after each test by setup.js's afterEach hook.
 * Each entry: { method: string, url: string (pathname), fullUrl: string }
 */
export const requestLog = []

server.events.on('request:start', ({ request }) => {
  try {
    const u = new URL(request.url)
    requestLog.push({
      method: request.method,
      url: u.pathname,
      fullUrl: request.url,
    })
  } catch {
    requestLog.push({ method: request.method, url: request.url, fullUrl: request.url })
  }
})
