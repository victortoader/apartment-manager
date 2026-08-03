import { test, expect } from '@playwright/test';
import { DEFAULT_PASSWORD, loginAs } from './helpers';

test.describe('Login', () => {
  test('owner logs in and sees OWNER role in header', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'owner', DEFAULT_PASSWORD);
    await expect(page.locator('.header-user')).toContainText('owner');
    await expect(page.locator('.header-user')).toContainText('OWNER');
  });

  test('tenant logs in and sees TENANT role in header', async ({ page }) => {
    test.setTimeout(60000);
    await loginAs(page, 'tenant', DEFAULT_PASSWORD);
    await expect(page.locator('.header-user')).toContainText('tenant');
    await expect(page.locator('.header-user')).toContainText('TENANT');
  });

  test('wrong password shows an error and stays on the login page', async ({ page }) => {
    test.setTimeout(60000);
    await page.goto('/login');
    await page.waitForSelector('.login-form', { timeout: 15000 });
    await page.getByPlaceholder('Username').fill('owner');
    await page.getByPlaceholder('Password').fill('definitely-wrong-password');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.locator('.login-error')).toBeVisible();
    await expect(page.locator('.login-form')).toBeVisible();
  });
});
