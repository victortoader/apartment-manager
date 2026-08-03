import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Manage mode', () => {
  test('owner needs the correct password to enter manage mode', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);

    await page.getByRole('button', { name: /^manage$/i }).click();
    const modal = page.locator('.modal-overlay');
    await modal.waitFor({ state: 'visible', timeout: 10000 });

    await modal.locator('input[type="password"]').fill('wrong-password');
    await modal.getByRole('button', { name: /confirm/i }).click();
    await expect(modal.locator('.modal-error')).toBeVisible();
    await expect(page.locator('.btn-manage.active')).toHaveCount(0);

    await modal.locator('input[type="password"]').fill(DEFAULT_PASSWORD);
    await modal.getByRole('button', { name: /confirm/i }).click();
    await expect(page.locator('.btn-manage.active')).toBeVisible();
    await expect(page.getByRole('button', { name: /add apartment/i })).toBeVisible();
  });
});
