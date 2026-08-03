import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Audit log', () => {
  test('owner can view the audit log and filter by user', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await page.goto('/audit');
    await page.waitForSelector('.audit-table', { timeout: 15000 });

    await expect(page.locator('.audit-table tbody tr')).not.toHaveCount(0);
    await expect(page.locator('.audit-table')).toContainText('LOGIN');

    const filter = page.locator('.audit-filter select');
    await filter.selectOption('owner');
    await expect(page.locator('.audit-username').filter({ hasNotText: 'owner' })).toHaveCount(0, { timeout: 20000 });
    const usernames = await page.locator('.audit-username').allInnerTexts();
    for (const username of usernames) {
      expect(username.trim()).toBe('owner');
    }
  });

  test('creating a user records an audit entry', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await page.goto('/users');
    await page.waitForSelector('.user-management', { timeout: 15000 });

    const username = `audituser${Date.now()}`;
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('TempPass123!');
    await page.getByPlaceholder('Email').fill(`${username}@example.com`);
    await page.getByRole('button', { name: /create user/i }).click();
    await page.locator('.user-item').filter({ hasText: username }).waitFor({ state: 'visible', timeout: 15000 });

    await page.goto('/audit');
    await page.waitForSelector('.audit-table', { timeout: 15000 });
    await expect(page.locator('.audit-table')).toContainText(`Created user '${username}'`);
  });
});
