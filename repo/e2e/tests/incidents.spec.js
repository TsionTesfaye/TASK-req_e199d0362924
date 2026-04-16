// @ts-check
const { test, expect } = require('@playwright/test')

/** Log in as admin and navigate to the incidents page. */
async function loginAndGoToIncidents(page) {
  await page.goto('/login')
  await page.locator('#username').fill('admin')
  await page.locator('#password').fill('strongPass123')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
  await page.goto('/incidents')
  await expect(page).toHaveURL(/\/incidents/)
}

test.describe('Incidents', () => {
  test('incidents page renders the title and New Incident button', async ({ page }) => {
    await loginAndGoToIncidents(page)

    await expect(page.locator('.page-title')).toContainText('Incidents')
    await expect(page.getByRole('button', { name: /New Incident/i })).toBeVisible()
  })

  test('New Incident button opens the submission dialog', async ({ page }) => {
    await loginAndGoToIncidents(page)

    await page.getByRole('button', { name: /New Incident/i }).click()
    await expect(page.locator('.dialog-title')).toContainText('Submit Incident Report')
  })

  test('can submit a new incident report', async ({ page }) => {
    await loginAndGoToIncidents(page)

    const ts = Date.now()

    // Open dialog
    await page.getByRole('button', { name: /New Incident/i }).click()
    const dialog = page.locator('.dialog-box')
    await expect(dialog).toBeVisible()

    // Fill required fields
    await dialog
      .locator('.field')
      .filter({ hasText: 'Approximate Location' })
      .locator('input')
      .fill(`5th & Main E2E-${ts}`)

    await dialog
      .locator('.field')
      .filter({ hasText: 'Neighborhood' })
      .locator('input')
      .fill('Downtown')

    // Select category
    await dialog
      .locator('.field')
      .filter({ hasText: 'Category' })
      .locator('select')
      .selectOption('MEDICAL')

    // Description (textarea)
    await dialog
      .locator('.field')
      .filter({ hasText: 'Description' })
      .locator('textarea')
      .fill(`E2E test incident created at ${ts}`)

    // Submit
    await dialog.getByRole('button', { name: 'Submit' }).click()

    // Dialog should close after successful submission
    await expect(dialog).not.toBeVisible({ timeout: 10_000 })
  })

  test('incident form requires location field', async ({ page }) => {
    await loginAndGoToIncidents(page)

    await page.getByRole('button', { name: /New Incident/i }).click()
    const dialog = page.locator('.dialog-box')

    // Click submit without filling required fields — HTML5 required should prevent it
    await dialog.getByRole('button', { name: 'Submit' }).click()

    // Dialog should still be open
    await expect(dialog).toBeVisible()
  })

  test('cancel closes the incident dialog', async ({ page }) => {
    await loginAndGoToIncidents(page)

    await page.getByRole('button', { name: /New Incident/i }).click()
    await expect(page.locator('.dialog-box')).toBeVisible()

    await page.locator('.dialog-header .btn-ghost').click()
    await expect(page.locator('.dialog-box')).not.toBeVisible()
  })
})
