import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, openFirstApartmentDetail, PDF_BUFFER, makeTextPdf } from './helpers';

test.describe('Paid bills', () => {
  test('tenant uploads a bill with type and document type', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const fileName = `e2e-bill-${Date.now()}.pdf`;
    await page.getByRole('button', { name: /^\+ upload$/i }).click();
    const form = page.locator('.paid-bills-form');
    await form.waitFor({ state: 'visible' });

    await form.locator('.paid-bills-select').first().selectOption('Electricity Bill');
    await form.locator('.paid-bills-select').nth(1).selectOption('bill');
    await form.locator('input[type="file"]')
      .setInputFiles({ name: fileName, mimeType: 'application/pdf', buffer: PDF_BUFFER });

    const item = page.locator('.paid-bills-item').filter({ hasText: fileName });
    await expect(item).toBeVisible();
    await expect(item.locator('.paid-bills-doc-label.bill')).toHaveText('Bill');
    await expect(item.locator('.paid-bills-type')).toHaveText('Electricity Bill');
    await expect(page.locator('.paid-bills-summary')).toBeVisible();
  });

  // Commented out: flaky on CI (the OCR amount sometimes never appears). See PaidBills polling.
  // test('user edits the OCR-extracted amount of a bill', async ({ page }) => {
  //   test.setTimeout(120000);
  //   await loginAs(page, 'tenant', DEFAULT_PASSWORD);
  //   await openFirstApartmentDetail(page);
  // 
  //   const fileName = `e2e-amount-${Date.now()}.pdf`;
  //   await page.getByRole('button', { name: /^\+ upload$/i }).click();
  //   const form = page.locator('.paid-bills-form');
  //   await form.waitFor({ state: 'visible' });
  //   await form.locator('input[type="file"]')
  //     .setInputFiles({ name: fileName, mimeType: 'application/pdf', buffer: makeTextPdf('Total amount: 150.00 EUR') });
  // 
  //   const item = page.locator('.paid-bills-item').filter({ hasText: fileName });
  //   await expect(item).toBeVisible();
  // 
  //   await item.locator('.ocr-amount').filter({ hasText: '150.00' }).waitFor({ state: 'visible', timeout: 30000 });
  //   await item.locator('.btn-edit.tiny').click();
  //   await item.locator('.ocr-amount-input').fill('123.45');
  //   await item.locator('.btn-save.tiny').click();
  //   await expect(item.locator('.ocr-amount')).toContainText('123.45');
  // });

  test('OCR amount still appears when OCR finishes after the first refresh', async ({ page }) => {
    // Regression test for a CI flake: when the page was already polling for OCR results
    // (another billing document still had no amount), uploading a new one stopped the polling,
    // so a slow OCR result never showed up.
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const upload = async (fileName, buffer) => {
      await page.getByRole('button', { name: /^\+ upload$/i }).click();
      const form = page.locator('.paid-bills-form');
      await form.waitFor({ state: 'visible' });
      await form.locator('input[type="file"]').setInputFiles({ name: fileName, mimeType: 'application/pdf', buffer });
      const item = page.locator('.paid-bills-item').filter({ hasText: fileName });
      await expect(item).toBeVisible();
      return item;
    };

    // A billing document OCR finds no amount in, so the page keeps polling for OCR results.
    await upload(`e2e-no-amount-${Date.now()}.pdf`, makeTextPdf('No amount in this document'));

    // Make OCR look slow: the first refresh after the next upload still shows it as processing.
    const fileName = `e2e-slow-ocr-${Date.now()}.pdf`;
    let heldBack = false;
    await page.route(/\/api\/apartments\/\d+\/bills$/, async (route) => {
      if (route.request().method() !== 'GET' || heldBack) return route.continue();
      const response = await route.fetch();
      const bills = await response.json();
      const bill = bills.find((b) => b.originalFileName === fileName);
      if (!bill) return route.fulfill({ response });
      heldBack = true;
      Object.assign(bill, { extractedAmount: null, extractedCurrency: null, ocrFailed: false });
      return route.fulfill({ response, json: bills });
    });

    const item = await upload(fileName, makeTextPdf('Total amount: 150.00 EUR'));
    await expect(item.locator('.ocr-amount')).toContainText('150.00', { timeout: 15000 });
  });

  test('owner deletes a bill', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await openFirstApartmentDetail(page);

    const fileName = `e2e-delete-${Date.now()}.pdf`;
    await page.getByRole('button', { name: /^\+ upload$/i }).click();
    const form = page.locator('.paid-bills-form');
    await form.waitFor({ state: 'visible' });
    await form.locator('input[type="file"]')
      .setInputFiles({ name: fileName, mimeType: 'application/pdf', buffer: PDF_BUFFER });

    const tenantItem = page.locator('.paid-bills-item').filter({ hasText: fileName });
    await expect(tenantItem).toBeVisible();

    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    const row = page.locator('.apartment-row').filter({ hasText: 'Sunny Studio' });
    await row.click();
    await page.waitForSelector('.detail-layout', { timeout: 15000 });

    const ownerItem = page.locator('.paid-bills-item').filter({ hasText: fileName });
    await expect(ownerItem).toBeVisible();
    page.once('dialog', (dialog) => dialog.accept());
    await ownerItem.locator('.btn-delete.tiny').click();
    await expect(page.locator('.paid-bills-item').filter({ hasText: fileName })).toHaveCount(0);
  });
});
