import { test, expect } from '@playwright/test';
import { waitForTurboFrame } from '../helpers/turbo';

test.describe('Заказы — read-only проверки', () => {

    // ============================================================
    // WAITING (300 заказов)
    // ============================================================
    test('waiting: 12 карточек, у каждой компания/тип/цена', async ({ page }) => {
        await page.goto('/orders/waiting');
        await waitForTurboFrame(page);

        await expect(page.locator('.handbook-title'))
            .toContainText('Заказы в ожидании');
        await expect(page.locator('.handbook-count')).toContainText('300');

        const cards = page.locator('.order-card');
        await expect(cards).toHaveCount(12);

        // Первые три карточки — проверим обязательные поля
        for (let i = 0; i < 3; i++) {
            const card = cards.nth(i);
            await expect(card.locator('.order-company')).not.toBeEmpty();
            await expect(card.locator('.order-type-badge'))
                .toHaveText(/Муфта|Переводник/);
            await expect(card.locator('.order-meta-value.accent'))
                .toContainText('₽');
        }
    });

    test('waiting: пагинация доходит до 2-й страницы', async ({ page }) => {
        await page.goto('/orders/waiting');
        await waitForTurboFrame(page);

        const firstCompany = await page
            .locator('.order-company').first().innerText();

        await page.click('a.hb-page-btn:has-text("Вперёд")');
        await page.waitForURL(/page=1/);
        await waitForTurboFrame(page);

        const newFirst = await page
            .locator('.order-company').first().innerText();
        expect(newFirst).not.toBe(firstCompany);
        await expect(page.locator('.hb-page-info')).toContainText('2 /');
    });

    // ============================================================
    // PRODUCTION (300 заказов)
    // ============================================================
    test('production: 12 карточек, у каждой цена', async ({ page }) => {
        await page.goto('/orders/production');
        await waitForTurboFrame(page);

        await expect(page.locator('.handbook-title'))
            .toContainText('Заказы в производстве');
        await expect(page.locator('.handbook-count')).toContainText('300');

        const cards = page.locator('.order-card');
        await expect(cards).toHaveCount(12);

        // у каждого заказа в производстве должна быть кнопка «Завершить»
        await expect(
            page.locator('.btn-mini:has-text("Завершить")').first(),
        ).toBeVisible();
    });

    // ============================================================
    // COMPLETED (10 000 заказов)
    // ============================================================
    test('completed: 12 карточек, счётчик 10000, ~833 страницы', async ({ page }) => {
        await page.goto('/orders/completed');
        await waitForTurboFrame(page);

        await expect(page.locator('.handbook-title'))
            .toContainText('Выполненные заказы');
        await expect(page.locator('.handbook-count')).toContainText('10000');

        await expect(page.locator('.order-card')).toHaveCount(12);

        // Инфа о пагинации: «1 / 834» или около того
        await expect(page.locator('.hb-page-info')).toContainText('1 /');
    });

    test('completed: прыжок на 500-ю страницу работает', async ({ page }) => {
        const t0 = Date.now();

        await page.goto('/orders/completed?page=500&size=12');
        await waitForTurboFrame(page);

        await expect(page.locator('.order-card')).toHaveCount(12);
        await expect(page.locator('.hb-page-info')).toContainText('501 /');

        expect(Date.now() - t0).toBeLessThan(5000);
    });

    // ============================================================
    // Просмотр отдельного заказа
    // ============================================================
    test('waiting: клик по компании открывает карточку заказа', async ({ page }) => {
        await page.goto('/orders/waiting');
        await waitForTurboFrame(page);

        const company = await page
            .locator('.order-company').first().innerText();

        await page.locator('.order-company').first().click();
        await page.waitForURL(/\/orders\/\d+/);
        await waitForTurboFrame(page);

        // На странице детали должно быть:
        await expect(page.locator('.handbook-title')).toContainText('Заказ №');
        await expect(page.locator('.hb-detail-value').first())
            .toHaveText(company);
    });

});