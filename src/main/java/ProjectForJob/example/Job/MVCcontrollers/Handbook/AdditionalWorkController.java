package ProjectForJob.example.Job.MVCcontrollers.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.services.Handbook.AdditionalWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/additionalWork")
@RequiredArgsConstructor
@Slf4j
public class AdditionalWorkController {

    private final AdditionalWorkService additionalWorkService;

    private static final String FRAGMENT_LIST = "fragments/works/list :: content";
    private static final String FRAGMENT_FORM = "fragments/works/form :: content";

    @GetMapping
    public String listAllWork(Model model) {
        log.info("GET /additionalWork");
        List<AdditionalWorkEntity> works = additionalWorkService.findAll();
        model.addAttribute("works", works);
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return "welcomeToHome/layout";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("work", new AdditionalWorkEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/save")
    public String saveWork(@Valid @ModelAttribute("work") AdditionalWorkEntity work,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return "welcomeToHome/layout";
        }
        additionalWorkService.save(work);
        redirectAttributes.addFlashAttribute("message", "Дополнительная работа успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/additionalWork";
    }

    @GetMapping("/edit/{id}")
    public String showEditFormWork(@PathVariable Long id, Model model) {
        AdditionalWorkEntity work = additionalWorkService.findById(id);
        model.addAttribute("work", work);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/delete/{id}")
    public String deleteWork(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        additionalWorkService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Работа удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/additionalWork";
    }
}