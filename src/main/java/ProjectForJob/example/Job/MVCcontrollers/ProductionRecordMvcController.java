package ProjectForJob.example.Job.MVCcontrollers;


import ProjectForJob.example.Job.entityJob.CouplingEntity;
import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.MachinesEntity;
import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.CouplingService;
import ProjectForJob.example.Job.services.EmployeesService;
import ProjectForJob.example.Job.services.MachinesService;
import ProjectForJob.example.Job.services.ProductionRecordService;
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
@RequestMapping("/production-records")
@RequiredArgsConstructor
public class ProductionRecordMvcController {

    private static final Logger log = LoggerFactory.getLogger(ProductionRecordMvcController.class);
    private final ProductionRecordService recordService;
    private final CouplingService couplingService;
    private final EmployeesService employeeService;
    private final MachinesService machineService;


    @GetMapping
    public String listAll(Model model) {
        log.info("GET /production-records");
        List<ProductionRecordEntity> records = recordService.findAll();
        model.addAttribute("records", records);
        return "production-records/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("GET /production-records/new");
        prepareFormModel(model);
        model.addAttribute("record", new ProductionRecordEntity());
        return "production-records/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("GET /production-records/edit/{}", id);
        ProductionRecordEntity record = recordService.findById(id);
        prepareFormModel(model);
        model.addAttribute("record", record);
        return "production-records/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("record") ProductionRecordEntity record,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        log.info("POST /production-records/save");
        if (bindingResult.hasErrors()) {
            prepareFormModel(model);
            return "production-records/form";
        }
        recordService.save(record);
        redirectAttributes.addFlashAttribute("message", "Запись успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/production-records";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /production-records/delete/{}", id);
        recordService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Запись удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/production-records";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    private void prepareFormModel(Model model) {
        List<CouplingEntity> couplings = couplingService.findAll();
        List<EmployeesEntity> employees = employeeService.findAll();
        List<MachinesEntity> machines = machineService.findAll();
        model.addAttribute("couplings", couplings);
        model.addAttribute("employees", employees);
        model.addAttribute("machines", machines);
    }
}
