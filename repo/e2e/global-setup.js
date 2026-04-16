/**
 * Playwright global setup — runs once before all spec files.
 *
 * Responsibilities:
 *  1. Ensure the backend is reachable.
 *  2. Bootstrap the system (create admin account) if it has not been done yet.
 *     If api-tests ran first the system is already bootstrapped — that case is
 *     handled gracefully.
 *  3. Verify the admin account is usable by performing a test login.
 *
 * All HTTP calls use Playwright's built-in request context so that
 * ignoreHTTPSErrors is respected for the self-signed TLS cert.
 */

const { request } = require('@playwright/test')

const ADMIN = { username: 'admin', password: 'strongPass123' }
const ORG   = 'Test Org'

/** Poll until the backend/nginx is reachable or timeout expires. */
async function waitForBackend(ctx, maxAttempts = 30, intervalMs = 2000) {
  for (let i = 1; i <= maxAttempts; i++) {
    try {
      const res = await ctx.get('/api/bootstrap/status')
      if (res.ok()) return res
      console.log(`[global-setup] Attempt ${i}/${maxAttempts}: status ${res.status()} — retrying…`)
    } catch (err) {
      console.log(`[global-setup] Attempt ${i}/${maxAttempts}: ${err.message} — retrying…`)
    }
    if (i < maxAttempts) await new Promise(r => setTimeout(r, intervalMs))
  }
  throw new Error(`Backend did not become ready after ${maxAttempts * intervalMs / 1000}s`)
}

module.exports = async (config) => {
  const baseURL =
    process.env.PLAYWRIGHT_BASE_URL ||
    config.projects[0]?.use?.baseURL ||
    'https://localhost:15443'

  const ctx = await request.newContext({ baseURL, ignoreHTTPSErrors: true })

  try {
    // ── 1. Check bootstrap status (with retry for nginx startup delay) ───────
    console.log('[global-setup] Waiting for backend to be reachable…')
    const statusRes = await waitForBackend(ctx)
    const { data: { initialized } } = await statusRes.json()

    // ── 2. Bootstrap if first run ────────────────────────────────────────────
    if (!initialized) {
      console.log('[global-setup] System not initialized — bootstrapping…')
      const bootstrapRes = await ctx.post('/api/bootstrap', {
        data: {
          username: ADMIN.username,
          password: ADMIN.password,
          organizationName: ORG,
        },
      })
      if (!bootstrapRes.ok()) {
        const body = await bootstrapRes.text()
        throw new Error(`Bootstrap failed (${bootstrapRes.status()}): ${body}`)
      }
      console.log('[global-setup] Bootstrap complete.')
    } else {
      console.log('[global-setup] System already initialized.')
    }

    // ── 3. Verify admin login ────────────────────────────────────────────────
    const loginRes = await ctx.post('/api/auth/login', {
      data: ADMIN,
    })
    if (!loginRes.ok()) {
      const body = await loginRes.text()
      throw new Error(
        `Admin login verification failed (${loginRes.status()}): ${body}`
      )
    }
    console.log('[global-setup] Admin login verified. E2E setup complete.')

  } finally {
    await ctx.dispose()
  }
}
