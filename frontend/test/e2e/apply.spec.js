import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, PDF_BUFFER } from './helpers';

test.describe('Apartment applications', () => {
  test('guest applies to an apartment and must attach a document', async ({ page }) => {
    test.setTimeout(120000);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });

    await page.locator('.pl-card').filter({ hasText: 'Garden Flat' }).getByRole('button', { name: /^apply$/i }).click();
    const modal = page.locator('.apply-modal-wide');
    await modal.waitFor({ state: 'visible', timeout: 10000 });

    await modal.getByRole('button', { name: /submit application/i }).click();
    await expect(modal.locator('.login-error')).toContainText('Please upload a document');

    await modal.locator('.apply-file-section input[type="file"]')
      .setInputFiles({ name: 'application.pdf', mimeType: 'application/pdf', buffer: PDF_BUFFER });
    await modal.getByRole('button', { name: /submit application/i }).click();

    await expect(modal.getByRole('heading', { name: /application submitted/i })).toBeVisible();
  });

  test('owner sees the application and its form data', async ({ page }) => {
    test.setTimeout(120000);

    const firstName = `E2E${Date.now()}`;
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });
    await page.locator('.pl-card').filter({ hasText: 'Garden Flat' }).getByRole('button', { name: /^apply$/i }).click();
    const modal = page.locator('.apply-modal-wide');
    await modal.waitFor({ state: 'visible', timeout: 10000 });

    const mainSection = modal.locator('.apply-section').filter({ hasText: 'Main Applicant' });
    await mainSection.locator('.apply-field').filter({ hasText: 'First name' }).locator('input').fill(firstName);
    await modal.locator('.apply-file-section input[type="file"]')
      .setInputFiles({ name: 'application.pdf', mimeType: 'application/pdf', buffer: PDF_BUFFER });
    await modal.getByRole('button', { name: /submit application/i }).click();
    await expect(modal.getByRole('heading', { name: /application submitted/i })).toBeVisible();

    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    const row = page.locator('.apartment-row').filter({ hasText: 'Garden Flat' });
    await row.locator('.btn-application').waitFor({ state: 'visible', timeout: 15000 });
    await row.locator('.btn-application').click();

    await page.waitForSelector('.application-page-card', { timeout: 15000 });
    const appCard = page.locator('.application-page-card').filter({ hasText: firstName });
    await expect(appCard.locator('.application-page-name')).toHaveText(firstName);

    await appCard.locator('.application-page-header').click();
    const main = appCard.locator('.application-page-section').filter({ hasText: 'Main Applicant' });
    await expect(main).toBeVisible();
    await expect(main.locator('.app-table-value').filter({ hasText: firstName })).toBeVisible();
  });
});
