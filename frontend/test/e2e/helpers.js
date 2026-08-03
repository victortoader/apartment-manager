import { expect } from '@playwright/test';

export const DEFAULT_PASSWORD = process.env.DEFAULT_PASSWORD;
if (!DEFAULT_PASSWORD) {
  throw new Error('DEFAULT_PASSWORD environment variable is required to run e2e tests');
}

export const PDF_BUFFER = Buffer.from(
  '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000120 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n199\n%%EOF\n'
);

export const PNG_BUFFER = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64'
);

export async function loginAs(page, username, password = DEFAULT_PASSWORD) {
  await page.goto('/login');
  await page.waitForSelector('.login-form', { timeout: 15000 });
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForSelector('.apartment-stack', { timeout: 20000 });
}

export async function openFirstApartmentDetail(page) {
  await page.locator('.apartment-row').first().click();
  await page.waitForSelector('.detail-layout', { timeout: 15000 });
}

export async function enterManageMode(page, password = DEFAULT_PASSWORD) {
  await page.getByRole('button', { name: /^manage$/i }).click();
  const modal = page.locator('.modal-overlay');
  await modal.waitFor({ state: 'visible', timeout: 10000 });
  await modal.locator('input[type="password"]').fill(password);
  await modal.getByRole('button', { name: /confirm/i }).click();
  await expect(page.locator('.btn-manage.active')).toBeVisible();
}
