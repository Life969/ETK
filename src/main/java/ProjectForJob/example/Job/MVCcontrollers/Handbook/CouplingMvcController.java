package ProjectForJob.example.Job.MVCcontrollers.Handbook;


import ProjectForJob.example.Job.entityJob.CouplingEntity;
import ProjectForJob.example.Job.services.CouplingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/couplings")
@RequiredArgsConstructor
@Slf4j
public class CouplingMvcController {

    private final CouplingService couplingService;



    @GetMapping()
    public String listAllGrouped(Model model) {
        log.info("GET /couplings - группировка по типам");
        List<CouplingEntity> allCouplings = couplingService.findAll();

        // Группируем по типу
        Map<String, List<CouplingEntity>> groupedByType = allCouplings.stream()
                .collect(Collectors.groupingBy(
                        CouplingEntity::getType,
                        TreeMap::new,  // чтобы ключи (типы) были отсортированы
                        Collectors.toList()
                ));

        // Сортируем каждую группу по условному диаметру (как число)
        groupedByType.forEach((type, list) ->
                list.sort(Comparator.comparingInt(c -> Integer.parseInt(c.getConditionalDiameter())))
        );

        model.addAttribute("groupedCouplings", groupedByType);
        return "couplings/list"; // та же страница, но теперь с groupedCouplings
    }

    @GetMapping("/{id}")
    public String showCoupling(@PathVariable Long id, Model model) {
        log.info("GET /couplings/{} - карточка муфты", id);
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        return "couplings/detail";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("GET /couplings/new - форма создания");
        model.addAttribute("coupling", new CouplingEntity());
        return "couplings/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("coupling") CouplingEntity coupling,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        log.info("POST /couplings/save - сохранение муфты: {}", coupling);
        if (bindingResult.hasErrors()) {
            return "couplings/form";
        }
        couplingService.save(coupling);
        redirectAttributes.addFlashAttribute("message", "Муфта успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/couplings";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("GET /couplings/edit/{}", id);
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        return "couplings/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /couplings/delete/{}", id);
        couplingService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Муфта удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/couplings";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }
}