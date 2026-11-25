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
    const targetLabel = ['New', 'In Progress', 'Done', 'Rejected'].find((l) => l !== current);
    await card.getByRole('button', { name: targetLabel }).click();
    await expect(card.locator('.ticket-status')).toHaveText(targetLabel);
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
    await form.getByRole('textbox', { name: 'Ticket title' }).fill(title);
    await form.locator('textarea').fill('E2E description');
    await form
      .locator('input[type="file"]')
      .setInputFiles({ name: 'ticket-photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });
    await form.getByRole('button', { name: /submit ticket/i }).click();

    await expect(page.locator('.ticket-card').filter({ hasText: title })).toBeVisible();
  });

  test('ticket photo loads with the login and opens in a new tab', async ({ page, context }) => {
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await page.goto('/tickets');
    await page.waitForSelector('.ticket-list', { timeout: 15000 });

    const title = `E2E Photo Ticket ${Date.now()}`;
    await page.getByRole('button', { name: /new ticket/i }).click();
    const form = page.locator('.apartment-form');
    await form.waitFor({ state: 'visible' });
    await form.getByRole('textbox', { name: 'Ticket title' }).fill(title);
    await form
      .locator('input[type="file"]')
      .setInputFiles({ name: 'ticket-photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });
    await form.getByRole('button', { name: /submit ticket/i }).click();

    const card = page.locator('.ticket-card').filter({ hasText: title });
    await expect(card).toBeVisible();

    // Ticket photos are no longer public: the thumbnail must be fetched with the login
    // and shown from a temporary blob: URL, and it must actually render.
    const thumb = card.locator('.ticket-photo-button img.ticket-photo-thumb');
    await expect(thumb).toBeVisible({ timeout: 15000 });
    await expect(thumb).toHaveAttribute('src', /^blob:/);
    await expect.poll(() => thumb.evaluate((img) => img.complete && img.naturalWidth > 0)).toBe(true);

    // Clicking opens the already-loaded photo in a new tab (inside the click, so popup
    // blockers allow it).
    const [popup] = await Promise.all([
      context.waitForEvent('page'),
      card.locator('.ticket-photo-button').first().click(),
    ]);
    await popup.waitForLoadState();
    expect(popup.url()).toMatch(/^blob:/);
    await popup.close();
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
