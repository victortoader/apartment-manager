import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Role-based UI', () => {
  test('admin sees all apartments and open tickets, but no delete/manage controls', async ({ page }) => {
    test.setTimeout(90000);

    const ticketTitle = `E2E Unread Ticket ${Date.now()}`;
    const login = await page.request.post('/api/auth/login', {
      data: { username: 'tenant', password: DEFAULT_PASSWORD },
    });
    expect(login.ok()).toBeTruthy();
    const token = (await login.json()).token;
    const createTicket = await page.request.post('/api/apartments/1/tickets', {
      headers: { Authorization: `Bearer ${token}` },
      data: { title: ticketTitle, description: 'Created by the roles e2e test' },
    });
    expect(createTicket.ok()).toBeTruthy();

    await loginAs(page, 'admin', DEFAULT_PASSWORD);

    for (const title of ['Sunny Studio', 'Garden Flat', 'City Loft']) {
      await expect(page.locator('.apartment-row').filter({ hasText: title })).toBeVisible();
    }

    await expect(page.locator('.btn-manage')).toHaveCount(0);
    await expect(page.locator('.btn-delete')).toHaveCount(0);

    await page.goto('/tickets');
    await page.waitForSelector('.ticket-list', { timeout: 15000 });
    await expect(page.locator('.ticket-card').filter({ hasText: ticketTitle })).toBeVisible();
  });
});
