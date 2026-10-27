package ProjectForJob.example.Job.exception;

import org.springframework.http.HttpStatus;

/**
 * Общий контракт для всех enum'ов ошибок приложения.
 *
 * Каждый домен (util, service/order, service/company, ...) заводит свой
 * enum, реализующий этот интерфейс. Благодаря этому любое доменное
 * исключение наследуется от {@link BusinessException} и ловится одним
 * @ExceptionHandler, независимо от того, сколько сервисов появится.
 */
public interface ErrorCode {

    /** Машиночитаемый код, напр. "ORD-001", "CALC-003". */
    String getCode();

    /** HTTP-статус, который вернётся клиенту (для MVC — используется в логах). */
    HttpStatus getStatus();

    /** Дефолтное человекочитаемое сообщение. */
    String getMessage();
}