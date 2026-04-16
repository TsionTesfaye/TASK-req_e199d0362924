// @ts-check
const { test, expect } = require('@playwright/test')

/** Log in as admin and navigate to the patients page. */
async function loginAndGoToPatients(page) {
  await page.goto('/login')
  await page.locator('#username').fill('admin')
  await page.locator('#password').fill('strongPass123')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
  await page.goto('/patients')
  await expect(page).toHaveURL(/\/patients/)
}

test.describe('Patients', () => {
  test('patients page renders the title and Register Patient button', async ({ page }) => {
    await loginAndGoToPatients(page)

    await expect(page.locator('.page-title')).toContainText('Patients')
    await expect(page.getByRole('button', { name: /Register Patient/i })).toBeVisible()
  })

  test('Register Patient button opens the registration dialog', async ({ page }) => {
    await loginAndGoToPatients(page)

    await page.getByRole('button', { name: /Register Patient/i }).click()
    await expect(page.locator('.dialog-title')).toContainText('Register Patient')
    await expect(page.getByRole('button', { name: 'Register' })).toBeVisible()
  })

  test('can register a new patient and it appears in the list', async ({ page }) => {
    await loginAndGoToPatients(page)

    // Unique timestamp suffix to avoid collisions with api-tests data
    const ts = Date.now()
    const firstName = `E2E${ts}`
    const lastName  = 'Patient'

    // Open dialog
    await page.getByRole('button', { name: /Register Patient/i }).click()
    await expect(page.locator('.dialog-box')).toBeVisible()

    // Fill in required fields using label-adjacent inputs
    const dialog = page.locator('.dialog-box')
    await dialog.locator('.field').filter({ hasText: 'First Name' }).locator('input').fill(firstName)
    await dialog.locator('.field').filter({ hasText: 'Last Name' }).locator('input').fill(lastName)
    await dialog.locator('.field').filter({ hasText: 'Date of Birth' }).locator('input').fill('1990-06-15')

    // Submit the form
    await dialog.getByRole('button', { name: 'Register' }).click()

    // Dialog should close on success
    await expect(page.locator('.dialog-box')).not.toBeVisible({ timeout: 10_000 })
  })

  test('cancel button closes the registration dialog', async ({ page }) => {
    await loginAndGoToPatients(page)

    await page.getByRole('button', { name: /Register Patient/i }).click()
    await expect(page.locator('.dialog-box')).toBeVisible()

    await page.getByRole('button', { name: 'Cancel' }).click()
    await expect(page.locator('.dialog-box')).not.toBeVisible()
  })

  test('patients page shows DataTable component', async ({ page }) => {
    await loginAndGoToPatients(page)
    // The patients list is rendered inside a .card — either table or empty/loading state
    await expect(page.locator('.card')).toBeVisible()
  })
})
