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


    // Страница "В ожидании"
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

        // 🔧 Обогащаем: подгружаем продукт для каждого заказа
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

        model.addAttribute("ordersPage", ordersPage);          // для пагинации
        model.addAttribute("waitingItems", waitingItems);      // для карточек
        model.addAttribute("status", "WAITING");

        Set<Long> selectedOrderIds = (Set<Long>) session.getAttribute("commercialOffer");
        if (selectedOrderIds == null) {
            selectedOrderIds = Collections.emptySet();
        }
        model.addAttribute("selectedOrderIds", selectedOrderIds);
        return "orders/waiting";
    }

    // Страница "В производстве"
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
        return "orders/production";
    }

    // Страница "Выполненные"
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
        return "orders/completed";
    }

    // Форма создания заказа
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("orderCreateDto", new OrderCreateDto());
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("couplings", couplingService.findAll());
        model.addAttribute("additionalWorks", additionalWorkService.findAll());
        model.addAttribute("adapterFirstSideTypes", pipeAdapterService.findAllDistinctFirstSideTypes());
        return "orders/create";
    }

    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("orderCreateDto") OrderCreateDto dto,
                              BindingResult result,
                              Model model) {
        log.info("OrderController create POST orders");
        if (result.hasErrors()) {
            model.addAttribute("companies", companyService.findAll());
            model.addAttribute("couplings", couplingService.findAll());
            model.addAttribute("additionalWorks", additionalWorkService.findAll());
            return "orders/create";
        }
        orderService.createOrder(dto);
        return "redirect:/orders/waiting";
    }

    // Просмотр деталей заказа (кликабельный заказ)
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        log.info("OrderController viewOrder GET order with id={}", id);
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/view";
    }

    // Изменение статуса
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam OrderStatus newStatus,
                               @RequestParam(required = false) String returnTo) {
        log.info("OrderController change status with id={}, newStatus={}", id, newStatus);
        if (newStatus == OrderStatus.IN_PRODUCTION) {
            orderService.startProduction(id, null);  // дедлайн не меняем
        } else {
            orderService.updateStatus(id, newStatus);
        }
        if (returnTo != null && !returnTo.isEmpty()) {
            return "redirect:" + returnTo;
        }
        return "redirect:/orders/waiting";
    }

    // Удаление заказа
    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
                              @RequestParam(required = false) String returnTo) {
        log.info("OrderController bу delete order with id={}", id);
        orderService.deleteOrder(id);
        if (returnTo != null && !returnTo.isEmpty()) {
            return "redirect:" + returnTo;
        }
        return "redirect:/orders/waiting";
    }

    // Форма редактирования заказа
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
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("additionalWorks", additionalWorkService.findAll());
        model.addAttribute("adapterFirstSideTypes", pipeAdapterService.findAllDistinctFirstSideTypes());
        // Для муфт можно передать текущий тип и диаметр для JS
        if ("COUPLING".equals(order.getProductType())) {
            CouplingEntity coupling = couplingService.findById(order.getProductId());
            model.addAttribute("currentCouplingType", coupling != null ? coupling.getType() : "");
            model.addAttribute("currentCouplingDiameter", coupling != null ? coupling.getConditionalDiameter() : "");
        } else {
            PipeAdapterEntity adapter = pipeAdapterService.findById(order.getProductId());
            model.addAttribute("currentAdapterFirstSideType", adapter.getFirstSideType());
            model.addAttribute("currentAdapterId", adapter.getId());
        }
        return "orders/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id,
                              @Valid @ModelAttribute("orderUpdateDto") OrderUpdateDto dto,
                              BindingResult result,
                              Model model) {
        log.info("OrderController update GET order with id={}", id);
        if (result.hasErrors()) {
            // при ошибке нужно снова загрузить справочники
            model.addAttribute("companies", companyService.findAll());
            model.addAttribute("couplings", couplingService.findAll());
            model.addAttribute("additionalWorks", additionalWorkService.findAll());
            return "orders/edit";
        }
        orderService.updateOrder(id, dto);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/set-deadline-and-start")
    @ResponseBody
    public Map<String, Object> setDeadlineAndStartProduction(
            @PathVariable Long id,
            @RequestParam("deadline")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate deadline) {
        Map<String, Object> response = new HashMap<>();
        log.info("Called method setDeadlineAndStartProduction from order id={}", id);
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

    @GetMapping("/api/couplings/types")
    @ResponseBody
    public List<String> getCouplingTypes() {
        log.info("OrderController getCouplingTypes");
        return couplingService.findAllDistinctTypes();
    }

    @GetMapping("/api/couplings/by-type")
    @ResponseBody
    public List<Map<String, Object>> getCouplingsByType(@RequestParam String type) {
        log.info("OrderController getCouplingsByType GET order by type={}", type);
        List<CouplingEntity> couplings = couplingService.findByType(type);
        return couplings.stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("diameter", c.getConditionalDiameter()); // условный диаметр
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