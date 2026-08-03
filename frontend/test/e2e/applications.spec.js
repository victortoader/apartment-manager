import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, PDF_BUFFER } from './helpers';

test.describe('Applications', () => {
  test('guest applies for an available apartment and owner sees the application', async ({ page }) => {
    test.setTimeout(180000);

    const firstName = `Anna${Date.now()}`;

    await page.goto('/presentations');
    await page.waitForSelector('.pl-grid', { timeout: 15000 });

    const card = page.locator('.pl-card').filter({ hasText: 'Garden Flat' });
    await card.getByRole('button', { name: /^apply$/i }).click();
    const modal = page.locator('.apply-modal-wide');
    await modal.waitFor({ state: 'visible', timeout: 15000 });

    await modal.getByRole('button', { name: /submit application/i }).click();
    await expect(modal.locator('.login-error')).toBeVisible();

    const main = modal.locator('.apply-section').filter({ hasText: 'Main Applicant' });
    await main.locator('.apply-field').filter({ hasText: 'First name' }).locator('input').fill(firstName);
    await main.locator('.apply-field').filter({ hasText: 'Last name' }).locator('input').fill('E2E');
    await main.locator('input[type="email"]').first().fill('anna.e2e@example.com');

    await modal
      .locator('.apply-file-section input[type="file"]')
      .setInputFiles({ name: 'application.pdf', mimeType: 'application/pdf', buffer: PDF_BUFFER });
    await modal.getByRole('button', { name: /submit application/i }).click();
    await expect(modal).toContainText('Application Submitted!');

    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await expect(page.locator('.btn-application').filter({ hasText: 'See Applications' }).first()).toBeVisible();

    await page.goto('/applications/2');
    await page.waitForSelector('.application-page-list', { timeout: 15000 });
    await expect(page.locator('.application-page-card').filter({ hasText: firstName }).first()).toBeVisible();
  });
});
