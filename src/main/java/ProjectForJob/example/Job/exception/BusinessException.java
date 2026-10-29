package ProjectForJob.example.Job.exception;

/**
 * Базовое бизнес-исключение приложения.
 *
 * Хранит {@link ErrorCode} (код + HTTP-статус + дефолтное сообщение)
 * и опциональное кастомное сообщение, если нужно уточнить детали
 * (например, "Муфта не найдена: id=42").
 *
 * Все доменные исключения (CalculatorException, будущие OrderException
 * и т.п.) наследуются от этого класса — так ErrorHandler не растёт
 * с числом сервисов.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** Дефолтное сообщение из enum. */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** Кастомное сообщение (с конкретными значениями). */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /** С причиной — для обёртки чужих исключений (например, NumberFormatException). */
    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}