/**
 * Vitest global test setup.
 *
 * Runs before every test file (via vite.config.js → test.setupFiles).
 *
 * 1. Forces Axios to use the Fetch adapter so MSW's Node server can intercept
 *    requests. (Axios defaults to XMLHttpRequest in browser environments, which
 *    MSW's node server cannot intercept in the jsdom environment.)
 *
 * 2. Manages the MSW server lifecycle: listen → reset handlers → close.
 */

import axios from 'axios'
import client from '../api/client.js'
import { server, requestLog } from './msw/server.js'

// Force the fetch adapter on both the Axios global defaults (so any newly-
// created instances inherit it) and on the already-created `client` instance.
// Also override baseURL to be absolute: Node's undici-based fetch (used in the
// jsdom environment) requires an absolute URL.  A relative URL like '/api/...'
// throws "TypeError: Failed to parse URL" before MSW can intercept it.
axios.defaults.adapter = 'fetch'
client.defaults.adapter = 'fetch'
client.defaults.baseURL = 'http://localhost/api'

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))

afterEach(() => {
  // Remove per-test handler overrides, restoring the defaults from handlers.js
  server.resetHandlers()
  // Clear the request log so each test starts clean
  requestLog.length = 0
})

afterAll(() => server.close())
