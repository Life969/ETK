package ProjectForJob.example.Job.exception.service.order;

import ProjectForJob.example.Job.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Коды ошибок бизнес-логики заказов.
 *
 * Отдельный enum от CalculatorErrorCode: калькулятор — самостоятельный
 * компонент, его ошибки помечены префиксом CALC. Ошибки заказов — ORD.
 *
 * Один enum на сервис: при появлении CompanyService заведём CompanyErrorCode
 * в exception/service/company/. ErrorHandler при этом не меняется.
 */
public enum OrderErrorCode implements ErrorCode {

    // ===== Продукт =====
    PRODUCT_TYPE_MISSING("ORD-001", HttpStatus.BAD_REQUEST, "Тип продукции не задан"),
    PRODUCT_TYPE_INVALID("ORD-002", HttpStatus.BAD_REQUEST, "Неверный тип продукции"),

    // ===== Муфта / переводник =====
    COUPLING_NOT_SELECTED("ORD-010", HttpStatus.BAD_REQUEST, "Муфта не выбрана"),
    COUPLING_NOT_FOUND("ORD-011", HttpStatus.NOT_FOUND, "Муфта не найдена"),
    ADAPTER_NOT_SELECTED("ORD-012", HttpStatus.BAD_REQUEST, "Переводник не выбран"),
    ADAPTER_NOT_FOUND("ORD-013", HttpStatus.NOT_FOUND, "Переводник не найден"),

    // ===== Труба / патрубок =====
    THREAD_NOT_SELECTED("ORD-020", HttpStatus.BAD_REQUEST, "Резьба не выбрана"),
    THREAD_NOT_FOUND("ORD-021", HttpStatus.NOT_FOUND, "Резьба не найдена"),
    THREAD_DIAMETER_PARSE_ERROR("ORD-022", HttpStatus.INTERNAL_SERVER_ERROR,
            "Не удалось разобрать условный диаметр резьбы"),
    WALL_THICKNESS_REQUIRED("ORD-023", HttpStatus.BAD_REQUEST, "Толщина стенки не задана"),
    PRICE_PER_TON_REQUIRED("ORD-024", HttpStatus.BAD_REQUEST, "Укажите цену за тонну металла"),
    PIPE_LENGTH_OR_WEIGHT_REQUIRED("ORD-025", HttpStatus.BAD_REQUEST,
            "Для трубы укажите общий метраж или тоннаж"),
    NIPPLE_LENGTH_REQUIRED("ORD-026", HttpStatus.BAD_REQUEST,
            "Длина патрубка должна быть положительной"),
    INVALID_PIPE_PARAMS("ORD-027", HttpStatus.BAD_REQUEST, "Некорректные параметры трубы"),

    // ===== Группа прочности / марка =====
    GROUP_NOT_FOUND("ORD-030", HttpStatus.NOT_FOUND, "Группа прочности не найдена"),
    GRADE_NOT_FOUND("ORD-031", HttpStatus.NOT_FOUND, "Марка стали не найдена"),
    GRADE_NOT_IN_GROUP("ORD-032", HttpStatus.BAD_REQUEST,
            "Марка не принадлежит указанной группе прочности"),

    // ===== Количество =====
    QUANTITY_NOT_POSITIVE("ORD-035", HttpStatus.BAD_REQUEST, "Количество должно быть > 0"),

    // ===== Lifecycle =====
    ORDER_NOT_FOUND("ORD-040", HttpStatus.NOT_FOUND, "Заказ не найден"),
    ORDER_NOT_WAITING("ORD-041", HttpStatus.CONFLICT, "Заказ уже не в статусе ожидания");

    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override public String getCode()       { return code; }
    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage()    { return message; }
}