# Playwright test runner — Linux (Ubuntu Jammy), Node 18, Chromium pre-installed
FROM mcr.microsoft.com/playwright:v1.59.1-jammy

WORKDIR /e2e

COPY package.json ./
RUN npm install

COPY . .

# Run tests with list reporter (no interactive mode in CI)
CMD ["npx", "playwright", "test", "--reporter=list"]
