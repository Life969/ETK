package ProjectForJob.example.Job.utill;


import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.util.OrderCostCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты формулы расчёта стоимости (Фаза 2: с группой прочности и весозависимыми работами).
 *
 * Напоминание по семантике:
 *   - additionalWork.price — теперь цена ЗА ТОННУ;
 *   - вес для биллинга = max(W_total, 100).
 */
class OrderCostCalculatorTest {

    // ============================================================
    // БАЗОВЫЕ СЦЕНАРИИ БЕЗ ГРУППЫ И РАБОТ
    // ============================================================

    @Test
    @DisplayName("1 шт, без группы, без работ → цена = unitCost")
    void singleItemNoGroupNoWorks() {
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("1000.00"), List.of(), 1, null, 150.0);
        assertEquals(new BigDecimal("1000.00"), result);
    }

    @Test
    @DisplayName("3 шт, без группы, без работ → unitCost * 3")
    void threeItemsNoGroupNoWorks() {
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("2500.00"), List.of(), 3, null, 300.0);
        assertEquals(new BigDecimal("7500.00"), result);
    }

    // ============================================================
    // НАДБАВКА ЗА ГРУППУ ПРОЧНОСТИ
    // ============================================================

    @Test
    @DisplayName("Группа Е (10%): baseCost * 1.10")
    void markupTenPercent() {
        // 1000 * 2 = 2000, надбавка 10% = 200 → 2200
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("1000.00"), List.of(), 2,
                new BigDecimal("10.00"), 200.0);
        assertEquals(new BigDecimal("2200.00"), result);
    }

    @Test
    @DisplayName("Группа Р (30%) + дробное: 333.33 * 3 = 999.99; +30% = 1299.99")
    void markupThirtyPercentFractional() {
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("333.33"), List.of(), 3,
                new BigDecimal("30.00"), 200.0);
        // 333.33 * 3 = 999.99
        // * 1.30 = 1299.987 → 1299.99 (scale=2 HALF_UP)
        assertEquals(new BigDecimal("1299.99"), result);
    }

    // ============================================================
    // ДОП. РАБОТЫ ЧЕРЕЗ ВЕС
    // ============================================================

    @Test
    @DisplayName("1 работа (1000 руб/т), вес 500 кг → 500 руб")
    void worksHalfTon() {
        // base = 1000, group 0%, вес 500 кг → 1000 * 500/1000 = 500
        List<AdditionalWorkEntity> works = List.of(work("1000.00"));
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("1000.00"), works, 1,
                BigDecimal.ZERO, 500.0);
        assertEquals(new BigDecimal("1500.00"), result);
    }

    @Test
    @DisplayName("1 работа (1000 руб/т), вес 50 кг → подтягивается до 100 кг → 100 руб")
    void worksUnderMinWeight() {
        // вес 50 < 100 → биллинг как 100 кг → 1000 * 100/1000 = 100
        List<AdditionalWorkEntity> works = List.of(work("1000.00"));
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("1000.00"), works, 1,
                BigDecimal.ZERO, 50.0);
        assertEquals(new BigDecimal("1100.00"), result);
    }

    @Test
    @DisplayName("2 работы (500 + 300 руб/т), вес 2000 кг → base + 1600")
    void twoWorksTwoTons() {
        // (500 + 300) * 2000 / 1000 = 1600
        List<AdditionalWorkEntity> works = List.of(work("500.00"), work("300.00"));
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("2000.00"), works, 1,
                BigDecimal.ZERO, 2000.0);
        assertEquals(new BigDecimal("3600.00"), result);
    }

    // ============================================================
    // КОМБИНИРОВАННЫЕ СЦЕНАРИИ
    // ============================================================

    @Test
    @DisplayName("Всё вместе: 2 шт × 1000, группа Е 10%, 1 работа 1000 руб/т, вес 500 кг")
    void combined() {
        // base = 2000, markup = 200, productTotal = 2200
        // works = 1000 * 500/1000 = 500
        // total = 2700
        List<AdditionalWorkEntity> works = List.of(work("1000.00"));
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("1000.00"), works, 2,
                new BigDecimal("10.00"), 500.0);
        assertEquals(new BigDecimal("2700.00"), result);
    }

    // ============================================================
    // ЗАЩИТА ОТ НУЛЕЙ/ПУСТЫХ
    // ============================================================

    @Test
    @DisplayName("weightKg = null → биллинг как 100 кг")
    void nullWeightFallsToMin() {
        List<AdditionalWorkEntity> works = List.of(work("1000.00"));
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("100.00"), works, 1,
                null, null);
        // 100 + 1000 * 100/1000 = 200
        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    @DisplayName("additionalWorks = null → трактуем как пустой список")
    void nullWorks() {
        BigDecimal result = OrderCostCalculator.calculate(
                new BigDecimal("100.00"), null, 1, null, 100.0);
        assertEquals(new BigDecimal("100.00"), result);
    }

    private AdditionalWorkEntity work(String price) {
        return AdditionalWorkEntity.builder()
                .name("test-work")
                .price(new BigDecimal(price))
                .build();
    }
}