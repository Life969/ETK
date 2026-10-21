import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

export default defineConfig({
    // Где искать тесты
    testDir: '.',

    // Таймаут одного теста (если тест идёт дольше — падает)
    timeout: 30_000,

    // По умолчанию 5 секунд на проверку `expect(...)`.
    // У нас иногда медленные страницы — поставим 10.
    expect: { timeout: 10_000 },

    // Тесты, которые меняют данные (создают/удаляют заказы),
    // лучше запускать по одному, чтобы не мешали друг другу.
    fullyParallel: false,
    workers: 1,

    // Не перезапускать упавшие тесты автоматически
    retries: 0,

    // Отчёты: HTML (красивый) + список в консоль
    reporter: [
        ['html', { open: 'never', outputFolder: 'playwright-report' }],
        ['list'],
    ],

    // Общие настройки для всех тестов
    use: {
        baseURL: 'http://localhost:8080',

        // Сохраняем артефакты только при падении — иначе диск забьётся
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',

        // По умолчанию используем сохранённую сессию
        storageState: path.join(__dirname, '.auth', 'admin.json'),
    },

    // Проекты: сначала setup, потом тесты
    projects: [
        {
            name: 'setup',
            testMatch: /auth\.setup\.ts/,
            // ВАЖНО: setup НЕ читает сохранённую сессию — он её создаёт.
            // Передаём пустое состояние, чтобы Playwright не искал файл.
            use: { storageState: { cookies: [], origins: [] } },
        },
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
            dependencies: ['setup'],
        },
    ],
});