package ProjectForJob.example.Job.exception.util;

public enum CalculatorErrorCode {
    OUTER_DIAMETER_NOT_POSITIVE, // наружний диаметр отрицательный
    WALL_THICKNESS_NOT_POSITIVE, // толщина стенки отрицательная
    LENGTH_NOT_POSITIVE, // длинна отрицательная
    WALL_EXCEEDS_DIAMETER, // Толщина стенки больше диаметра трубы
    QUANTITY_NOT_POSITIVE // Количество должно быть положительным

}
