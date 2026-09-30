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

// When Gradle drives the suite it passes the packaged jar; running the suite directly
// still falls back to booting the backend through Gradle.
function backendWebServer() {
  const jar = process.env.E2E_BACKEND_JAR;
  const common = {
    url: 'http://localhost:8080/api/auth/login',
    reuseExistingServer: !process.env.CI,
    timeout: 300000,
    stdout: 'pipe',
    stderr: 'pipe',
  };
  if (jar) {
    return {
      command: 'node scripts/start-backend.cjs',
      cwd: __dirname,
      ...common,
    };
  }
  return {
    command: `${gradle} :backend:bootRun --no-daemon`,
    cwd: root,
    ...common,
  };
}

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
    backendWebServer(),
    {
      command: 'npx react-scripts start',
      port: 3000,
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
      cwd: path.resolve(__dirname),
    },
  ],
});
