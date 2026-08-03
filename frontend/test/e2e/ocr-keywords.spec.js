import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs, enterManageMode } from './helpers';

test.describe('OCR keywords', () => {
  test('owner can edit an OCR keyword entry in manage mode', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await enterManageMode(page, DEFAULT_PASSWORD);

    const card = page
      .locator('.ocr-keyword-card')
      .filter({ has: page.locator('.ocr-keyword-lang').filter({ hasText: /^EN$/ }) });
    await card.waitFor({ state: 'visible', timeout: 15000 });
    await expect(card.locator('.ocr-keyword-currency')).toHaveText('EUR');

    await card.locator('.btn-edit.small').click();
    await card.locator('.ocr-keyword-fields select').selectOption('CHF');
    await card.locator('.ocr-keyword-actions .btn-primary.small').click();
    await expect(card.locator('.ocr-keyword-currency')).toHaveText('CHF');

    await card.locator('.btn-edit.small').click();
    await card.locator('.ocr-keyword-fields select').selectOption('EUR');
    await card.locator('.ocr-keyword-actions .btn-primary.small').click();
    await expect(card.locator('.ocr-keyword-currency')).toHaveText('EUR');
  });
});
