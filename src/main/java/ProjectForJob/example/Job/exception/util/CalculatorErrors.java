package ProjectForJob.example.Job.exception.util;

// CalculatorErrors.java — фабрика
public final class CalculatorErrors {
    private CalculatorErrors() {
    }

    public static CalculatorException outerDiameter(double value) {
        return new CalculatorException(
                CalculatorErrorCode.OUTER_DIAMETER_NOT_POSITIVE,
                "Наружный диаметр должен быть > 0, получено: " + value);
    }

    public static CalculatorException wallThickness(double value) {
        return new CalculatorException(
                CalculatorErrorCode.WALL_THICKNESS_NOT_POSITIVE,
                "Толщина стенки должна быть > 0, получено: " + value);
    }

    public static CalculatorException length(double value) {
        return new CalculatorException(
                CalculatorErrorCode.LENGTH_NOT_POSITIVE,
                "Длина должна быть > 0, получено: " + value);
    }

    public static CalculatorException wallExceedsDiameter(double d, double s) {
        return new CalculatorException(
                CalculatorErrorCode.WALL_EXCEEDS_DIAMETER,
                "Толщина стенки (%.2f мм) не может быть больше наружного диаметра (%.2f мм)"
                        .formatted(s, d));
    }

    public static CalculatorException quantityNotPositive(int value) {
        return new CalculatorException(
                CalculatorErrorCode.QUANTITY_NOT_POSITIVE,
                "Количество должно быть > 0, получено: " + value);
    }
}
