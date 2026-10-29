package ProjectForJob.example.Job.exception.util;

import ProjectForJob.example.Job.exception.BusinessException;

/**
 * Ошибка калькулятора (вес, стоимость).
 *
 * Семантически независима от бизнес-логики заказов: калькулятором может
 * пользоваться любой сервис. Поэтому свой {@link CalculatorErrorCode},
 * а не OrderErrorCode.
 *
 * Технически наследует {@link BusinessException}, чтобы ErrorHandler
 * ловил все бизнес-ошибки одним @ExceptionHandler.
 */
public class CalculatorException extends BusinessException {

    public CalculatorException(CalculatorErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CalculatorException(CalculatorErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public CalculatorErrorCode getCode() {
        return (CalculatorErrorCode) getErrorCode();
    }
}