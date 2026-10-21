package ProjectForJob.example.Job.controller.mvc.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.SteelGradeEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelStrengthGroupEntity;
import ProjectForJob.example.Job.service.handbook.SteelStrengthGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/steel-groups")
@RequiredArgsConstructor
@Slf4j
public class SteelStrengthGroupMvcController {

    private final SteelStrengthGroupService service;

    private static final String LAYOUT          = "welcomeToHome/layout";
    private static final String FRAGMENT_LIST   = "fragments/steel-groups/list :: content";
    private static final String FRAGMENT_FORM   = "fragments/steel-groups/form :: content";
    private static final String FRAGMENT_GRADE  = "fragments/steel-groups/grade-form :: content";

    // ================== СПИСОК ГРУПП ==================

    @GetMapping
    public String list(Model model) {
        log.info("GET /steel-groups");
        model.addAttribute("groups", service.findAll());
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return LAYOUT;
    }

    // ================== СОЗДАНИЕ ГРУППЫ ==================

    @GetMapping("/new")
    public String showCreateGroupForm(Model model) {
        SteelStrengthGroupEntity group = SteelStrengthGroupEntity.builder()
                .markupPercent(BigDecimal.ZERO)
                .active(true)
                .isDefault(false)
                .build();
        model.addAttribute("group", group);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    @PostMapping("/save")
    public String saveGroup(@Valid @ModelAttribute("group") SteelStrengthGroupEntity group,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return LAYOUT;
        }
        try {
            service.save(group);
            redirectAttributes.addFlashAttribute("message", "Группа прочности сохранена");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            log.error("Ошибка сохранения группы прочности", e);
            redirectAttributes.addFlashAttribute("message",
                    "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/steel-groups";
    }

    // ================== РЕДАКТИРОВАНИЕ ГРУППЫ ==================

    @GetMapping("/edit/{id}")
    public String showEditGroupForm(@PathVariable Long id, Model model) {
        model.addAttribute("group", service.findById(id));
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ================== УДАЛЕНИЕ ГРУППЫ ==================

    @PostMapping("/delete/{id}")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Группа удалена");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        } catch (Exception e) {
            log.warn("Не удалось удалить группу {}", id, e);
            redirectAttributes.addFlashAttribute("message",
                    "Не удалось удалить: возможно, есть связанные марки. " +
                            "Сначала удалите марки, потом группу.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/steel-groups";
    }

    // ================== СОЗДАНИЕ МАРКИ ==================

    @GetMapping("/{groupId}/grades/new")
    public String showCreateGradeForm(@PathVariable Long groupId, Model model) {
        SteelStrengthGroupEntity group = service.findById(groupId);
        SteelGradeEntity grade = SteelGradeEntity.builder()
                .strengthGroup(group)
                .active(true)
                .build();
        model.addAttribute("group", group);
        model.addAttribute("grade", grade);
        model.addAttribute("contentTemplate", FRAGMENT_GRADE);
        return LAYOUT;
    }

    @PostMapping("/{groupId}/grades/save")
    public String saveGrade(@PathVariable Long groupId,
                            @Valid @ModelAttribute("grade") SteelGradeEntity grade,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        SteelStrengthGroupEntity group = service.findById(groupId);
        grade.setStrengthGroup(group);

        if (bindingResult.hasErrors()) {
            model.addAttribute("group", group);
            model.addAttribute("contentTemplate", FRAGMENT_GRADE);
            return LAYOUT;
        }
        try {
            service.saveGrade(grade);
            redirectAttributes.addFlashAttribute("message", "Марка стали сохранена");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            log.error("Ошибка сохранения марки", e);
            redirectAttributes.addFlashAttribute("message",
                    "Ошибка (возможно, такая марка уже есть в группе): " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/steel-groups";
    }

    // ================== РЕДАКТИРОВАНИЕ МАРКИ ==================

    @GetMapping("/grades/edit/{id}")
    public String showEditGradeForm(@PathVariable Long id, Model model) {
        SteelGradeEntity grade = service.findGradeById(id);
        model.addAttribute("group", grade.getStrengthGroup());
        model.addAttribute("grade", grade);
        model.addAttribute("contentTemplate", FRAGMENT_GRADE);
        return LAYOUT;
    }

    // ================== УДАЛЕНИЕ МАРКИ ==================

    @PostMapping("/grades/delete/{id}")
    public String deleteGrade(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteGradeById(id);
            redirectAttributes.addFlashAttribute("message", "Марка стали удалена");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        } catch (Exception e) {
            log.error("Ошибка удаления марки {}", id, e);
            redirectAttributes.addFlashAttribute("message",
                    "Не удалось удалить марку");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/steel-groups";
    }
}