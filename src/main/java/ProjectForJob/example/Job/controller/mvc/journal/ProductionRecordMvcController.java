package ProjectForJob.example.Job.controller.mvc.journal;

import ProjectForJob.example.Job.dto.ProductionRecordListDto;
import ProjectForJob.example.Job.domain.entity.order.PipeProductKind;
import ProjectForJob.example.Job.domain.entity.journal.ProductionRecordEntity;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.service.handbook.CouplingService;
import ProjectForJob.example.Job.service.handbook.EmployeeService;
import ProjectForJob.example.Job.service.handbook.ExternalThreadService;
import ProjectForJob.example.Job.service.handbook.MachinesService;
import ProjectForJob.example.Job.service.handbook.PipeAdapterService;
import ProjectForJob.example.Job.service.ProductionRecordService;
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
    private final EmployeeService employeeService;
    private final MachinesService machineService;
    private final PipeAdapterService adapterService;
    private final ExternalThreadService externalThreadService;

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_LIST = "fragments/production-records/list :: content";
    private static final String FRAGMENT_FORM = "fragments/production-records/form :: content";

    // ================================================================
    // СПИСОК
    // ================================================================
    @GetMapping
    public String listAll(@RequestParam(required = false) String productType,
                          @RequestParam(required = false) Long couplingId,
                          @RequestParam(required = false) Long adapterId,
                          @RequestParam(required = false) Long externalThreadId,
                          @RequestParam(required = false) Long employeeId,
                          @RequestParam(required = false) Long machineId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        log.info("GET /production-records filters page={} size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProductionRecordEntity> entityPage = recordService.findAllByFilters(
                productType, couplingId, adapterId, externalThreadId,
                employeeId, machineId, startDate, endDate, pageable);

        Page<ProductionRecordListDto> pageResult = entityPage.map(this::toListDto);

        model.addAttribute("recordsPage", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());

        addReferenceData(model);

        model.addAttribute("selectedProductType", productType);
        model.addAttribute("selectedCouplingId", couplingId);
        model.addAttribute("selectedAdapterId", adapterId);
        model.addAttribute("selectedExternalThreadId", externalThreadId);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedMachineId", machineId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return LAYOUT;
    }

    // ================================================================
    // СОЗДАНИЕ
    // ================================================================
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareFormModel(model);

        ProductionRecordEntity empty = new ProductionRecordEntity();
        empty.setProductionDate(LocalDate.now());   // ← дефолт
        model.addAttribute("record", empty);

        model.addAttribute("currentProductType", "");
        model.addAttribute("currentCouplingId", null);
        model.addAttribute("currentAdapterId", null);
        model.addAttribute("currentExternalThreadId", null);
        model.addAttribute("currentCouplingType", null);
        model.addAttribute("currentAdapterFirstSideType", null);
        model.addAttribute("currentExternalThreadType", null);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ================================================================
    // РЕДАКТИРОВАНИЕ
    // ================================================================
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductionRecordEntity record = recordService.findById(id);
        prepareFormModel(model);
        model.addAttribute("record", record);

        String productType = record.getProductType();
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
        } else if (("PIPE".equals(productType) || "NIPPLE".equals(productType))
                && record.getExternalThread() != null) {
            Long threadId = record.getExternalThread().getId();
            ExternalThreadEntity t = externalThreadService.findById(threadId);
            model.addAttribute("currentExternalThreadId", threadId);
            model.addAttribute("currentExternalThreadType", t != null ? t.getType() : null);
        }

        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ================================================================
    // СОХРАНЕНИЕ
    // ================================================================
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("record") ProductionRecordEntity record,
                       BindingResult bindingResult,
                       @RequestParam(value = "productType", required = false) String productType,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        log.info("POST /production-records/save productType={}", productType);

        // --- Нормализация связей: пустой id → null, иначе Hibernate создаст пустую сущность ---
        if (record.getCoupling() != null && record.getCoupling().getId() == null) {
            record.setCoupling(null);
        }
        if (record.getAdapter() != null && record.getAdapter().getId() == null) {
            record.setAdapter(null);
        }
        if (record.getExternalThread() != null && record.getExternalThread().getId() == null) {
            record.setExternalThread(null);
        }

        // --- Тип продукции обязателен ---
        if (productType == null || productType.isBlank()) {
            bindingResult.reject("error.record", "Выберите тип продукции");
        }

        boolean isCoupling = "COUPLING".equals(productType);
        boolean isAdapter  = "ADAPTER".equals(productType);
        boolean isPipe     = "PIPE".equals(productType);
        boolean isNipple   = "NIPPLE".equals(productType);

        // --- Проверка выбранного продукта ---
        if (isCoupling && record.getCoupling() == null) {
            bindingResult.rejectValue("coupling", "error.record", "Выберите муфту");
        }
        if (isAdapter && record.getAdapter() == null) {
            bindingResult.rejectValue("adapter", "error.record", "Выберите переводник");
        }
        if ((isPipe || isNipple) && record.getExternalThread() == null) {
            bindingResult.rejectValue("externalThread", "error.record", "Выберите резьбу");
        }

        // --- Длина патрубка: обязательна и > 0 только для NIPPLE ---
        if (isNipple
                && (record.getPieceLengthMm() == null || record.getPieceLengthMm() <= 0)) {
            bindingResult.rejectValue("pieceLengthMm", "error.record",
                    "Укажите длину патрубка в мм");
        }

        // --- Обнуляем неиспользуемые поля + фиксируем pipeProductKind ---
        if (isCoupling) {
            record.setAdapter(null);
            record.setExternalThread(null);
            record.setPipeProductKind(null);
            record.setPieceLengthMm(null);
        } else if (isAdapter) {
            record.setCoupling(null);
            record.setExternalThread(null);
            record.setPipeProductKind(null);
            record.setPieceLengthMm(null);
        } else if (isPipe) {
            record.setCoupling(null);
            record.setAdapter(null);
            record.setPipeProductKind(PipeProductKind.PIPE);
            record.setPieceLengthMm(null);   // длина — только у патрубка
        } else if (isNipple) {
            record.setCoupling(null);
            record.setAdapter(null);
            record.setPipeProductKind(PipeProductKind.NIPPLE);
        }

        if (bindingResult.hasErrors()) {
            prepareFormModel(model);
            model.addAttribute("currentProductType", productType);

            if (isCoupling && record.getCoupling() != null) {
                Long couplingId = record.getCoupling().getId();
                CouplingEntity c = couplingService.findById(couplingId);
                model.addAttribute("currentCouplingId", couplingId);
                model.addAttribute("currentCouplingType", c != null ? c.getType() : null);
            } else if (isAdapter && record.getAdapter() != null) {
                Long adapterId = record.getAdapter().getId();
                PipeAdapterEntity a = adapterService.findById(adapterId);
                model.addAttribute("currentAdapterId", adapterId);
                model.addAttribute("currentAdapterFirstSideType",
                        a != null ? a.getFirstSideType() : null);
            } else if ((isPipe || isNipple) && record.getExternalThread() != null) {
                Long threadId = record.getExternalThread().getId();
                ExternalThreadEntity t = externalThreadService.findById(threadId);
                model.addAttribute("currentExternalThreadId", threadId);
                model.addAttribute("currentExternalThreadType",
                        t != null ? t.getType() : null);
            }

            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return LAYOUT;
        }

        recordService.save(record);
        redirectAttributes.addFlashAttribute("message", "Запись успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/production-records";
    }

    // ================================================================
    // УДАЛЕНИЕ
    // ================================================================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /production-records/delete/{}", id);
        recordService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Запись удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/production-records";
    }

    /**
     * Локальный 404 для REST-эндпоинтов этого контроллера
     * (api/couplings, api/adapters, api/external-threads): их сервисы
     * бросают NoSuchElementException, а глобальный MvcErrorHandler
     * ловит только BusinessException и делает redirect — для AJAX это плохо.
     *
     * TODO (шаг 3): вынести REST-эндпоинты в ProductionRecordRestController
     *  и заменить этот хендлер на @RestControllerAdvice (RestErrorHandler).
     *  После этого @ExceptionHandler(NoSuchElementException.class)
     *  можно удалить полностью — он больше не нужен.
     *
     * Записи журнала (findById/deleteById) теперь бросают BusinessException
     * из ProductionRecordService и сюда НЕ попадают — их обрабатывает
     * MvcErrorHandler (redirect + flash).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    // ================================================================
    // REST API
    // ================================================================

    @GetMapping("/api/couplings/types")
    @ResponseBody
    public List<String> getCouplingTypes() {
        return couplingService.findAllDistinctTypes();
    }

    @GetMapping("/api/couplings/by-type")
    @ResponseBody
    public List<Map<String, Object>> getCouplingsByType(@RequestParam String type) {
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
        CouplingEntity coupling = couplingService.findById(id);
        Map<String, String> map = new HashMap<>();
        map.put("type", coupling.getType());
        map.put("diameter", coupling.getConditionalDiameter());
        return map;
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

    /** AJAX: диаметры наружных резьб по типу — для формы журнала. */
    @GetMapping("/api/external-threads/by-type")
    @ResponseBody
    public List<Map<String, Object>> getExternalThreadsByType(@RequestParam String type) {
        return externalThreadService.findByType(type).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("conditionalDiameter", t.getConditionalDiameter());
                    m.put("name", t.getName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** AJAX: детали конкретной резьбы — для фильтра в списке. */
    @GetMapping("/api/external-threads/{id}")
    @ResponseBody
    public Map<String, Object> getExternalThreadDetails(@PathVariable Long id) {
        ExternalThreadEntity t = externalThreadService.findById(id);
        Map<String, Object> m = new HashMap<>();
        m.put("type", t.getType());
        m.put("conditionalDiameter", t.getConditionalDiameter());
        m.put("name", t.getName());
        return m;
    }

    // ================================================================
    // ХЕЛПЕРЫ
    // ================================================================

    private void prepareFormModel(Model model) {
        addReferenceData(model);
    }

    private void addReferenceData(Model model) {
        model.addAttribute("couplings", couplingService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("machines", machineService.findAll());

        // Типы муфт — для блока «Муфта» в форме журнала
        model.addAttribute("couplingTypes", couplingService.findAllDistinctTypes());

        // Типы наружных резьб — для блока «Труба / Патрубок» и фильтра
        model.addAttribute("externalThreadTypes", externalThreadService.findAllDistinctTypes());

        // Типы первых сторон переводника — для блока «Переводник» и фильтра
        model.addAttribute("adapterFirstSideTypes", adapterService.findAllDistinctFirstSideTypes());
    }

    private ProductionRecordListDto toListDto(ProductionRecordEntity e) {
        String productType = e.getProductType();

        String productName = "—";
        if ("COUPLING".equals(productType) && e.getCoupling() != null) {
            // "Муфта " убираем — она и так в badge
            productName = e.getCoupling().getName();                       // "НКТ 73"
        } else if ("ADAPTER".equals(productType) && e.getAdapter() != null) {
            // "Переводник " убираем
            productName = e.getAdapter().getFullName();                    // "НКТ-НКТ 73/89"
        } else if (("PIPE".equals(productType) || "NIPPLE".equals(productType))
                && e.getExternalThread() != null) {
            // Собираем модель вручную, без префикса "Труба "/"Патрубок "
            productName = e.getExternalThread().getName();                 // "НКТ 89"
            if ("NIPPLE".equals(productType) && e.getPieceLengthMm() != null) {
                productName += " L-" + e.getPieceLengthMm().intValue();    // "НКТ 89 L-2000"
            }
        }

        ProductionRecordListDto dto = new ProductionRecordListDto();
        dto.setId(e.getId());
        dto.setProductionDate(e.getProductionDate());
        dto.setProductName(productName);
        dto.setQuantity(e.getQuantity());
        dto.setEmployeeName(e.getEmployee() != null ? e.getEmployee().getName() : "—");
        dto.setMachineName(e.getMachine() != null ? e.getMachine().getName() : "—");
        dto.setCreatedAt(e.getCreatedAt());
        dto.setComment(e.getComment());
        dto.setProductType(productType);
        dto.setPieceLengthMm(e.getPieceLengthMm());

        if (e.getCoupling() != null) dto.setCouplingId(e.getCoupling().getId());
        if (e.getAdapter() != null) dto.setAdapterId(e.getAdapter().getId());
        if (e.getExternalThread() != null) dto.setExternalThreadId(e.getExternalThread().getId());

        return dto;
    }
}