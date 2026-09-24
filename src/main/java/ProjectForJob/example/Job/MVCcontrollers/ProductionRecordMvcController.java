package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.DataTransferObject.ProductionRecordListDto;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.ProductionRecordEntity;
import ProjectForJob.example.Job.services.*;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import ProjectForJob.example.Job.services.Handbook.EmployeesService;
import ProjectForJob.example.Job.services.Handbook.MachinesService;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_LIST = "fragments/production-records/list :: content";
    private static final String FRAGMENT_FORM = "fragments/production-records/form :: content";


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

        // 1) тянем entity
        Page<ProductionRecordEntity> entityPage = recordService.findAllByFilters(
                productType, couplingId, adapterId, employeeId, machineId,
                startDate, endDate, pageable);

        // 2) маппим в DTO — вот это и было пропущено
        Page<ProductionRecordListDto> pageResult = entityPage.map(this::toListDto);

        model.addAttribute("recordsPage", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());

        addReferenceData(model);
        model.addAttribute("adapterFirstSideTypes", adapterService.findAllDistinctFirstSideTypes());

        model.addAttribute("selectedProductType", productType);
        model.addAttribute("selectedCouplingId", couplingId);
        model.addAttribute("selectedAdapterId", adapterId);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedMachineId", machineId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return LAYOUT;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareFormModel(model);
        model.addAttribute("record", new ProductionRecordEntity());
        model.addAttribute("currentProductType", "");
        model.addAttribute("currentCouplingId", null);
        model.addAttribute("currentAdapterId", null);
        model.addAttribute("currentCouplingType", null);
        model.addAttribute("currentAdapterFirstSideType", null);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductionRecordEntity record = recordService.findById(id);
        prepareFormModel(model);
        model.addAttribute("record", record);

        String productType = record.getProductType();
        model.addAttribute("currentProductType", productType);

        if ("COUPLING".equals(productType) && record.getCoupling() != null) {
            // сходим в сервис — вернёт полностью загруженную муфту
            Long couplingId = record.getCoupling().getId();
            CouplingEntity c = couplingService.findById(couplingId);
            model.addAttribute("currentCouplingId", couplingId);
            model.addAttribute("currentCouplingType", c != null ? c.getType() : null);
        } else if ("ADAPTER".equals(productType) && record.getAdapter() != null) {
            Long adapterId = record.getAdapter().getId();
            PipeAdapterEntity a = adapterService.findById(adapterId);
            model.addAttribute("currentAdapterId", adapterId);
            model.addAttribute("currentAdapterFirstSideType", a != null ? a.getFirstSideType() : null);
        }

        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
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
            model.addAttribute("currentProductType", productType);

            if ("COUPLING".equals(productType) && record.getCoupling() != null) {
                Long couplingId = record.getCoupling().getId();
                CouplingEntity c = couplingService.findById(couplingId);
                model.addAttribute("currentCouplingId", couplingId);
                model.addAttribute("currentCouplingType", c != null ? c.getType() : null);
            } else if ("ADAPTER".equals(productType) && record.getAdapter() != null) {
                Long adapterId = record.getAdapter().getId();
                PipeAdapterEntity a = adapterService.findById(adapterId);
                model.addAttribute("currentAdapterId", adapterId);
                model.addAttribute("currentAdapterFirstSideType", a != null ? a.getFirstSideType() : null);
            }

            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return LAYOUT;
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
        return couplingService.findAllDistinctTypes();
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

    private ProductionRecordListDto toListDto(ProductionRecordEntity e) {
        String productName = "—";
        if ("COUPLING".equals(e.getProductType()) && e.getCoupling() != null) {
            productName = "Муфта " + e.getCoupling().getConditionalDiameter();
        } else if ("ADAPTER".equals(e.getProductType()) && e.getAdapter() != null) {
            productName = e.getAdapter().getFullName();
        }

        return new ProductionRecordListDto(
                e.getId(),
                e.getProductionDate(),
                productName,
                e.getQuantity(),
                e.getEmployee() != null ? e.getEmployee().getName() : "—",
                e.getMachine()  != null ? e.getMachine().getName()  : "—",
                e.getCreatedAt(),
                e.getComment()
        );
    }
}
