import { test, expect } from '@playwright/test';

const DEFAULT_PASSWORD = process.env.DEFAULT_PASSWORD;
if (!DEFAULT_PASSWORD) {
  throw new Error('DEFAULT_PASSWORD environment variable is required to run e2e tests');
}

test.describe('Inspection E2E', () => {

  test('save inspection, generate PDF, and download it', async ({ page }) => {
    test.setTimeout(120000);

    let alertText = '';
    page.on('dialog', async (dialog) => {
      alertText = dialog.message();
      await dialog.accept();
    });

    await page.goto('/login');
    await page.waitForSelector('.login-form', { timeout: 15000 });
    await page.getByPlaceholder('Username').fill('owner');
    await page.getByPlaceholder('Password').fill(DEFAULT_PASSWORD);
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.waitForSelector('.apartment-stack', { timeout: 20000 });
    await page.locator('.apartment-row').first().click();
    await page.waitForSelector('.detail-layout', { timeout: 15000 });

    await page.getByRole('link', { name: /new inspection/i }).click();
    await page.waitForSelector('.inspection-form', { timeout: 15000 });

    // Wait for apartment fetch (property auto-filled) to settle
    await page.waitForTimeout(2000);

    // Use getByLabel for fields wrapped in <label>; scope to the Moving in Tenant section
    const movingIn = page.locator('.insp-section').filter({ hasText: /moving in tenant/i });
    await movingIn.getByLabel('Name', { exact: true }).fill('Test GmbH');
    await movingIn.getByLabel('Address', { exact: true }).fill('Musterstr. 1');
    await movingIn.getByLabel('Postal Code', { exact: true }).fill('12345');
    await movingIn.getByLabel('City', { exact: true }).fill('Berlin');
    await movingIn.getByLabel('Phone', { exact: true }).fill('+49 30 12345678');
    await movingIn.getByLabel('Email', { exact: true }).fill('test@example.com');

    await page.getByLabel('Object Number').fill('OBJ-001');
    await page.getByLabel('Rental Object').fill('Whg 3 OG');
    await page.getByLabel('Incoming Party').fill('Max Mustermann');

    await page.getByRole('button', { name: /add section/i }).click();
    await page.waitForSelector('.insp-card', { timeout: 5000 });
    await page.locator('.insp-section-title-input').fill('Wohnzimmer');

    await page.getByRole('button', { name: /add row/i }).click();
    const row = page.locator('.insp-row').first();
    await row.locator('input').first().fill('Boden');
    await row.locator('textarea').fill('Laminat, Kratzer im Eingangsbereich');
    await row.locator('select').selectOption('defect');
    await row.getByPlaceholder('Cost share').fill('50%');

    // Save first
    alertText = '';
    await page.getByRole('button', { name: /^save$/i }).click();
    await expect.poll(() => alertText.toLowerCase()).toContain('saved');

    // Generate PDF
    alertText = '';
    await page.getByRole('button', { name: /generate pdf/i }).click();
    await expect.poll(() => alertText.toLowerCase()).toContain('pdf generated');

    // Download
    await page.getByRole('button', { name: /download pdf/i }).waitFor({ state: 'visible', timeout: 10000 });

    const downloadPromise = page.waitForEvent('download', { timeout: 20000 });
    await page.getByRole('button', { name: /download pdf/i }).click();

    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('Uebergabeprotokoll');
    expect(download.suggestedFilename()).toContain('.pdf');
    expect(await download.path()).toBeTruthy();
  });

  test('login as tenant cannot access inspection form', async ({ page }) => {
    test.setTimeout(60000);

    await page.goto('/login');
    await page.waitForSelector('.login-form', { timeout: 15000 });
    await page.getByPlaceholder('Username').fill('tenant');
    await page.getByPlaceholder('Password').fill(DEFAULT_PASSWORD);
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.waitForSelector('.apartment-stack', { timeout: 20000 });
    await page.locator('.apartment-row').first().click();
    await page.waitForSelector('.detail-layout', { timeout: 15000 });
    await expect(page.getByRole('link', { name: /new inspection/i })).toHaveCount(0);

    await page.goto('/apartments/1/inspections/new');
    await page.waitForTimeout(3000);
    await expect(page.locator('.inspection-form')).toHaveCount(0);
  });
});
