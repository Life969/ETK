package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.MachinesEntity;
import ProjectForJob.example.Job.services.EmployeesService;
import ProjectForJob.example.Job.services.MachinesService;
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
@RequestMapping("/machines")
public class MachinesMvcController {
    private static final Logger log = LoggerFactory.getLogger(MachinesMvcController.class);
    private final MachinesService machinesService;

    public MachinesMvcController(MachinesService machinesService) {
        this.machinesService = machinesService;
    }

    // Метод для отображения списка всех станков
    @GetMapping
    public String listAllMachines(Model model) {
        log.info("getAllMachines getMapping");

        // 1. Получаем данные через существующий сервис
        List<MachinesEntity> machines = machinesService.findAll();

        // 2. Кладем данные в модель Spring (атрибут "machines")
        model.addAttribute("machines", machines);

        // 3. Возвращаем имя HTML-шаблона (без расширения .html)
        return "machines/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("called showCreateFormMachines");
        // Создаём пустой объект, чтобы форма могла к нему привязаться
        model.addAttribute("machine", new MachinesEntity());
        return "machines/form";
    }

    @PostMapping("/save")
    public String saveMachine(@Valid @ModelAttribute("machine")
                               MachinesEntity machine,
                               BindingResult bindingResult, Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "machines/form";
        } // Реализовать в REST! тоже.
        log.info("method saveMachine called: {}", machine);
        machinesService.save(machine);
        redirectAttributes.addFlashAttribute("message", "Станок успешно сохранен");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/machines";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("called showEditFormMachine id = {}", id);
        MachinesEntity machine = machinesService.findById(id);
        if (machine == null) {
            throw new NoSuchElementException("Станок с id" + id + " не найден");
        }
        model.addAttribute("machine", machine);
        return "machines/form";
    }

    @PostMapping("/delete/{id}")
    public String deleteMachine(@PathVariable Long id,RedirectAttributes redirectAttributes) {
        log.info("deleteMachine id = {}", id);
        machinesService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Станок удалён.");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/machines";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }
}
