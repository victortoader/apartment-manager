import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Presentations', () => {
  test('guest can browse the presentation list and open a detail page', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-grid', { timeout: 15000 });

    for (const title of ['Sunny Studio', 'Garden Flat', 'City Loft']) {
      await expect(page.locator('.pl-card').filter({ hasText: title })).toBeVisible();
    }

    await page.getByRole('link', { name: /view details/i }).first().click();
    await page.waitForSelector('.pres-main', { timeout: 15000 });
    await expect(page.locator('.pres-hero h1')).toBeVisible();
    await expect(page.getByRole('button', { name: /edit presentation/i })).toHaveCount(0);
  });

  test('owner can edit the presentation of an apartment', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await page.goto('/presentations/apartments/3');
    await page.waitForSelector('.pres-main', { timeout: 15000 });

    const description = `E2E description ${Date.now()}`;
    await page.getByRole('button', { name: /edit presentation/i }).click();
    const editor = page.locator('.pres-editor');
    await editor.waitFor({ state: 'visible' });

    await editor.getByPlaceholder(/short description/i).fill(description);
    await editor.locator('input[type="number"]').fill('999');
    await editor.locator('select').selectOption('RENTED');
    await page.getByRole('button', { name: /^save$/i }).click();

    await expect(page.locator('.pres-body-text')).toHaveText(description);
    await expect(page.locator('.pres-stat-value').first()).toHaveText('€999');
  });
});
