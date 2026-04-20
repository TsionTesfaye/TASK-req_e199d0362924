// @ts-check
const { test, expect, request: apiRequest } = require('@playwright/test')

const BACKEND = process.env.API_BACKEND || 'http://backend:8080/api'

/** Created once per worker; usernames are timestamp-unique to avoid collisions. */
const ts = Date.now()
const roleUsers = {
  CLINICIAN:  { username: `e2e_clin_${ts}`,  password: 'RoleTest123!' },
  BILLING:    { username: `e2e_bill_${ts}`,   password: 'RoleTest123!' },
  FRONT_DESK: { username: `e2e_front_${ts}`,  password: 'RoleTest123!' },
  QUALITY:    { username: `e2e_qual_${ts}`,   password: 'RoleTest123!' },
  MODERATOR:  { username: `e2e_mod_${ts}`,    password: 'RoleTest123!' },
}

test.describe.configure({ mode: 'serial' })

/** Set up all role users once via the backend API before any browser tests run. */
test.beforeAll(async () => {
  const ctx = await apiRequest.newContext({ ignoreHTTPSErrors: true })

  const loginRes = await ctx.post(`${BACKEND}/auth/login`, {
    data: { username: 'admin', password: 'strongPass123' },
  })
  const loginData = (await loginRes.json()).data
  const adminToken = loginData.sessionToken
  const adminCsrf = loginData.csrfToken

  for (const [role, { username, password }] of Object.entries(roleUsers)) {
    await ctx.post(`${BACKEND}/admin/users`, {
      headers: { 'X-Session-Token': adminToken, 'X-CSRF-Token': adminCsrf },
      data: { username, password, displayName: username, role },
    })
  }

  await ctx.dispose()
})

async function loginAs(page, role) {
  const { username, password } = roleUsers[role]
  await page.goto('/login')
  await page.locator('#username').fill(username)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
}

// ── CLINICIAN ──────────────────────────────────────────────────────────────

test.describe('CLINICIAN role restrictions', () => {
  test('CLINICIAN visiting /billing is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/billing')
    await expect(page).not.toHaveURL(/\/billing/, { timeout: 8_000 })
  })

  test('CLINICIAN visiting /admin/users is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/admin/users')
    await expect(page).not.toHaveURL(/\/admin\/users/, { timeout: 8_000 })
  })

  test('CLINICIAN visiting /backups is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/backups')
    await expect(page).not.toHaveURL(/\/backups/, { timeout: 8_000 })
  })

  test('CLINICIAN visiting /exports is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/exports')
    await expect(page).not.toHaveURL(/\/exports/, { timeout: 8_000 })
  })

  test('CLINICIAN visiting /quality is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/quality')
    await expect(page).not.toHaveURL(/\/quality/, { timeout: 8_000 })
  })

  test('CLINICIAN visiting /ranking is redirected away', async ({ page }) => {
    await loginAs(page, 'CLINICIAN')
    await page.goto('/ranking')
    await expect(page).not.toHaveURL(/\/ranking/, { timeout: 8_000 })
  })
})

// ── BILLING ────────────────────────────────────────────────────────────────

test.describe('BILLING role restrictions', () => {
  test('BILLING visiting /admin/users is redirected away', async ({ page }) => {
    await loginAs(page, 'BILLING')
    await page.goto('/admin/users')
    await expect(page).not.toHaveURL(/\/admin\/users/, { timeout: 8_000 })
  })

  test('BILLING visiting /backups is redirected away', async ({ page }) => {
    await loginAs(page, 'BILLING')
    await page.goto('/backups')
    await expect(page).not.toHaveURL(/\/backups/, { timeout: 8_000 })
  })

  test('BILLING visiting /quality is redirected away', async ({ page }) => {
    await loginAs(page, 'BILLING')
    await page.goto('/quality')
    await expect(page).not.toHaveURL(/\/quality/, { timeout: 8_000 })
  })

  test('BILLING visiting /ranking is redirected away', async ({ page }) => {
    await loginAs(page, 'BILLING')
    await page.goto('/ranking')
    await expect(page).not.toHaveURL(/\/ranking/, { timeout: 8_000 })
  })
})

// ── FRONT_DESK ─────────────────────────────────────────────────────────────

test.describe('FRONT_DESK role restrictions', () => {
  test('FRONT_DESK visiting /billing is redirected away', async ({ page }) => {
    await loginAs(page, 'FRONT_DESK')
    await page.goto('/billing')
    await expect(page).not.toHaveURL(/\/billing/, { timeout: 8_000 })
  })

  test('FRONT_DESK visiting /quality is redirected away', async ({ page }) => {
    await loginAs(page, 'FRONT_DESK')
    await page.goto('/quality')
    await expect(page).not.toHaveURL(/\/quality/, { timeout: 8_000 })
  })

  test('FRONT_DESK visiting /admin/users is redirected away', async ({ page }) => {
    await loginAs(page, 'FRONT_DESK')
    await page.goto('/admin/users')
    await expect(page).not.toHaveURL(/\/admin\/users/, { timeout: 8_000 })
  })

  test('FRONT_DESK visiting /exports is redirected away', async ({ page }) => {
    await loginAs(page, 'FRONT_DESK')
    await page.goto('/exports')
    await expect(page).not.toHaveURL(/\/exports/, { timeout: 8_000 })
  })
})

// ── QUALITY ────────────────────────────────────────────────────────────────

test.describe('QUALITY role restrictions', () => {
  test('QUALITY visiting /billing is redirected away', async ({ page }) => {
    await loginAs(page, 'QUALITY')
    await page.goto('/billing')
    await expect(page).not.toHaveURL(/\/billing/, { timeout: 8_000 })
  })

  test('QUALITY visiting /admin/users is redirected away', async ({ page }) => {
    await loginAs(page, 'QUALITY')
    await page.goto('/admin/users')
    await expect(page).not.toHaveURL(/\/admin\/users/, { timeout: 8_000 })
  })

  test('QUALITY visiting /backups is redirected away', async ({ page }) => {
    await loginAs(page, 'QUALITY')
    await page.goto('/backups')
    await expect(page).not.toHaveURL(/\/backups/, { timeout: 8_000 })
  })

  test('QUALITY visiting /exports is redirected away', async ({ page }) => {
    await loginAs(page, 'QUALITY')
    await page.goto('/exports')
    await expect(page).not.toHaveURL(/\/exports/, { timeout: 8_000 })
  })
})

// ── MODERATOR ──────────────────────────────────────────────────────────────

test.describe('MODERATOR role restrictions', () => {
  test('MODERATOR visiting /billing is redirected away', async ({ page }) => {
    await loginAs(page, 'MODERATOR')
    await page.goto('/billing')
    await expect(page).not.toHaveURL(/\/billing/, { timeout: 8_000 })
  })

  test('MODERATOR visiting /admin/users is redirected away', async ({ page }) => {
    await loginAs(page, 'MODERATOR')
    await page.goto('/admin/users')
    await expect(page).not.toHaveURL(/\/admin\/users/, { timeout: 8_000 })
  })

  test('MODERATOR visiting /quality is redirected away', async ({ page }) => {
    await loginAs(page, 'MODERATOR')
    await page.goto('/quality')
    await expect(page).not.toHaveURL(/\/quality/, { timeout: 8_000 })
  })

  test('MODERATOR visiting /backups is redirected away', async ({ page }) => {
    await loginAs(page, 'MODERATOR')
    await page.goto('/backups')
    await expect(page).not.toHaveURL(/\/backups/, { timeout: 8_000 })
  })

  test('MODERATOR visiting /exports is redirected away', async ({ page }) => {
    await loginAs(page, 'MODERATOR')
    await page.goto('/exports')
    await expect(page).not.toHaveURL(/\/exports/, { timeout: 8_000 })
  })
})
