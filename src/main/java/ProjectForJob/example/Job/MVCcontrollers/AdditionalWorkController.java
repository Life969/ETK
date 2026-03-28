package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.AdditionalWorkEntity;
import ProjectForJob.example.Job.services.AdditionalWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;


@Controller
@RequestMapping("/additionalWork")
@RequiredArgsConstructor
public class AdditionalWorkController {
    private static final Logger log = LoggerFactory.getLogger(AdditionalWorkController.class);
    private final AdditionalWorkService additionalWorkService;


    // Метод для отображения списка всех станков
    @GetMapping
    public String listAllWork(Model model) {
        log.info("getAllWork getMapping");

        // 1. Получаем данные через существующий сервис
        List<AdditionalWorkEntity> works = additionalWorkService.findAll();

        // 2. Кладем данные в модель Spring (атрибут "machines")
        model.addAttribute("works", works);

        // 3. Возвращаем имя HTML-шаблона (без расширения .html)
        return "additionalWork/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("called showCreateFormWork");
        // Создаём пустой объект, чтобы форма могла к нему привязаться
        model.addAttribute("work", new AdditionalWorkEntity());
        return "additionalWork/form";
    }

    @PostMapping("/save")
    public String saveWork(@Valid @ModelAttribute("work")
                              AdditionalWorkEntity work,
                              BindingResult bindingResult, Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "additionalWork/form";
        } // Реализовать в REST! тоже.
        log.info("method work called: {}", work);
        additionalWorkService.save(work);
        redirectAttributes.addFlashAttribute("message", "Дополнительная работа успешно сохранен");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/additionalWork";
    }

    @GetMapping("/edit/{id}")
    public String showEditFormWork(@PathVariable Long id, Model model) {
        log.info("called showEditFormWork id = {}", id);
        AdditionalWorkEntity work = additionalWorkService.findById(id);
        if (work == null) {
            throw new NoSuchElementException("Дополнительная работа с " + id + " не найдена");
        }
        model.addAttribute("work", work);
        return "additionalWork/form";
    }

    @PostMapping("/delete/{id}")
    public String deleteWork(@PathVariable Long id,RedirectAttributes redirectAttributes) {
        log.info("deleteWork id = {}", id);
        additionalWorkService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Работа удалёна.");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/additionalWork";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

}
