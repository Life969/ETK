package ProjectForJob.example.Job.exception.util;

import ProjectForJob.example.Job.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Коды ошибок калькуляторов (вес, стоимость).
 * <p>
 * Отдельный enum, а не общий JobErrorCode, потому что калькулятором
 * может пользоваться любой сервис — он не должен зависеть от домена заказов.
 */
public enum CalculatorErrorCode implements ErrorCode {

    OUTER_DIAMETER_NOT_POSITIVE("CALC-001",
            HttpStatus.BAD_REQUEST,
            "Наружный диаметр должен быть > 0"),

    WALL_THICKNESS_NOT_POSITIVE("CALC-002",
            HttpStatus.BAD_REQUEST,
            "Толщина стенки должна быть > 0"),

    LENGTH_NOT_POSITIVE("CALC-003",
            HttpStatus.BAD_REQUEST,
            "Длина должна быть > 0"),

    WALL_EXCEEDS_DIAMETER("CALC-004",
            HttpStatus.BAD_REQUEST,
            "Толщина стенки не может быть больше наружного диаметра"),

    QUANTITY_NOT_POSITIVE("CALC-005",
            HttpStatus.BAD_REQUEST,
            "Количество должно быть > 0");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CalculatorErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }



    @Override
    public String getCode() {
        return code;
    }



    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
