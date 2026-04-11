package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.CouplingEntity;
import ProjectForJob.example.Job.entityJob.PipeAdapterEntity;
import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/production-records")
@RequiredArgsConstructor
@Slf4j
public class ProductionRecordMvcController {

    private final ProductionRecordService recordService;
    private final CouplingService couplingService;
    private final EmployeesService employeeService;
    private final MachinesService machineService;
    private final PipeAdapterService adapterService;


    @GetMapping
    public String listAll(@RequestParam(required = false) String productType,
                          @RequestParam(required = false) Long couplingId,
                          @RequestParam(required = false) Long adapterId,
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
                productType,
                couplingId,
                adapterId,
                employeeId,
                machineId,
                startDate,
                endDate,
                pageable);

        model.addAttribute("recordsPage", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());


        // для выпадающих списков фильтров
        addReferenceData(model);
        model.addAttribute("adapterFirstSideTypes", adapterService.findAllDistinctFirstSideTypes());

        // сохраняем выбранные фильтры для формы
        model.addAttribute("selectedProductType", productType);
        model.addAttribute("selectedCouplingId", couplingId);
        model.addAttribute("selectedAdapterId", adapterId);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedMachineId", machineId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "production-records/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareFormModel(model);
        model.addAttribute("record", new ProductionRecordEntity());
        model.addAttribute("productTypes", List.of("COUPLING", "ADAPTER"));
        // Для JS: текущие значения пустые
        model.addAttribute("currentProductType", "");
        model.addAttribute("currentCouplingId", null);
        model.addAttribute("currentAdapterId", null);
        return "production-records/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductionRecordEntity record = recordService.findById(id);
        prepareFormModel(model);
        model.addAttribute("record", record);
        model.addAttribute("productTypes", List.of("COUPLING", "ADAPTER"));

        String productType = record.getProductType();
        model.addAttribute("currentProductType", productType);
        if ("COUPLING".equals(productType)) {
            model.addAttribute("currentCouplingId", record.getCoupling().getId());
        } else if ("ADAPTER".equals(productType)) {
            model.addAttribute("currentAdapterId", record.getAdapter().getId());
        }
        return "production-records/form";
    }

   @PostMapping("/save")
public String save(@Valid @ModelAttribute("record") ProductionRecordEntity record,
                   BindingResult bindingResult,
                   @RequestParam(value = "productType", required = false) String productType,
                   Model model,
                   RedirectAttributes redirectAttributes) {
    log.info("POST /production-records/save, productType={}", productType);

    // Проверяем, что тип изделия указан
    if (productType == null || productType.isBlank()) {
        bindingResult.reject("error.record", "Выберите тип изделия");
    }

    // Валидация соответствующего ID
    if ("COUPLING".equals(productType) && record.getCoupling() == null) {
        bindingResult.rejectValue("coupling", "error.record", "Выберите муфту");
    }
    if ("ADAPTER".equals(productType) && record.getAdapter() == null) {
        bindingResult.rejectValue("adapter", "error.record", "Выберите переводник");
    }

    // Обнуляем неиспользуемое поле
    if ("COUPLING".equals(productType)) {
        record.setAdapter(null);
    } else if ("ADAPTER".equals(productType)) {
        record.setCoupling(null);
    }

    if (bindingResult.hasErrors()) {
        prepareFormModel(model);
        // Чтобы радиокнопки отобразили выбранный тип после ошибки
        model.addAttribute("currentProductType", productType);
        if ("COUPLING".equals(productType)) {
            model.addAttribute("currentCouplingId", record.getCoupling() != null ? record.getCoupling().getId() : null);
        } else if ("ADAPTER".equals(productType)) {
            model.addAttribute("currentAdapterId", record.getAdapter() != null ? record.getAdapter().getId() : null);
        }
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



    @GetMapping("/api/couplings/types")
    @ResponseBody
    public List<String> getCouplingTypes() {
        log.info("ReportController getCouplingTypes");
        return couplingService.findAllDistinctTypes(); // нужно добавить метод в CouplingService и CouplingRepository
    }

    @GetMapping("/api/couplings/by-type")
    @ResponseBody
    public List<Map<String, Object>> getCouplingsByType(@RequestParam String type) {
        log.info("ReportController getCouplingsBytype");
        List<CouplingEntity> couplings = couplingService.findByType(type);
        return couplings.stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("diameter", c.getConditionalDiameter());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/couplings/{id}")
    @ResponseBody

    public Map<String, String> getCouplingDetails(@PathVariable Long id) {
        log.info("ReportController getCouplingDetails");
        CouplingEntity coupling = couplingService.findById(id);
        Map<String, String> map = new HashMap<>();
        map.put("type", coupling.getType());
        map.put("diameter", coupling.getConditionalDiameter());
        return map;
    }

    private void prepareFormModel(Model model) {
        addReferenceData(model);
    } // общий метод для упаковки

    private void addReferenceData(Model model) { //общий метод для упаковки
        model.addAttribute("couplings", couplingService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("machines", machineService.findAll());
    }

    @GetMapping("/api/adapters/first-side-types")
    @ResponseBody
    public List<String> getAdapterFirstSideTypes() {
        return adapterService.findAllDistinctFirstSideTypes();
    }

    @GetMapping("/api/adapters/by-first-side-type")
    @ResponseBody
    public List<Map<String, Object>> getAdaptersByFirstSideType(@RequestParam String type) {
        List<PipeAdapterEntity> adapters = adapterService.findByFirstSideType(type);
        return adapters.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("firstSideDiameter", a.getFirstSideDiameter());
                    map.put("secondSideType", a.getSecondSideType());
                    map.put("secondSideDiameter", a.getSecondSideDiameter());
                    map.put("fullName", a.getFullName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/adapters/{id}")
    @ResponseBody
    public Map<String, Object> getAdapterDetails(@PathVariable Long id) {
        PipeAdapterEntity a = adapterService.findById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("firstSideType", a.getFirstSideType());
        map.put("firstSideDiameter", a.getFirstSideDiameter());
        map.put("secondSideType", a.getSecondSideType());
        map.put("secondSideDiameter", a.getSecondSideDiameter());
        return map;
    }
    @GetMapping("/api/adapters")
    @ResponseBody
    public List<Map<String, Object>> getAllAdapters() {
        List<PipeAdapterEntity> adapters = adapterService.findAll();
        return adapters.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("fullName", a.getFullName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
