package ProjectForJob.example.Job.MVCcontrollers.ForOrders;

import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.services.Handbook.AdditionalWorkService;
import ProjectForJob.example.Job.services.Handbook.CompanyService;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
import ProjectForJob.example.Job.services.OrderService;
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

    // ================================================================
    // Путь к layout — как в CouplingMvcController
    // ================================================================
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
            if ("COUPLING".equals(orderDto.getProductType()) && orderDto.getProductId() != null) {
                CouplingEntity coupling = couplingService.findById(orderDto.getProductId());
                item.put("product", coupling);
                item.put("productType", "COUPLING");
            } else if ("ADAPTER".equals(orderDto.getProductType()) && orderDto.getProductId() != null) {
                PipeAdapterEntity adapter = pipeAdapterService.findById(orderDto.getProductId());
                item.put("product", adapter);
                item.put("productType", "ADAPTER");
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
        if (newStatus == OrderStatus.IN_PRODUCTION) {
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
        if ("COUPLING".equals(order.getProductType())) {
            dto.setCouplingId(order.getProductId());
        } else {
            dto.setAdapterId(order.getProductId());
        }
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setAdditionalWorkIds(order.getAdditionalWorkIds());

        model.addAttribute("order", order);
        model.addAttribute("orderUpdateDto", dto);
        fillFormRefs(model);

        if ("COUPLING".equals(order.getProductType())) {
            CouplingEntity coupling = couplingService.findById(order.getProductId());
            model.addAttribute("currentCouplingType", coupling != null ? coupling.getType() : "");
            model.addAttribute("currentCouplingDiameter", coupling != null ? coupling.getConditionalDiameter() : "");
        } else {
            PipeAdapterEntity adapter = pipeAdapterService.findById(order.getProductId());
            model.addAttribute("currentAdapterFirstSideType", adapter.getFirstSideType());
            model.addAttribute("currentAdapterId", adapter.getId());
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
}