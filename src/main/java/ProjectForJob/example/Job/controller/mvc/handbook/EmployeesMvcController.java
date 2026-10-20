package ProjectForJob.example.Job.controller.mvc.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.EmployeeEntity;
import ProjectForJob.example.Job.service.handbook.EmployeeService;
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
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeesMvcController {

    private final EmployeeService employeesService;

    private static final String FRAGMENT_LIST = "fragments/employees/list :: content";
    private static final String FRAGMENT_FORM = "fragments/employees/form :: content";

    @GetMapping
    public String listAllEmployees(Model model) {
        log.info("GET /employees");
        List<EmployeeEntity> employees = employeesService.findAll();
        model.addAttribute("employees", employees);
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return "welcomeToHome/layout";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("employee", new EmployeeEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/save")
    public String saveEmployee(@Valid @ModelAttribute("employee") EmployeeEntity employee,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return "welcomeToHome/layout";
        }
        employeesService.save(employee);
        redirectAttributes.addFlashAttribute("message", "Сотрудник успешно сохранён");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/employees";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        EmployeeEntity employee = employeesService.findById(id);
        model.addAttribute("employee", employee);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeesService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Сотрудник удалён");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/employees";
    }
}
