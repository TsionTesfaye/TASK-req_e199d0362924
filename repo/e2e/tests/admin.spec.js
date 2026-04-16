// @ts-check
const { test, expect } = require('@playwright/test')

/** Log in as admin and navigate to the user management page. */
async function loginAndGoToAdmin(page) {
  await page.goto('/login')
  await page.locator('#username').fill('admin')
  await page.locator('#password').fill('strongPass123')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
  await page.goto('/admin/users')
  await expect(page).toHaveURL(/\/admin\/users/)
}

test.describe('Admin — User Management', () => {
  test('user management page renders the title and New User button', async ({ page }) => {
    await loginAndGoToAdmin(page)

    await expect(page.locator('.page-title')).toContainText('User Management')
    await expect(page.getByRole('button', { name: /New User/i })).toBeVisible()
  })

  test('New User button opens the creation dialog', async ({ page }) => {
    await loginAndGoToAdmin(page)

    await page.getByRole('button', { name: /New User/i }).click()
    await expect(page.locator('.dialog-title')).toContainText('New User')
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible()
  })

  test('can create a new user via the dialog', async ({ page }) => {
    await loginAndGoToAdmin(page)

    const ts = Date.now()
    const newUsername = `e2e_user_${ts}`

    // Open dialog
    await page.getByRole('button', { name: /New User/i }).click()
    const dialog = page.locator('.dialog-box')
    await expect(dialog).toBeVisible()

    // Fill form fields (siblings of <label>, not label-wrapped)
    await dialog.locator('.field').filter({ hasText: 'Username' }).locator('input').fill(newUsername)
    await dialog.locator('.field').filter({ hasText: 'Display Name' }).locator('input').fill(`E2E User ${ts}`)
    await dialog.locator('.field').filter({ hasText: 'Password' }).locator('input[type="password"]').fill('StrongE2E123!')
    await dialog.locator('.field').filter({ hasText: 'Role' }).locator('select').selectOption('BILLING')

    // Submit
    await dialog.getByRole('button', { name: 'Create' }).click()

    // Dialog should close on success
    await expect(dialog).not.toBeVisible({ timeout: 10_000 })
  })

  test('created user can log in with their credentials', async ({ page, browser }) => {
    await loginAndGoToAdmin(page)

    const ts = Date.now()
    const newUsername = `e2e_login_${ts}`
    const newPassword = 'StrongLogin123!'

    // Create the user via admin UI
    await page.getByRole('button', { name: /New User/i }).click()
    const dialog = page.locator('.dialog-box')
    await dialog.locator('.field').filter({ hasText: 'Username' }).locator('input').fill(newUsername)
    await dialog.locator('.field').filter({ hasText: 'Password' }).locator('input[type="password"]').fill(newPassword)
    await dialog.locator('.field').filter({ hasText: 'Role' }).locator('select').selectOption('FRONT_DESK')
    await dialog.getByRole('button', { name: 'Create' }).click()
    await expect(dialog).not.toBeVisible({ timeout: 10_000 })

    // Open a new browser context (clean session) and try to log in as the new user
    const newCtx = await browser.newContext({ ignoreHTTPSErrors: true })
    const newPage = await newCtx.newPage()

    await newPage.goto('/login')
    await newPage.locator('#username').fill(newUsername)
    await newPage.locator('#password').fill(newPassword)
    await newPage.getByRole('button', { name: 'Login' }).click()

    await expect(newPage).not.toHaveURL(/\/login/, { timeout: 15_000 })
    await expect(newPage.locator('.sidebar')).toBeVisible()

    await newCtx.close()
  })

  test('cancel closes the user creation dialog', async ({ page }) => {
    await loginAndGoToAdmin(page)

    await page.getByRole('button', { name: /New User/i }).click()
    await expect(page.locator('.dialog-box')).toBeVisible()

    await page.getByRole('button', { name: 'Cancel' }).click()
    await expect(page.locator('.dialog-box')).not.toBeVisible()
  })
})
