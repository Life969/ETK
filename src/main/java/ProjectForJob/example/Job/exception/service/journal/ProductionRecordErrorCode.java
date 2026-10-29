package ProjectForJob.example.Job.exception.service.journal;

import ProjectForJob.example.Job.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Коды ошибок журнала производства.
 *
 * Префикс PRC (Production ReCord) — чтобы не путать с ORD (orders)
 * и CALC (calculator) при чтении логов.
 *
 * Журнал — самостоятельный домен (production journal), не часть Order,
 * поэтому отдельный enum. MvcErrorHandler при этом не меняется —
 * он ловит любой BusinessException.
 */
public enum ProductionRecordErrorCode implements ErrorCode {

    // ===== Lifecycle =====
    RECORD_NOT_FOUND("PRC-001", HttpStatus.NOT_FOUND,
            "Запись производства не найдена"),

    // ===== Обязательные связи =====
    EMPLOYEE_REQUIRED("PRC-010", HttpStatus.BAD_REQUEST,
            "Сотрудник не выбран"),
    MACHINE_REQUIRED("PRC-011", HttpStatus.BAD_REQUEST,
            "Станок не выбран"),

    // ===== Продукт =====
    PRODUCT_TYPE_MISSING("PRC-020", HttpStatus.BAD_REQUEST,
            "Тип продукции не задан"),
    PRODUCT_TYPE_INVALID("PRC-021", HttpStatus.BAD_REQUEST,
            "Неверный тип продукции"),
    COUPLING_NOT_SELECTED("PRC-022", HttpStatus.BAD_REQUEST,
            "Муфта не выбрана"),
    ADAPTER_NOT_SELECTED("PRC-023", HttpStatus.BAD_REQUEST,
            "Переводник не выбран"),
    THREAD_NOT_SELECTED("PRC-024", HttpStatus.BAD_REQUEST,
            "Резьба не выбрана"),

    // ===== Патрубок =====
    PIECE_LENGTH_REQUIRED("PRC-030", HttpStatus.BAD_REQUEST,
            "Длина патрубка должна быть положительной");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ProductionRecordErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override public String getCode()       { return code; }
    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage()    { return message; }
}