// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Auth E2E tests — login / logout / session handling.
 * These tests exercise the real login flow through the browser UI.
 */

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    // Start each test on the login page with a clean state
    await page.goto('/login')
    await expect(page).toHaveURL(/\/login/)
  })

  test('login page renders the brand and form', async ({ page }) => {
    await expect(page.locator('.brand-name')).toContainText('RescueHub')
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Login' })).toBeVisible()
  })

  test('successful login redirects to dashboard', async ({ page }) => {
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('strongPass123')
    await page.getByRole('button', { name: 'Login' }).click()

    // Should redirect away from /login and show the sidebar
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
    await expect(page.locator('.sidebar')).toBeVisible()
  })

  test('wrong credentials shows an error message', async ({ page }) => {
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('wrong-password-xyz')
    await page.getByRole('button', { name: 'Login' }).click()

    // Error div should appear; URL should still be /login
    await expect(page.locator('.login-error')).toBeVisible({ timeout: 10_000 })
    await expect(page).toHaveURL(/\/login/)
  })

  test('empty username prevents form submission', async ({ page }) => {
    // HTML5 `required` prevents submit — button click should not navigate
    await page.locator('#password').fill('strongPass123')
    await page.getByRole('button', { name: 'Login' }).click()
    await expect(page).toHaveURL(/\/login/)
  })

  test('logout redirects back to login page', async ({ page }) => {
    // Log in first
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('strongPass123')
    await page.getByRole('button', { name: 'Login' }).click()
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })

    // Click the logout button in the topbar
    const logoutBtn = page.locator('button.btn-ghost', { hasText: /logout|sign out/i })
    await logoutBtn.click()

    // Should redirect to /login
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
  })

  test('visiting a protected route while unauthenticated redirects to login', async ({ page }) => {
    // Clear any existing session
    await page.evaluate(() => {
      localStorage.removeItem('sessionToken')
      localStorage.removeItem('csrfToken')
    })

    await page.goto('/')
    // The router guard should redirect to /login
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
  })
})
