import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, PNG_BUFFER } from './helpers';

test.describe('Tickets', () => {
  test('admin sees all tickets and can change a status', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'admin', DEFAULT_PASSWORD);
    await page.goto('/tickets');
    await page.waitForSelector('.ticket-list', { timeout: 15000 });

    const card = page.locator('.ticket-card').filter({ hasText: 'Leaky faucet' });
    await card.waitFor({ state: 'visible', timeout: 15000 });

    const current = (await card.locator('.ticket-status').innerText()).trim();
    const targets = [
      { label: 'New', selector: '.ticket-actions button.btn-sm' },
      { label: 'In Progress', selector: '.ticket-actions button.btn-warning' },
      { label: 'Done', selector: '.ticket-actions button.btn-success' },
      { label: 'Rejected', selector: '.ticket-actions button.btn-danger' },
    ];
    const target = targets.find((t) => t.label !== current);
    await card.locator(target.selector).click();
    await expect(card.locator('.ticket-status')).toHaveText(target.label);
  });

  test('tenant can create a ticket with a photo', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await page.goto('/tickets');
    await page.waitForSelector('.ticket-list', { timeout: 15000 });

    const title = `E2E Ticket ${Date.now()}`;
    await page.getByRole('button', { name: /new ticket/i }).click();
    const form = page.locator('.apartment-form');
    await form.waitFor({ state: 'visible' });
    await form.locator('input').fill(title);
    await form.locator('textarea').fill('E2E description');
    await form
      .locator('input[type="file"]')
      .setInputFiles({ name: 'ticket-photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });
    await form.getByRole('button', { name: /submit ticket/i }).click();

    await expect(page.locator('.ticket-card').filter({ hasText: title })).toBeVisible();
  });

  test('tenant only sees tickets of their own apartment', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await page.goto('/tickets');
    await page.waitForSelector('.ticket-list', { timeout: 15000 });

    await expect(page.locator('.ticket-card').filter({ hasText: 'Leaky faucet' })).toBeVisible();
    await expect(page.locator('.ticket-card').filter({ hasText: 'Garden fence damaged' })).toHaveCount(0);
    await expect(page.locator('.ticket-actions')).toHaveCount(0);
  });
});
