import { test, expect } from '@playwright/test';
import { login } from '../helpers/login';

test.use({ storageState: { cookies: [], origins: [] } });

test('успешный логин перенаправляет на /layout', async ({ page }) => {
    await login(page, 'admin', 'admin123');

    // Убеждаемся, что layout отрисовался
    await expect(page.locator('turbo-frame#app')).toBeVisible();
    await expect(page.locator('.home-title')).toHaveText('Производство');
});

test('неверный пароль оставляет на /login с ошибкой', async ({ page }) => {
    await page.goto('/login');

    // Программно отправляем форму с неправильным паролем
    await page.evaluate(() => {
        const form = document.querySelector('#authForm form') as HTMLFormElement;
        const uField = form.querySelector('input[name="username"]') as HTMLInputElement;
        const pField = form.querySelector('input[name="password"]') as HTMLInputElement;

        uField.value = 'admin';
        pField.value = 'НЕПРАВИЛЬНЫЙ';
        form.submit();
    });

    // Spring Security редиректит на /login?error=true
    await page.waitForURL(/\/login\?error/, { timeout: 10_000 });
});