import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, enterManageMode } from './helpers';

test.describe('Apartment management', () => {
  test('owner sees the seeded apartments on the dashboard', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await expect(page.locator('.apartment-row').first()).toBeVisible();
    for (const title of ['Sunny Studio', 'Garden Flat', 'City Loft']) {
      await expect(page.locator('.apartment-row').filter({ hasText: title })).toBeVisible();
    }
  });

  test('owner creates and deletes an apartment in manage mode', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await enterManageMode(page, DEFAULT_PASSWORD);

    const title = `E2E Apartment ${Date.now()}`;
    await page.getByRole('button', { name: /add apartment/i }).click();
    const form = page.locator('.apartment-form');
    await form.waitFor({ state: 'visible' });
    await form.locator('input[name="title"]').fill(title);
    await form.locator('input[name="location"]').fill('E2E Teststraße 1');
    await form.locator('input[name="rooms"]').fill('2');
    await form.locator('input[name="area"]').fill('55');
    await form.getByRole('button', { name: /save/i }).click();

    const created = page.locator('.apartment-row').filter({ hasText: title });
    await created.waitFor({ state: 'visible', timeout: 15000 });
    await expect(created.locator('.row-location')).toHaveText('E2E Teststraße 1');

    page.once('dialog', (dialog) => dialog.accept());
    await created.locator('.btn-delete').click();
    await expect(page.locator('.apartment-row').filter({ hasText: title })).toHaveCount(0);
  });

  test('owner can change the status of an apartment', async ({ page }) => {
    test.setTimeout(90000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await enterManageMode(page, DEFAULT_PASSWORD);

    const row = page.locator('.apartment-row').first();
    await row.locator('.row-status .btn-edit.small').click();
    const editor = row.locator('.status-editor');
    await editor.waitFor({ state: 'visible' });
    await editor.locator('select').selectOption('AVAILABLE_IMMEDIATELY');
    await editor.getByRole('button', { name: /save/i }).click();
    await expect(row.locator('.status-badge')).toContainText('Available Now');
  });

  test('owner can add a metadata entry to an apartment', async ({ page }) => {
    test.setTimeout(90000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await enterManageMode(page, DEFAULT_PASSWORD);

    const row = page.locator('.apartment-row').filter({ hasText: 'City Loft' });
    await row.locator('.row-metadata .btn-upload.small').click();

    const entry = `E2E-META-${Date.now()}`;
    const editor = row.locator('.metadata-editor');
    await editor.waitFor({ state: 'visible' });
    await editor.locator('.metadata-add-row input').fill(entry);
    await editor.getByRole('button', { name: /add entry/i }).click();
    await editor.locator('.metadata-actions .btn-primary.small').click();

    await expect(row.locator('.metadata-list .metadata-item').filter({ hasText: entry })).toBeVisible();
  });

  test('tenant sees only their own apartment', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await expect(page.locator('.apartment-row')).toHaveCount(1);
    await expect(page.locator('.apartment-row')).toContainText('Sunny Studio');
  });
});
