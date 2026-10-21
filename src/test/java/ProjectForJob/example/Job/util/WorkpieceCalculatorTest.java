package ProjectForJob.example.Job.util;


import ProjectForJob.example.Job.exception.util.CalculatorErrorCode;
import ProjectForJob.example.Job.exception.util.CalculatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты для {@link WorkpieceCalculator}.
 *
 * Покрывают два аспекта:
 *   1) корректный расчёт теоретического веса трубы;
 *   2) валидацию входа — калькулятор бросает {@link CalculatorException}
 *      с понятным сообщением и машиночитаемым {@link CalculatorErrorCode}.
 */
@DisplayName("WorkpieceCalculator: расчёт веса трубы/заготовки для муфт")
class WorkpieceCalculatorTest {

    private static final double EPSILON = 0.1; // Погрешность для double-сравнений, кг

    // 1. ВАЛИДАЦИЯ: некорректный вход → CalculatorException

    @Nested
    @DisplayName("Валидация входа")
    class Validation {

        @Test
        @DisplayName("Нулевой наружный диаметр → исключение")
        void zero_outer_diameter_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(0.0, 5.5, 10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Наружный диаметр")
                    .extracting(ex -> ((CalculatorException) ex).getCode())
                    .isEqualTo(CalculatorErrorCode.OUTER_DIAMETER_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Отрицательный наружный диаметр → исключение")
        void negative_outer_diameter_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(-73, 5.5, 10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Наружный диаметр");
        }

        @Test
        @DisplayName("Нулевая толщина стенки → исключение")
        void zero_wall_thickness_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(73, 0, 10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Толщина стенки")
                    .extracting(ex -> ((CalculatorException) ex).getCode())
                    .isEqualTo(CalculatorErrorCode.WALL_THICKNESS_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Отрицательная толщина стенки → исключение")
        void negative_wall_thickness_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(73, -5.5, 10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Толщина стенки");
        }

        @Test
        @DisplayName("Нулевая длина → исключение")
        void zero_length_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(73, 5.5, 0))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Длина")
                    .extracting(ex -> ((CalculatorException) ex).getCode())
                    .isEqualTo(CalculatorErrorCode.LENGTH_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Отрицательная длина → исключение")
        void negative_length_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(73, 5.5, -10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("Длина");
        }

        @Test
        @DisplayName("Толщина стенки больше наружного диаметра → исключение")
        void wall_thicker_than_diameter_throws() {
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(50, 60, 10))
                    .isInstanceOf(CalculatorException.class)
                    .hasMessageContaining("не может быть больше")
                    .extracting(ex -> ((CalculatorException) ex).getCode())
                    .isEqualTo(CalculatorErrorCode.WALL_EXCEEDS_DIAMETER);
        }

        @Test
        @DisplayName("Толщина стенки равна наружному диаметру → исключение (труба нулевой стенки)")
        void wall_equals_diameter_throws() {
            // D - S = 0 → вес тоже 0, но физически это не труба. Считаем ошибкой входа.
            assertThatThrownBy(() -> WorkpieceCalculator.calculateWeight(73, 73, 10))
                    .isInstanceOf(CalculatorException.class)
                    .extracting(ex -> ((CalculatorException) ex).getCode())
                    .isEqualTo(CalculatorErrorCode.WALL_EXCEEDS_DIAMETER);
        }

    }


    // 2. Расчет: корректный вход → правильный вес


    @Nested
    @DisplayName("Расчёт веса")
    class Calculation {

        @Test
        @DisplayName("Труба 73 × 5.5 мм, 10 м → ~91.6 кг")
        void standard_pipe_73x5_5_10m() {
            double weight = WorkpieceCalculator.calculateWeight(73, 5.5, 10);
            assertThat(weight).isCloseTo(91.6, org.assertj.core.data.Offset.offset(EPSILON));
        }

        @Test
        @DisplayName("Труба 89 × 6.5 мм, 1 м → ~13.2 кг")
        void standard_pipe_89x6_5_1m() {
            double weight = WorkpieceCalculator.calculateWeight(89, 6.5, 1);
            assertThat(weight).isCloseTo(13.2, org.assertj.core.data.Offset.offset(EPSILON));
        }

        @Test
        @DisplayName("Вес пропорционален длине")
        void weight_scales_with_length() {
            double w1 = WorkpieceCalculator.calculateWeight(73, 5.5, 1);
            double w10 = WorkpieceCalculator.calculateWeight(73, 5.5, 10);

            assertThat(w10).isCloseTo(w1 * 10, org.assertj.core.data.Offset.offset(EPSILON));
        }

        @Test
        @DisplayName("Результат округлён до 2 знаков после запятой")
        void result_is_rounded_to_two_decimals() {
            double weight = WorkpieceCalculator.calculateWeight(73, 5.5, 10);

            // Проверяем, что нет "хвоста" из double-арифметики
            double scaled = weight * 100;
            assertThat(scaled).isCloseTo(Math.round(scaled),
                    org.assertj.core.data.Offset.offset(1e-9));
        }
    }
}
