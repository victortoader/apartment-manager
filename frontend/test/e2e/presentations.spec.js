import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Presentation pages', () => {
  test('guest can browse the public presentation list', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });

    const sunny = page.locator('.pl-card').filter({ hasText: 'Sunny Studio' });
    const garden = page.locator('.pl-card').filter({ hasText: 'Garden Flat' });
    const loft = page.locator('.pl-card').filter({ hasText: 'City Loft' });

    await expect(sunny.locator('.pl-status')).toContainText('Available Immediately');
    await expect(garden.locator('.pl-status')).toContainText('Available Immediately');
    await expect(loft.locator('.pl-status')).toContainText('Available from');

    for (const card of [sunny, garden, loft]) {
      await expect(card.getByRole('button', { name: /^apply$/i })).toBeVisible();
      await expect(card.getByRole('link', { name: /view details/i })).toBeVisible();
    }
  });

  test('guest opens an apartment presentation and sees its details', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });

    await page.locator('.pl-card').filter({ hasText: 'City Loft' }).getByRole('link', { name: /view details/i }).click();
    await page.waitForSelector('.pres-hero', { timeout: 15000 });

    await expect(page.locator('.pres-hero h1')).toHaveText('City Loft');
    await expect(page.locator('.pres-location')).toContainText('789 Industrial Way');
    await expect(page.locator('.pres-stat-value').first()).toContainText('1500');
    await expect(page.locator('.pres-body-text')).toContainText('Modern loft');
    await expect(page.locator('.pres-rich-text')).toContainText('industrial loft');
  });

  test('gallery carousel navigates through the apartment photos', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });

    await page.locator('.pl-card').filter({ hasText: 'Garden Flat' }).getByRole('link', { name: /view details/i }).click();
    await page.waitForSelector('.pres-gallery-thumbs', { timeout: 15000 });

    const counter = page.locator('.pres-gallery-counter');
    const total = parseInt((await counter.innerText()).split('/')[1].trim(), 10);
    expect(total).toBeGreaterThan(1);
    await expect(page.locator('.pres-thumb')).toHaveCount(total);
    await expect(counter).toHaveText(`1 / ${total}`);

    await page.locator('.pres-gallery-arrow.right').click();
    await expect(counter).toHaveText(`2 / ${total}`);

    await page.locator('.pres-gallery-arrow.left').click();
    await expect(counter).toHaveText(`1 / ${total}`);
  });

  test('owner can edit the presentation and save the changes', async ({ page }) => {
    test.setTimeout(120000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await page.goto('/presentations');
    await page.waitForSelector('.pl-card', { timeout: 15000 });

    await page.locator('.pl-card').filter({ hasText: 'Garden Flat' }).getByRole('link', { name: /view details/i }).click();
    await page.waitForSelector('.pres-hero', { timeout: 15000 });

    const originalPrice = (await page.locator('.pres-stat-value').first().innerText()).trim();
    const originalNum = parseFloat(originalPrice.replace(/[^\d.]/g, ''));
    const changedNum = originalNum + 1;
    const originalDesc = (await page.locator('.pres-body-text').innerText()).trim();

    await page.getByRole('button', { name: /edit presentation/i }).click();
    await page.locator('.pres-price-input').fill(String(changedNum));
    await page.getByPlaceholder('Short description of the apartment...').fill('E2E updated presentation description');
    await page.getByRole('button', { name: /^save$/i }).click();

    await expect(page.locator('.pres-stat-value').first()).toContainText(String(changedNum));
    await expect(page.locator('.pres-body-text')).toHaveText('E2E updated presentation description');

    await page.getByRole('button', { name: /edit presentation/i }).click();
    await page.locator('.pres-price-input').fill(String(originalNum));
    await page.getByPlaceholder('Short description of the apartment...').fill(originalDesc);
    await page.getByRole('button', { name: /^save$/i }).click();
    await page.waitForFunction(() => !document.querySelector('.pres-editor'), null, { timeout: 15000 });
    await page.reload();
    await page.waitForSelector('.pres-hero', { timeout: 15000 });
    await expect(page.locator('.pres-stat-value').first()).toContainText(String(originalNum));
    await expect(page.locator('.pres-body-text')).toHaveText(originalDesc);
  });
});
