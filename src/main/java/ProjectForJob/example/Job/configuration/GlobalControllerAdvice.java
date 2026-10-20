package ProjectForJob.example.Job.configuration;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
/* В контроллер, + обработка ошибка*/

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
    }
}