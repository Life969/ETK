package ProjectForJob.example.Job.MVCcontrollers.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.services.Handbook.ExternalThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/external-threads")
@RequiredArgsConstructor
@Slf4j
public class ExternalThreadMvcController {

    private final ExternalThreadService service;

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_LIST = "fragments/external-threads/list :: content";
    private static final String FRAGMENT_FORM = "fragments/external-threads/form :: content";

    // ============================================================
    // Список — аккордеон по типу резьбы (НКТ, ОТТМ, ...)
    // ============================================================
    @GetMapping
    public String list(Model model) {
        List<ExternalThreadEntity> all = service.findAll();

        // LinkedHashMap сохраняет порядок, в котором пришли типы (уже ASC)
        Map<String, List<ExternalThreadEntity>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getType() != null ? e.getType() : "—",
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("groupedThreads", grouped);
        model.addAttribute("totalThreads", all.size());
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return LAYOUT;
    }

    // ============================================================
    // Создание
    // ============================================================
    @GetMapping("/new")
    public String showCreate(Model model) {
        model.addAttribute("externalThread", new ExternalThreadEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ============================================================
    // Редактирование
    // ============================================================
    @GetMapping("/edit/{id}")
    public String showEdit(@PathVariable Long id, Model model) {
        model.addAttribute("externalThread", service.findById(id));
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ============================================================
    // Сохранение
    // ============================================================
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("externalThread") ExternalThreadEntity entity,
                       BindingResult result,
                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return LAYOUT;
        }
        service.save(entity);
        return "redirect:/external-threads";
    }

    // ============================================================
    // Удаление
    // ============================================================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        log.info("DELETE external thread id={}", id);
        service.deleteById(id);
        return "redirect:/external-threads";
    }

    // ============================================================
    // REST API — каскад «тип → диаметр» (для формы заказа в Фазе 2)
    // ============================================================

    @GetMapping("/api/types")
    @ResponseBody
    public List<String> getTypes() {
        return service.findAllDistinctTypes();
    }

    @GetMapping("/api/by-type")
    @ResponseBody
    public List<Map<String, Object>> getByType(@RequestParam String type) {
        return service.findByType(type).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("conditionalDiameter", t.getConditionalDiameter());
                    m.put("name", t.getName());
                    m.put("manufacturingCost", t.getManufacturingCost());
                    m.put("priceForEmployee", t.getPriceForEmployee());
                    return m;
                })
                .toList();
    }
}