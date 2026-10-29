package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;;
import ProjectForJob.example.Job.exception.util.CalculatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты для {@link PipeOrderCostCalculator}.
 *
 * Калькулятор используется для PIPE и NIPPLE:
 *   baseCost = threadsCost (Q × 2 × manufacturingCost)
 *            + metalCost   (pricePerTon × weight / 1000)
 *   + надбавка за группу прочности
 *   + доп. работы (минимум 100 кг для тарификации работ)
 */
@DisplayName("PipeOrderCostCalculator: стоимость труб/патрубков")
class PipeOrderCostCalculatorTest {

       // Хелперы


    private static AdditionalWorkEntity work(String name, String price) {
        return AdditionalWorkEntity.builder()
                .name(name)
                .price(new BigDecimal(price))
                .build();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }


    // 1. ВАЛИДАЦИЯ


    @Nested
    @DisplayName("Валидация входа")
    class Validation {

        @Test
        @DisplayName("quantity = 0 → исключение")
        void zero_quantity_throws() {
            assertThatThrownBy(() -> PipeOrderCostCalculator.calculate(
                    new BigDecimal("100"), new BigDecimal("50000"),
                    0, 100.0, List.of(), null))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Количество");
        }

        @Test
        @DisplayName("quantity < 0 → исключение")
        void negative_quantity_throws() {
            assertThatThrownBy(() -> PipeOrderCostCalculator.calculate(
                    new BigDecimal("100"), new BigDecimal("50000"),
                    -3, 100.0, List.of(), null))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Количество");
        }
    }


    //  threadsCost = Q × 2 × manufacturingCost


    @Nested
    @DisplayName("threadsCost: Q × 2 × manufacturingCost")
    class ThreadsCost {

        @Test
        @DisplayName("2 нарезки × 500 руб × 10 шт = 10 000 руб")
        void threads_cost_two_sides_per_piece() {
            // metalCost = 0 (pricePerTon = 0)
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("500"), BigDecimal.ZERO,
                    10, 100.0, List.of(), null);

            assertThat(result).isEqualTo(money("10000.00"));
        }

        @Test
        @DisplayName("threadManufacturingCost = null → 0")
        void null_thread_cost_is_zero() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    null, BigDecimal.ZERO,
                    10, 100.0, List.of(), null);

            assertThat(result).isEqualTo(money("0.00"));
        }

        @Test
        @DisplayName("Нарезка с двух сторон: 1 шт × 2 × 300 = 600")
        void single_piece_two_threads() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("300"), BigDecimal.ZERO,
                    1, 100.0, List.of(), null);

            assertThat(result).isEqualTo(money("600.00"));
        }
    }


    //  metalCost = pricePerTon × weight / 1000


    @Nested
    @DisplayName("metalCost: pricePerTon × weight / 1000")
    class MetalCost {

        @Test
        @DisplayName("50 000 руб/т × 1000 кг / 1000 = 50 000 руб")
        void one_ton_cost() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, new BigDecimal("50000"),
                    1, 1000.0, List.of(), null);

            assertThat(result).isEqualTo(money("50000.00"));
        }

        @Test
        @DisplayName("50 000 руб/т × 500 кг = 25 000 руб")
        void half_ton_cost() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, new BigDecimal("50000"),
                    1, 500.0, List.of(), null);

            assertThat(result).isEqualTo(money("25000.00"));
        }

        @Test
        @DisplayName("weight = null → metalCost = 0 (не поднимается до 100 кг!)")
        void null_weight_gives_zero_metal_cost() {
            // ЭТО ВАЖНО: для металла вес не поднимается до 100 кг,
            // в отличие от доп. работ.
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, new BigDecimal("50000"),
                    1, null, List.of(), null);

            assertThat(result).isEqualTo(money("0.00"));
        }

        @Test
        @DisplayName("weight <= 0 → metalCost = 0")
        void non_positive_weight_gives_zero_metal_cost() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, new BigDecimal("50000"),
                    1, -100.0, List.of(), null);

            assertThat(result).isEqualTo(money("0.00"));
        }

        @Test
        @DisplayName("pricePerTon = null → metalCost = 0")
        void null_price_per_ton_is_zero() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, null,
                    1, 1000.0, List.of(), null);

            assertThat(result).isEqualTo(money("0.00"));
        }
    }


    //  КЛЮЧЕВОЕ: вес для металла ≠ вес для работ


    @Nested
    @DisplayName("Разный вес: metalCost vs worksCost")
    class DifferentWeights {

        @Test
        @DisplayName("weight = 50 кг: металл от 50, работы от 100")
        void metal_uses_real_weight_works_uses_minimum() {
            // threadsCost = 0 (нет нарезки)
            // metalCost   = 50000 × 50 / 1000 = 2500
            // worksCost   = 1000 × 100 / 1000 = 100  (минимум 100 кг!)
            // total       = 2600
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO,
                    new BigDecimal("50000"),
                    1, 50.0,
                    List.of(work("Работа", "1000")),
                    null);

            assertThat(result).isEqualTo(money("2600.00"));
        }

        @Test
        @DisplayName("weight = null: металл = 0, работы = от 100 кг")
        void null_weight_metal_zero_works_minimum() {
            // metalCost = 0 (нет веса)
            // worksCost = 1000 × 100 / 1000 = 100
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO,
                    new BigDecimal("50000"),
                    1, null,
                    List.of(work("Работа", "1000")),
                    null);

            assertThat(result).isEqualTo(money("100.00"));
        }

        @Test
        @DisplayName("weight = 500 кг: и металл, и работы от 500")
        void weight_above_minimum_used_for_both() {
            // metalCost = 50000 × 500 / 1000 = 25000
            // worksCost = 1000 × 500 / 1000 = 500
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO,
                    new BigDecimal("50000"),
                    1, 500.0,
                    List.of(work("Работа", "1000")),
                    null);

            assertThat(result).isEqualTo(money("25500.00"));
        }
    }


    //  НАДБАВКА (применяется к baseCost, не к работам)


    @Nested
    @DisplayName("Надбавка за группу прочности")
    class Markup {

        @Test
        @DisplayName("threadsCost 1000 + metalCost 0, 10% → 1100")
        void markup_on_threads_only() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("500"), BigDecimal.ZERO,
                    1, 100.0, List.of(), new BigDecimal("10"));

            // threadsCost = 500 × 2 × 1 = 1000
            // markup = 100, total = 1100
            assertThat(result).isEqualTo(money("1100.00"));
        }

        @Test
        @DisplayName("markupPercent = null → 0%")
        void null_markup_is_zero() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("500"), BigDecimal.ZERO,
                    1, 100.0, List.of(), null);

            assertThat(result).isEqualByComparingTo(money("1000.00"));
        }

        @Test
        @DisplayName("Надбавка НЕ применяется к worksCost")
        void markup_not_applied_to_works() {
            // threadsCost = 1000, markup 10% → 1100
            // worksCost = 1000 × 100 / 1000 = 100 (не облагается markup)
            // total = 1200
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("500"), BigDecimal.ZERO,
                    1, 100.0,
                    List.of(work("Работа", "1000")),
                    new BigDecimal("10"));

            assertThat(result).isEqualTo(money("1200.00"));
        }
    }


    //  ДОП. РАБОТЫ


    @Nested
    @DisplayName("Доп. работы")
    class AdditionalWorks {

        @Test
        @DisplayName("Несколько работ — цены суммируются")
        void multiple_works_summed() {
            // worksCost = (1000 + 500) × 100 / 1000 = 150
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    1, 100.0,
                    List.of(work("A", "1000"), work("B", "500")),
                    null);

            assertThat(result).isEqualTo(money("150.00"));
        }

        @Test
        @DisplayName("null-элемент и null-цена игнорируются")
        void null_elements_skipped() {
            List<AdditionalWorkEntity> works = Arrays.asList(
                    work("A", "1000"),
                    null,
                    AdditionalWorkEntity.builder().name("B").price(null).build(),
                    work("C", "500"));

            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    1, 100.0, works, null);

            assertThat(result).isEqualTo(money("150.00"));
        }

        @Test
        @DisplayName("null-список работ → worksCost = 0")
        void null_works_list() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("500"), BigDecimal.ZERO,
                    1, 100.0, null, null);

            assertThat(result).isEqualByComparingTo(money("1000.00"));
        }
    }


    // ОКРУГЛЕНИЕ


    @Nested
    @DisplayName("Округление")
    class Rounding {

        @Test
        @DisplayName("Результат всегда scale=2")
        void result_has_scale_two() {
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("333.33"), new BigDecimal("50000"),
                    3, 123.45, List.of(), null);

            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Дробный вес округляется HALF_UP на финальном шаге")
        void fractional_weight_rounds_half_up() {
            // metalCost = 100 × 333.33 / 1000 = 33.333 → 33.33
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    BigDecimal.ZERO, new BigDecimal("100"),
                    1, 333.33, List.of(), null);

            assertThat(result).isEqualTo(money("33.33"));
        }
    }


    // КОМБИНИРОВАННЫЙ СЦЕНАРИЙ


    @Nested
    @DisplayName("Комбинированные сценарии")
    class Combined {

        @Test
        @DisplayName("Труба: 10 шт × 2 × 300 руб + 50 т × 50000 + 10% + работы")
        void pipe_full_scenario() {
            // threadsCost = 10 × 2 × 300 = 6 000
            // metalCost   = 50000 × 1000 / 1000 = 50 000
            // baseCost    = 56 000
            // markup      = 5 600
            // productTotal = 61 600
            // worksCost   = 1000 × 1000 / 1000 = 1 000
            // total       = 62 600
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("300"),
                    new BigDecimal("50000"),
                    10, 1000.0,
                    List.of(work("Покраска", "1000")),
                    new BigDecimal("10"));

            assertThat(result).isEqualTo(money("62600.00"));
        }

        @Test
        @DisplayName("Патрубок: 5 шт × 2 × 200 + 50 кг металла, без надбавки")
        void nipple_full_scenario() {
            // threadsCost = 5 × 2 × 200 = 2 000
            // metalCost   = 50000 × 50 / 1000 = 2 500
            // baseCost    = 4 500
            // worksCost   = 0 (нет работ)
            // total       = 4 500
            BigDecimal result = PipeOrderCostCalculator.calculate(
                    new BigDecimal("200"),
                    new BigDecimal("50000"),
                    5, 50.0,
                    List.of(), null);

            assertThat(result).isEqualTo(money("4500.00"));
        }
    }
}