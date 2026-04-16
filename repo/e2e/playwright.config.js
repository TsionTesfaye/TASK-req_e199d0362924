// @ts-check
const { defineConfig, devices } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './tests',
  timeout: 60_000,
  globalSetup: './global-setup.js',
  use: {
    // Inside Docker: PLAYWRIGHT_BASE_URL=https://frontend
    // Outside Docker: falls back to the TLS proxy host port
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'https://localhost:15443',
    // Self-signed cert in dev / CI
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'off',
  },
  // Run tests sequentially — they share a single database and session state
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
