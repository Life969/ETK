package ProjectForJob.example.Job.controller.mvc.handler;

import ProjectForJob.example.Job.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Единый обработчик бизнес-ошибок для MVC-контроллеров.
 *
 * Ловит любое исключение, унаследованное от {@link BusinessException}
 * (CalculatorException, будущие OrderException и т.п.), и делает
 * redirect с flash-сообщением. Пользователь остаётся в UI и видит
 * красную плашку с текстом ошибки — вместо белого экрана со стектрейсом.
 *
 * Scope: только пакет controller.mvc. REST-контроллеры (когда появятся)
 * получат свой @RestControllerAdvice с JSON-ответом.
 */
@Slf4j
@ControllerAdvice(basePackages = "ProjectForJob.example.Job.controller.mvc")
public class MvcErrorHandler {

    private static final String DEFAULT_REDIRECT = "/orders/waiting";
    private static final String FLASH_ATTR = "errorMessage";

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        log.info("MVC business error: code={}, msg={}",
                ex.getErrorCode().getCode(), ex.getMessage());

        ra.addFlashAttribute(FLASH_ATTR, ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    /**
     * Возвращает Referer, если он валиден и не ведёт на /error,
     * иначе — дефолтный безопасный адрес. Защита от redirect-loop
     * и от редиректа на несуществующие страницы.
     */
    private String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank() || referer.contains("/error")) {
            return DEFAULT_REDIRECT;
        }
        return referer;
    }
}