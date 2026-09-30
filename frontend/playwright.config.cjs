// @ts-check
const fs = require('fs');
const { defineConfig, devices } = require('@playwright/test');
const path = require('path');

const root = path.resolve(__dirname, '..');
const isWindows = process.platform === 'win32';
const gradle = isWindows ? 'gradlew.bat' : './gradlew';

function resolveDefaultPassword() {
  if (process.env.DEFAULT_PASSWORD) return;
  try {
    for (const line of fs.readFileSync(path.join(root, '.env'), 'utf8').split(/\r?\n/)) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const eq = trimmed.indexOf('=');
      if (eq === -1) continue;
      if (trimmed.slice(0, eq) === 'DEFAULT_PASSWORD') {
        process.env.DEFAULT_PASSWORD = trimmed.slice(eq + 1);
        return;
      }
    }
  } catch {
    // handled below
  }
  throw new Error(
    'DEFAULT_PASSWORD is required to run e2e tests. Set it in the environment or in the root .env file.'
  );
}

resolveDefaultPassword();

module.exports = defineConfig({
  testDir: './test/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  failOnFlakyTests: !!process.env.CI,
  globalTimeout: process.env.CI ? 45 * 60 * 1000 : undefined,
  workers: process.env.CI ? 2 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: `${gradle} :backend:bootRun --no-daemon`,
      url: 'http://localhost:8080/api/auth/login',
      reuseExistingServer: !process.env.CI,
      timeout: 300000,
      cwd: root,
      stdout: 'pipe',
      stderr: 'pipe',
    },
    {
      command: 'npx react-scripts start',
      port: 3000,
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
      cwd: path.resolve(__dirname),
    },
  ],
});
