package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.services.EmployeesService;
import jakarta.validation.Valid;
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
@RequestMapping("/employees")
public class EmployeesMvcController {
    private static final Logger log = LoggerFactory.getLogger(EmployeesMvcController.class);
    private final EmployeesService employeesService;

    public EmployeesMvcController(EmployeesService employeesService) {
        this.employeesService = employeesService;
    }

    // Метод для отображения списка всех сотрудников
    @GetMapping
    public String listAllEmployees(Model model) {
        log.info("Запрос на отображение всех сотрудников в веб-интерфейсе");

        // 1. Получаем данные через существующий сервис
        List<EmployeesEntity> employees = employeesService.findAll();

        // 2. Кладем данные в модель Spring (атрибут "employees")
        model.addAttribute("employees", employees);

        // 3. Возвращаем имя HTML-шаблона (без расширения .html)
        return "employees/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("Запрос формы создания нового сотрудника");
        // Создаём пустой объект, чтобы форма могла к нему привязаться
        model.addAttribute("employee", new EmployeesEntity());
        return "employees/form";
    }

    @PostMapping("/save")
    public String saveEmployee(@Valid @ModelAttribute("employee")
                                    EmployeesEntity employee,
                               BindingResult bindingResult, Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "employees/form";
        } // Реализовать в REST! тоже.
        log.info("method save called: {}", employee);
        employeesService.save(employee);
        redirectAttributes.addFlashAttribute("message", "Сотрудник успешно сохранен");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/employees";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("Запрос формы редактирования сотрудника с id = {}", id);
        EmployeesEntity employee = employeesService.findById(id);
        if (employee == null) {
            throw new NoSuchElementException("Сотрудник с id" + id + " не найдет");
        }
        model.addAttribute("employee", employee);
        return "employees/form";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id,RedirectAttributes redirectAttributes) {
        log.info("deleteEmployee id = {}", id);
        employeesService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Сотрудник удалён.");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/employees";
    }



}
