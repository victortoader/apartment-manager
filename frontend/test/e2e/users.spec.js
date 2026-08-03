import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('User management', () => {
  test('owner can create, assign and delete a user', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await page.goto('/users');
    await page.waitForSelector('.user-management', { timeout: 15000 });

    const username = `e2euser${Date.now()}`;
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('TempPass123!');
    await page.getByPlaceholder('Email').fill(`${username}@example.com`);
    await page.getByRole('button', { name: /create user/i }).click();

    const item = page.locator('.user-item').filter({ hasText: username });
    await item.waitFor({ state: 'visible', timeout: 15000 });
    await expect(item.locator('.user-name')).toHaveText(username);

    const select = item.locator('.apt-select');
    const optionValues = await select
      .locator('option')
      .evaluateAll((opts) => opts.map((o) => o.value).filter((v) => v));
    expect(optionValues.length).toBeGreaterThan(0);
    await select.selectOption(optionValues[0]);
    await expect(select).toHaveValue(optionValues[0]);

    page.once('dialog', (dialog) => dialog.accept());
    await item.locator('.btn-delete-small').click();
    await expect(page.locator('.user-item').filter({ hasText: username })).toHaveCount(0);
  });
});
