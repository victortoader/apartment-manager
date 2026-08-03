import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, openFirstApartmentDetail, PDF_BUFFER } from './helpers';

test.describe('Apartment detail', () => {
  test('owner sees all detail sections', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);
    for (const heading of ['Documents', 'Contacts', 'Notes', 'Tickets']) {
      await expect(page.getByRole('heading', { name: new RegExp(heading, 'i') })).toBeVisible();
    }
  });

  test('owner can add, edit and delete a contact', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const name = `E2E Contact ${Date.now()}`;
    const value = 'e2e-contact@example.com';
    const updatedValue = 'e2e-contact-updated@example.com';

    await page.getByRole('button', { name: /^\+ add$/i }).first().click();
    const contactForm = page.locator('.contact-form');
    await contactForm.waitFor({ state: 'visible' });
    await contactForm.locator('input').nth(0).fill(name);
    await contactForm.locator('input').nth(1).fill(value);
    await contactForm.getByRole('button', { name: /save/i }).click();

    const item = page.locator('.contact-item').filter({ hasText: name });
    await expect(item).toBeVisible();
    await expect(item.locator('.contact-value')).toHaveText(value);

    await item.locator('.btn-sm').click();
    await page.locator('.contact-form input').nth(1).fill(updatedValue);
    await page.locator('.contact-form').getByRole('button', { name: /update/i }).click();
    await expect(item.locator('.contact-value')).toHaveText(updatedValue);

    page.once('dialog', (dialog) => dialog.accept());
    await item.locator('.btn-delete-small').click();
    await expect(page.locator('.contact-item').filter({ hasText: name })).toHaveCount(0);
  });

  test('owner can add and delete a note', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const noteText = `E2E Note ${Date.now()}`;
    await page.getByRole('button', { name: /^\+ add$/i }).last().click();
    const noteForm = page.locator('.contact-form');
    await noteForm.waitFor({ state: 'visible' });
    await noteForm.locator('textarea').fill(noteText);
    await noteForm.getByRole('button', { name: /save/i }).click();

    const noteItem = page.locator('.note-item').filter({ hasText: noteText });
    await expect(noteItem).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    await noteItem.locator('.btn-delete-small').click();
    await expect(page.locator('.note-item').filter({ hasText: noteText })).toHaveCount(0);
  });

  test('owner can upload a protocol document', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    await page.locator('.doc-type-select').selectOption('OTHER');
    await page
      .locator('.protocols-header input[type="file"]')
      .first()
      .setInputFiles({ name: 'e2e-protocol.pdf', mimeType: 'application/pdf', buffer: PDF_BUFFER });

    await expect(page.locator('.protocol-item').filter({ hasText: 'e2e-protocol.pdf' })).toBeVisible();
  });

  test('owner can create a ticket from the detail page', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const title = `E2E Detail Ticket ${Date.now()}`;
    await page.getByRole('button', { name: /new ticket/i }).click();
    const form = page.locator('.detail-tickets .apartment-form');
    await form.waitFor({ state: 'visible' });
    await form.locator('input').fill(title);
    await form.locator('textarea').fill('Created from the detail page');
    await form.getByRole('button', { name: /submit ticket/i }).click();

    await page.getByRole('link', { name: /view all/i }).click();
    await page.waitForSelector('.ticket-list', { timeout: 15000 });
    await expect(page.locator('.ticket-card').filter({ hasText: title })).toBeVisible();
  });

  test('tenant can upload a bill proof', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    await page.getByRole('button', { name: /^\+ upload$/i }).click();
    await page
      .locator('.paid-bills-form input[type="file"]')
      .setInputFiles({ name: 'e2e-bill.pdf', mimeType: 'application/pdf', buffer: PDF_BUFFER });

    await expect(page.locator('.paid-bills-link').filter({ hasText: 'e2e-bill.pdf' }).first()).toBeVisible();
  });
});
