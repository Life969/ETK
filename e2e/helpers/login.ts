import { Page, expect } from '@playwright/test';

/**
 * Логинится через POST формы логина.
 *
 * Работает напрямую с DOM-элементами, минуя hit-testing Playwright.
 * Это обходит проблемы, когда:
 *   - кнопка "Войти" не успевает отрисоваться из-за CSS-анимации/шрифтов;
 *   - что-то перекрывает кнопку (panelBody intercepts pointer events);
 *   - форма логина находится в скрытом (неактивном) состоянии.
 *
 * Что делает:
 *   1. Открывает /login
 *   2. Находит форму логина в DOM (сначала в auth-form, потом fallback)
 *   3. Заполняет username/password
 *   4. Отправляет форму через form.submit()
 *   5. Ждёт уход со страницы логина
 */
export async function login(
    page: Page,
    username: string,
    password: string,
): Promise<void> {
    await page.goto('/login');

    // Программный submit формы через DOM.
    // Работает независимо от CSS-видимости и анимаций.
    await page.evaluate(
        ({ u, p }) => {
            // Пробуем найти форму в блоке auth-form (второе состояние панели)
            let form = document.querySelector('#authForm form') as HTMLFormElement | null;

            // Fallback: ищем любую форму с полем username
            if (!form) {
                const uField = document.querySelector(
                    'input[name="username"]',
                ) as HTMLInputElement | null;
                if (uField && uField.form) {
                    form = uField.form;
                }
            }

            if (!form) {
                throw new Error('Форма логина не найдена на странице');
            }

            const uField = form.querySelector(
                'input[name="username"]',
            ) as HTMLInputElement | null;
            const pField = form.querySelector(
                'input[name="password"]',
            ) as HTMLInputElement | null;

            if (!uField || !pField) {
                throw new Error('Поля username/password не найдены в форме');
            }

            uField.value = u;
            pField.value = p;

            // form.submit() отправляет POST без HTML5-валидации
            // и без события submit — ровно то, что нужно для UI-теста
            form.submit();
        },
        { u: username, p: password },
    );

    // Ждём, пока URL уйдёт со страницы логина
    await page.waitForURL((url) => !url.pathname.startsWith('/login'), {
        timeout: 15_000,
    });
}

export async function expectOnLoginPage(page: Page): Promise<void> {
    await expect(page).toHaveURL(/\/login/);
}