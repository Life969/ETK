package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.exception.util.CalculatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;


/**
 * Тесты для {@link OrderCostCalculator}.
 * Калькулятор используется для муфт и переводников (COUPLING/ADAPTER):
 * база = unitCost × quantity, плюс надбавка за группу прочности,
 * плюс доп. работы (весозависимые, минимум 100 кг).
 * Фиксируем текущее поведение (character tests):
 * - guards от null;
 * - базовый расчёт;
 * - надбавка (snapshot);
 * - доп. работы с минимумом 100 кг;
 * - округление до копеек (HALF_UP).
 */
@DisplayName("OrderCostCalculator: расчет стоимости заказа")
public class OrderCostCalculatorTest {


    // Хелперы
    private static AdditionalWorkEntity work(String name, String price){
        return AdditionalWorkEntity.builder().name(name).price(new BigDecimal(price)).build();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Валидация
    @Nested
    @DisplayName("Валидация входа")
    class Validation {

        @Test
        @DisplayName("quantity = 0 → исключение")
        void zero_quantity_throws() {
            assertThatThrownBy(() -> OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 0, null, 50.0))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Количество");
        }

        @Test
        @DisplayName("quantity < 0 → исключение")
        void negative_quantity_throws() {
            assertThatThrownBy(() -> OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), -5, null, 50.0))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Количество");
        }
    }

    //Базовый расчет

    @Nested
    @DisplayName("Базовый расчет: baseCost = initCost * quantity = цена изготовления 1шт * количество")
    class BaseCost {

        @Test
        @DisplayName("100 руб * 10 шт -> 1000руб")
        void simple_multiplication(){
            BigDecimal result = OrderCostCalculator.calculate(new BigDecimal("100"),
                    List.of(),
                    10,
                    null,
                    null);
            assertThat(result).isEqualTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("unitCost = null -> трактуется как 0")
        void null_unit_cost_is_zero() {
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(), 10, null, null);

            assertThat(result).isEqualTo(money("0.00"));
        }

        @Test
        @DisplayName("пустой список работ и null-надбавка -> только baseCost")
        void no_works_no_markup() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("250.50"), List.of(), 4, null, null);

            assertThat(result).isEqualTo(money("1002.00"));
        }

    }

    // Надбавка за группу прочности
    @Nested
    @DisplayName("Надбавка за группу прочности")
    class Markup {

        @Test
        @DisplayName("1000 руб + 10% = 1100 руб")
        void ten_percent_markup() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 10, new BigDecimal("10"), null);

            assertThat(result).isEqualTo(money("1100.00"));
        }

        @Test
        @DisplayName("markupPercent = null -> 0%, надбавки нет")
        void null_markup_is_zero() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 10, null, null);

            assertThat(result).isEqualByComparingTo(money("1000.00"));
        }

        @Test
        @DisplayName("Дробный процент: 1000 руб × 33.33% = 1333.30 руб")
        void fractional_markup() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 10, new BigDecimal("33.33"), null);

            // baseCost = 1000, markupCost = 333.30, productTotal = 1333.30
            assertThat(result).isEqualTo(money("1333.30"));
        }

        @Test
        @DisplayName("Надбавка применяется только к baseCost, не к работам")
        void markup_not_applied_to_works() {
            // baseCost = 1000, markup 10% -> 1100
            // works: 1000 руб/т × 100 кг = 100 руб (минимум веса)
            // total = 1100 + 100 = 1200
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"),
                    List.of(work("Покраска", "1000")),
                    10,
                    new BigDecimal("10"),
                    null);

            assertThat(result).isEqualTo(money("1200.00"));
        }
    }


    // Доп работы (минимум 100 кг)


    @Nested
    @DisplayName("Доп. работы: W_billing = max(W, 100)")
    class AdditionalWorks {

        @Test
        @DisplayName("Вес null → минимум 100 кг")
        void null_weight_uses_minimum() {
            // works: 1000 руб/т × 100 кг / 1000 = 100 руб
            // baseCost = 0 (unitCost null), итого 100
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Покраска", "1000")), 1, null, null);

            assertThat(result).isEqualTo(money("100.00"));
        }

        @Test
        @DisplayName("Вес < 100 кг → поднимается до 100 кг")
        void weight_below_minimum_is_lifted() {
            // works: 2000 руб/т × 100 кг / 1000 = 200 руб
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Работа", "2000")), 1, null, 50.0);

            assertThat(result).isEqualTo(money("200.00"));
        }

        @Test
        @DisplayName("Вес ровно 100 кг → остаётся 100 кг")
        void weight_equal_to_minimum() {
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Работа", "1000")), 1, null, 100.0);

            assertThat(result).isEqualTo(money("100.00"));
        }

        @Test
        @DisplayName("Вес 500 кг → считается от 500 кг, не от 100")
        void weight_above_minimum_used_as_is() {
            // works: 1000 руб/т × 500 кг / 1000 = 500 руб
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Работа", "1000")), 1, null, 500.0);

            assertThat(result).isEqualTo(money("500.00"));
        }

        @Test
        @DisplayName("Вес отрицательный → трактуется как минимум 100 кг")
        void negative_weight_treated_as_minimum() {
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Работа", "1000")), 1, null, -50.0);

            assertThat(result).isEqualTo(money("100.00"));
        }

        @Test
        @DisplayName("Несколько работ — цены суммируются")
        void multiple_works_summed() {
            // (1000 + 500) руб/т × 100 кг / 1000 = 150 руб
            BigDecimal result = OrderCostCalculator.calculate(
                    null,
                    List.of(work("A", "1000"), work("B", "500")),
                    1, null, null);

            assertThat(result).isEqualTo(money("150.00"));
        }

        @Test
        @DisplayName("null-элемент и null-цена в списке работ игнорируются")
        void null_elements_skipped() {
            List<AdditionalWorkEntity> works = java.util.Arrays.asList(
                    work("A", "1000"),
                    null,
                    AdditionalWorkEntity.builder().name("B").price(null).build(),
                    work("C", "500"));

            BigDecimal result = OrderCostCalculator.calculate(
                    null, works, 1, null, null);

            // (1000 + 500) × 100 / 1000 = 150
            assertThat(result).isEqualTo(money("150.00"));
        }

        @Test
        @DisplayName("Пустой список работ → worksCost = 0")
        void empty_works_list() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 10, null, null);

            assertThat(result).isEqualByComparingTo(money("1000.00"));
        }
    }


    // Округление

    @Nested
    @DisplayName("Округление до копеек (HALF_UP)")
    class Rounding {

        @Test
        @DisplayName("Результат всегда scale=2")
        void result_has_scale_two() {
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("100"), List.of(), 3, null, null);

            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Дробный вес: 333.33 кг × 100 руб/т → 33.33 руб")
        void fractional_weight_rounds_half_up() {
            // works: 100 руб/т × 333.33 кг / 1000 = 33.333 руб → 33.33
            BigDecimal result = OrderCostCalculator.calculate(
                    null, List.of(work("Работа", "100")), 1, null, 333.33);

            assertThat(result).isEqualTo(money("33.33"));
        }

    }


    // Интеграционные тесты (всё вместе)


    @Nested
    @DisplayName("Комбинированные сценарии")
    class Combined {

        @Test
        @DisplayName("Муфта: 500 руб × 20 шт + 10% + работы 1000 руб/т при 400 кг")
        void coupling_full_scenario() {
            // baseCost   = 500 × 20 = 10 000
            // markupCost = 10 000 × 10% = 1 000
            // productTotal = 11 000
            // worksCost  = 1000 × 400 / 1000 = 400
            // total      = 11 400
            BigDecimal result = OrderCostCalculator.calculate(
                    new BigDecimal("500"),
                    List.of(work("Покраска", "1000")),
                    20,
                    new BigDecimal("10"),
                    400.0);

            assertThat(result).isEqualTo(money("11400.00"));
        }

        @Test
        @DisplayName("Нулевой unitCost, но работы есть → платим только за работы")
        void zero_unit_cost_with_works() {
            // baseCost = 0, markup = 0, works = 1000 × 100 / 1000 = 100
            BigDecimal result = OrderCostCalculator.calculate(
                    BigDecimal.ZERO, List.of(work("Работа", "1000")), 1, null, null);

            assertThat(result).isEqualTo(money("100.00"));
        }
    }
}
