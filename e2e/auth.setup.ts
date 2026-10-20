import { test as setup, expect } from '@playwright/test';
import path from 'node:path';
import fs from 'node:fs';
import { login } from './helpers/login';

const AUTH_DIR = path.join(__dirname, '.auth');
const AUTH_FILE = path.join(AUTH_DIR, 'admin.json');

setup('логин и сохранение сессии', async ({ page }) => {
    // Создаём папку .auth, если её нет
    if (!fs.existsSync(AUTH_DIR)) {
        fs.mkdirSync(AUTH_DIR, { recursive: true });
    }

    // Логинимся
    await login(page, 'admin', 'admin123');

    // Убеждаемся, что layout отрисовался
    await expect(page.locator('turbo-frame#app')).toBeVisible();
    await expect(page.locator('.home-title')).toHaveText('Производство');

    // Сохраняем сессию
    await page.context().storageState({ path: AUTH_FILE });
});