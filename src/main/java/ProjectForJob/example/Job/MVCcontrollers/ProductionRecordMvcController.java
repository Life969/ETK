package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.CouplingService;
import ProjectForJob.example.Job.services.EmployeesService;
import ProjectForJob.example.Job.services.MachinesService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

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
    public String listAll(@RequestParam(required = false) Long couplingId,
                          @RequestParam(required = false) Long employeeId,
                          @RequestParam(required = false) Long machineId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        log.info("GET /production-records with filters, page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductionRecordEntity> pageResult = recordService.findAllByFilters(
                couplingId, employeeId, machineId, startDate, endDate, pageable);

        model.addAttribute("recordsPage", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());

        // для выпадающих списков фильтров
        addReferenceData(model);

        // сохраняем выбранные фильтры для формы
        model.addAttribute("selectedCouplingId", couplingId);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedMachineId", machineId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

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
        addReferenceData(model);
    }

    private void addReferenceData(Model model) {
        model.addAttribute("couplings", couplingService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("machines", machineService.findAll());
    }
}
