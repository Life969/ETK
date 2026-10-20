import { Page, expect } from '@playwright/test';

/**
 * Ждёт, пока Turbo-фрейм #app закончит навигацию.
 *
 * Turbo ставит на фрейм атрибут `busy` на время загрузки.
 * Как только атрибут пропал — навигация завершена.
 */
export async function waitForTurboFrame(page: Page): Promise<void> {
    const frame = page.locator('turbo-frame#app');
    await frame.waitFor({ state: 'visible', timeout: 10_000 });

    await page.waitForFunction(
        () => {
            const f = document.querySelector('turbo-frame#app');
            return f !== null && !f.hasAttribute('busy');
        },
        null,
        { timeout: 15_000 },
    );
}

/**
 * Кликает по элементу внутри Turbo-фрейма и ждёт завершения навигации.
 *
 * Возвращает текст содержимого фрейма ДО клика — чтобы вызывающий код
 * мог сравнить с содержимым ПОСЛЕ и убедиться, что что-то изменилось.
 */
export async function turboClick(
    page: Page,
    selector: string,
): Promise<string> {
    const before = await page.locator('turbo-frame#app').innerHTML();

    await page.click(selector);

    // Ждём, пока Turbo завершит навигацию: атрибут busy снят
    // И содержимое фрейма изменилось
    await page.waitForFunction(
        (prev: string) => {
            const f = document.querySelector('turbo-frame#app');
            if (!f || f.hasAttribute('busy')) return false;
            return f.innerHTML !== prev;
        },
        before,
        { timeout: 15_000 },
    );

    return before;
}

/**
 * Проверяет, что страница — это журнал (используется как sanity-check).
 */
export async function expectOnJournalPage(page: Page): Promise<void> {
    await expect(page.locator('turbo-frame#app')).toBeVisible();
    await expect(page.locator('.handbook-title')).toContainText('Журнал производства');
}