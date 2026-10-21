import { test, expect } from '@playwright/test';
import { waitForTurboFrame, turboClick } from '../helpers/turbo';

test.describe('Журнал производства', () => {

    test('первая страница: 20 записей и счётчик 10000', async ({ page }) => {
        await page.goto('/production-records');
        await waitForTurboFrame(page);

        // Заголовок страницы
        await expect(page.locator('.handbook-title'))
            .toContainText('Журнал производства');

        // Счётчик в шапке — должно быть «10000 зап.»
        await expect(page.locator('.handbook-count')).toContainText('10000');

        // На странице ровно 20 записей (пагинация по 20)
        await expect(page.locator('article.journal-entry')).toHaveCount(20);
    });

    test('пагинация: клик "Вперёд" меняет содержимое и URL', async ({ page }) => {
        await page.goto('/production-records');
        await waitForTurboFrame(page);

        // Запоминаем название первой записи до клика
        const firstTitleBefore = await page
            .locator('article.journal-entry .journal-entry-title')
            .first()
            .innerText();

        // Кликаем "Вперёд" — turboClick дождётся, пока фрейм обновится
        await turboClick(page, 'a.hb-page-btn:has-text("Вперёд")');

        // Название первой записи изменилось
        const firstTitleAfter = await page
            .locator('article.journal-entry .journal-entry-title')
            .first()
            .innerText();
        expect(firstTitleAfter).not.toBe(firstTitleBefore);

        // URL обновился — Turbo должен был обновить адресную строку
        await expect(page).toHaveURL(/page=1/);

        // Информация о странице тоже обновилась: «2 / 500» (или похожее)
        await expect(page.locator('.hb-page-info')).toContainText('2 /');
    });

    test('прыжок на 250-ю страницу не таймаутит', async ({ page }) => {
        const t0 = Date.now();

        await page.goto('/production-records?page=250&size=20');
        await waitForTurboFrame(page);

        const elapsed = Date.now() - t0;

        // Страница отрендерилась
        await expect(page.locator('article.journal-entry')).toHaveCount(20);
        await expect(page.locator('.hb-page-info')).toContainText('251 /');

        // Уложились в разумный таймаут (с запасом — это регрессионный индикатор)
        expect(elapsed).toBeLessThan(5000);
    });

    test('фильтр по сотруднику сохраняется в URL', async ({ page }) => {
        await page.goto('/production-records');
        await waitForTurboFrame(page);

        // Раскрываем панель фильтров, если она свёрнута
        const filtersBlock = page.locator('#journalFilters');
        const isOpen = await filtersBlock.evaluate(
            (el) => el.classList.contains('open'),
        );
        if (!isOpen) {
            await page.click('#filtersToggle');
            // короткая пауза на CSS-анимацию раскрытия
            await page.waitForTimeout(300);
        }

        // Выбираем первого сотрудника в списке (индекс 1 — пропускаем "Все")
        await page.selectOption('#employeeId', { index: 1 });

        // Кликаем "Применить" и ждём, пока URL получит параметр employeeId
        await Promise.all([
            page.waitForURL(/employeeId=\d+/, { timeout: 10_000 }),
            page.click('button:has-text("Применить")'),
        ]);

        await waitForTurboFrame(page);

        // Селект сохранил значение
        await expect(page.locator('#employeeId')).not.toHaveValue('');

        // Список отрендерился (может быть пустым, если у сотрудника нет записей,
        // но блок `.journal-list` или `.journal-empty` должен быть в DOM)
        const hasList = await page.locator('.journal-list').count();
        const hasEmpty = await page.locator('.journal-empty').count();
        expect(hasList + hasEmpty).toBeGreaterThan(0);
    });

});