package ProjectForJob.example.Job.MVCcontrollers.ForOrders;

import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import ProjectForJob.example.Job.entityJob.Handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.services.Handbook.AdditionalWorkService;
import ProjectForJob.example.Job.services.Handbook.CompanyService;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import ProjectForJob.example.Job.services.Handbook.ExternalThreadService;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
import ProjectForJob.example.Job.services.OrderService;
import ProjectForJob.example.Job.services.Handbook.SteelStrengthGroupService;
import ProjectForJob.example.Job.DataTransferObject.CostBreakdownDto;
import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CompanyService companyService;
    private final CouplingService couplingService;
    private final AdditionalWorkService additionalWorkService;
    private final PipeAdapterService pipeAdapterService;
    private final SteelStrengthGroupService steelStrengthGroupService;
    private final ExternalThreadService externalThreadService;

    private static final String LAYOUT = "welcomeToHome/layout";

    private static final String FRAGMENT_WAITING    = "fragments/orders/waiting :: content";
    private static final String FRAGMENT_PRODUCTION = "fragments/orders/production :: content";
    private static final String FRAGMENT_COMPLETED  = "fragments/orders/completed :: content";
    private static final String FRAGMENT_VIEW       = "fragments/orders/view :: content";
    private static final String FRAGMENT_CREATE     = "fragments/orders/create :: content";
    private static final String FRAGMENT_EDIT       = "fragments/orders/edit :: content";

    // ================================================================
    // Хелпер — заполняет справочники для формы (create / edit)
    // ================================================================
    private void fillFormRefs(Model model) {
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("couplings", couplingService.findAll());
        model.addAttribute("additionalWorks", additionalWorkService.findAll());
        model.addAttribute("adapterFirstSideTypes", pipeAdapterService.findAllDistinctFirstSideTypes());
        model.addAttribute("steelGroups", steelStrengthGroupService.findAllActive());
        // типы наружных резьб — для каскада в форме трубы/патрубка
        model.addAttribute("externalThreadTypes", externalThreadService.findAllDistinctTypes());
    }

    // ================================================================
    // Список: В ожидании
    // ================================================================
    @GetMapping("/waiting")
    public String waitingOrders(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                @RequestParam(required = false) String search,
                                HttpServletRequest request,
                                HttpSession session) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("search", search);
        log.info("OrderController waiting orders");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderDto> ordersPage = orderService.getOrdersByStatus(OrderStatus.WAITING, search, pageable);

        List<Map<String, Object>> waitingItems = new ArrayList<>();
        for (OrderDto orderDto : ordersPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("order", orderDto);
            item.put("product", null);       // ← ключ всегда есть
            item.put("productType", null);   // ← ключ всегда есть

            String pt = orderDto.getProductType();
            if ("COUPLING".equals(pt) && orderDto.getProductId() != null) {
                CouplingEntity coupling = couplingService.findById(orderDto.getProductId());
                item.put("product", coupling);
                item.put("productType", "COUPLING");
            } else if ("ADAPTER".equals(pt) && orderDto.getProductId() != null) {
                PipeAdapterEntity adapter = pipeAdapterService.findById(orderDto.getProductId());
                item.put("product", adapter);
                item.put("productType", "ADAPTER");
            } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
                item.put("productType", pt);
                // product остаётся null — шаблон это понимает
            }
            waitingItems.add(item);
        }

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("waitingItems", waitingItems);
        model.addAttribute("status", "WAITING");

        Set<Long> selectedOrderIds = (Set<Long>) session.getAttribute("commercialOffer");
        if (selectedOrderIds == null) selectedOrderIds = Collections.emptySet();
        model.addAttribute("selectedOrderIds", selectedOrderIds);

        model.addAttribute("contentTemplate", FRAGMENT_WAITING);
        return LAYOUT;
    }

    // ================================================================
    // Список: В производстве
    // ================================================================
    @GetMapping("/production")
    public String productionOrders(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size,
                                   @RequestParam(required = false) String search,
                                   HttpServletRequest request) {
        log.info("OrderController production orders");
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("search", search);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderDto> ordersPage = orderService.getOrdersByStatus(OrderStatus.IN_PRODUCTION, search, pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("status", "IN_PRODUCTION");
        model.addAttribute("contentTemplate", FRAGMENT_PRODUCTION);
        return LAYOUT;
    }

    // ================================================================
    // Список: Выполненные
    // ================================================================
    @GetMapping("/completed")
    public String completedOrders(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  @RequestParam(required = false) String search,
                                  HttpServletRequest request) {
        log.info("OrderController completed orders");
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("search", search);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderDto> ordersPage = orderService.getOrdersByStatus(OrderStatus.COMPLETED, search, pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("status", "COMPLETED");
        model.addAttribute("contentTemplate", FRAGMENT_COMPLETED);
        return LAYOUT;
    }

    // ================================================================
    // Создание
    // ================================================================
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("orderCreateDto", new OrderCreateDto());
        fillFormRefs(model);
        model.addAttribute("contentTemplate", FRAGMENT_CREATE);
        return LAYOUT;
    }

    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("orderCreateDto") OrderCreateDto dto,
                              BindingResult result,
                              Model model) {
        log.info("OrderController create POST orders");
        if (result.hasErrors()) {
            fillFormRefs(model);
            model.addAttribute("contentTemplate", FRAGMENT_CREATE);
            return LAYOUT;
        }
        orderService.createOrder(dto);
        return "redirect:/orders/waiting";
    }

    // ================================================================
    // Просмотр
    // ================================================================
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        log.info("OrderController viewOrder GET order with id={}", id);
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("breakdown", buildBreakdown(order));   // ← НОВОЕ
        model.addAttribute("contentTemplate", FRAGMENT_VIEW);
        return LAYOUT;
    }

    // ================================================================
    // Смена статуса / удаление — БЕЗ layout (это POST-редиректы)
    // ================================================================
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam OrderStatus newStatus,
                               @RequestParam(required = false) String returnTo) {
        log.info("OrderController change status with id={}, newStatus={}", id, newStatus);

        OrderDto order = orderService.getOrderById(id);
        OrderStatus currentStatus = order.getStatus();

        if (newStatus == OrderStatus.IN_PRODUCTION
                && currentStatus == OrderStatus.WAITING) {
            orderService.startProduction(id, null);
        } else {
            orderService.updateStatus(id, newStatus);
        }

        return (returnTo != null && !returnTo.isEmpty())
                ? "redirect:" + returnTo
                : "redirect:/orders/waiting";
    }

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
                              @RequestParam(required = false) String returnTo) {
        log.info("OrderController delete order id={}", id);
        orderService.deleteOrder(id);
        return (returnTo != null && !returnTo.isEmpty())
                ? "redirect:" + returnTo
                : "redirect:/orders/waiting";
    }

    // ================================================================
    // Редактирование
    // ================================================================

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        OrderDto order = orderService.getOrderById(id);
        OrderUpdateDto dto = new OrderUpdateDto();

        dto.setCompanyName(order.getCompanyName());
        dto.setProductType(order.getProductType());

        // === Продукт: определяем по типу ===
        String pt = order.getProductType();
        if ("COUPLING".equals(pt)) {
            dto.setCouplingId(order.getProductId());
        } else if ("ADAPTER".equals(pt)) {
            dto.setAdapterId(order.getProductId());
        } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
            // Труба / патрубок — переносим всё, что нужно для повторного расчёта
            dto.setExternalThreadId(order.getExternalThreadId());
            dto.setPipeProductKind(order.getPipeProductKind());
            dto.setWallThicknessMm(order.getWallThicknessMm());
            dto.setAveragePipeLengthM(order.getAveragePipeLengthM());
            dto.setOrderTotalLengthMeters(order.getOrderTotalLengthMeters());
            dto.setOrderTotalWeightKg(order.getOrderTotalWeightKg());
            dto.setPricePerTon(order.getPricePerTon());
            dto.setPieceLengthMm(order.getPieceLengthMm());
        }

        // Группа прочности + марка — переносим, чтобы не сбросить бэкфилл
        dto.setStrengthGroupId(order.getStrengthGroupId());
        dto.setSteelGradeId(order.getSteelGradeId());

        // quantity только для не-PIPE — для PIPE сервер сам посчитает
        if (!"PIPE".equals(pt)) {
            dto.setQuantity(order.getQuantity());
        }

        dto.setDeadline(order.getDeadline());
        dto.setAdditionalWorkIds(order.getAdditionalWorkIds());

        model.addAttribute("order", order);
        model.addAttribute("orderUpdateDto", dto);
        fillFormRefs(model);

        // === Дополнительные атрибуты для предзаполнения каскадов ===
        if ("COUPLING".equals(pt)) {
            CouplingEntity coupling = couplingService.findById(order.getProductId());
            model.addAttribute("currentCouplingType",
                    coupling != null ? coupling.getType() : "");
            model.addAttribute("currentCouplingDiameter",
                    coupling != null ? coupling.getConditionalDiameter() : "");
        } else if ("ADAPTER".equals(pt)) {
            PipeAdapterEntity adapter = pipeAdapterService.findById(order.getProductId());
            model.addAttribute("currentAdapterFirstSideType", adapter.getFirstSideType());
            model.addAttribute("currentAdapterId", adapter.getId());
        } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
            // Данные для предзаполнения каскада «тип → диаметр»
            ExternalThreadEntity thread = externalThreadService.findById(order.getExternalThreadId());
            model.addAttribute("currentExternalThreadType", thread.getType());
            model.addAttribute("currentExternalThreadId", thread.getId());
            model.addAttribute("currentExternalThreadDiameter", thread.getConditionalDiameter());
        }

        model.addAttribute("contentTemplate", FRAGMENT_EDIT);
        return LAYOUT;
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id,
                              @Valid @ModelAttribute("orderUpdateDto") OrderUpdateDto dto,
                              BindingResult result,
                              Model model) {
        log.info("OrderController update order id={}", id);
        if (result.hasErrors()) {
            fillFormRefs(model);
            model.addAttribute("contentTemplate", FRAGMENT_EDIT);
            return LAYOUT;
        }
        orderService.updateOrder(id, dto);
        return "redirect:/orders/" + id;
    }

    // ================================================================
    // AJAX: старт производства с дедлайном
    // ================================================================
    @PostMapping("/{id}/set-deadline-and-start")
    @ResponseBody
    public Map<String, Object> setDeadlineAndStartProduction(
            @PathVariable Long id,
            @RequestParam("deadline")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate deadline) {
        Map<String, Object> response = new HashMap<>();
        log.info("setDeadlineAndStartProduction id={}", id);
        try {
            orderService.startProduction(id, deadline);
            response.put("success", true);
        } catch (Exception e) {
            log.error("Ошибка при старте производства заказа {}", id, e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    // ================================================================
    // REST API — без layout
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

    @GetMapping("/api/adapters/first-side-types")
    @ResponseBody
    public List<String> getAdapterFirstSideTypes() {
        return pipeAdapterService.findAllDistinctFirstSideTypes();
    }

    @GetMapping("/api/adapters/by-first-side-type")
    @ResponseBody
    public List<Map<String, Object>> getAdaptersByFirstSideType(@RequestParam String type) {
        List<PipeAdapterEntity> adapters = pipeAdapterService.findByFirstSideType(type);
        return adapters.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("fullName", a.getFullName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * AJAX: список активных марок для группы прочности.
     * Используется для каскада «группа → марка» в форме заказа.
     */
    @GetMapping("/api/steel-groups/{id}/grades")
    @ResponseBody
    public List<Map<String, Object>> getSteelGradesByGroup(@PathVariable Long id) {
        return steelStrengthGroupService.findActiveGradesByGroup(id).stream()
                .map(g -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", g.getId());
                    map.put("name", g.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * AJAX: диаметры наружных резьб по типу.
     * Каскад «тип → диаметр» в форме заказа для трубы/патрубка.
     */
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


    private CostBreakdownDto buildBreakdown(OrderDto order) {
        CostBreakdownDto b = new CostBreakdownDto();
        b.setProductType(order.getProductType());
        b.setQuantity(order.getQuantity());
        b.setTotalCost(order.getTotalCost());
        b.setStrengthGroupCode(order.getStrengthGroupCode());

        BigDecimal qty = BigDecimal.valueOf(order.getQuantity() != null ? order.getQuantity() : 0);
        BigDecimal markupPct = order.getAppliedMarkupPercent() != null
                ? order.getAppliedMarkupPercent() : BigDecimal.ZERO;
        b.setMarkupPercent(markupPct);

        String pt = order.getProductType();

        if ("COUPLING".equals(pt) || "ADAPTER".equals(pt)) {
            BigDecimal unit = order.getProductManufacturingCost() != null
                    ? order.getProductManufacturingCost() : BigDecimal.ZERO;
            b.setUnitCost(unit);
            b.setBaseCost(unit.multiply(qty));
            b.setTotalWeightKg(order.getWorkpieceWeightKg());
        } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
            BigDecimal thread = order.getExternalThreadManufacturingCost() != null
                    ? order.getExternalThreadManufacturingCost() : BigDecimal.ZERO;
            b.setThreadCost(thread);
            BigDecimal threadsCost = thread.multiply(BigDecimal.valueOf(2)).multiply(qty);
            b.setThreadsCost(threadsCost);

            BigDecimal ppt = order.getPricePerTon() != null
                    ? order.getPricePerTon() : BigDecimal.ZERO;
            b.setPricePerTon(ppt);
            double wKg = order.getOrderTotalWeightKg() != null ? order.getOrderTotalWeightKg() : 0;
            b.setTotalWeightKg(wKg);

            BigDecimal metalCost = ppt
                    .multiply(BigDecimal.valueOf(wKg))
                    .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            b.setMetalCost(metalCost);
            b.setBaseCost(threadsCost.add(metalCost));
        } else {
            b.setBaseCost(BigDecimal.ZERO);
        }

        BigDecimal markup = b.getBaseCost()
                .multiply(markupPct)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        b.setMarkupCost(markup);

        double weight = b.getTotalWeightKg() != null ? b.getTotalWeightKg() : 0;
        double billing = Math.max(weight, 100);
        b.setBillingWeight(billing);

        List<CostBreakdownDto.WorkLine> lines = new ArrayList<>();
        BigDecimal worksTotal = BigDecimal.ZERO;
        if (order.getAdditionalWorkIds() != null) {
            for (Long wid : order.getAdditionalWorkIds()) {
                AdditionalWorkEntity w = additionalWorkService.findById(wid);
                if (w == null || w.getPrice() == null) continue;
                BigDecimal cost = w.getPrice()
                        .multiply(BigDecimal.valueOf(billing))
                        .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                lines.add(new CostBreakdownDto.WorkLine(w.getName(), w.getPrice(), cost));
                worksTotal = worksTotal.add(cost);
            }
        }
        b.setWorks(lines);
        b.setWorksTotal(worksTotal);

        return b;
    }
}