package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.OrderStatus;
import ProjectForJob.example.Job.repositories.AdditionalWorkRepository;
import ProjectForJob.example.Job.repositories.CompanyRepository;
import ProjectForJob.example.Job.repositories.CouplingRepository;
import ProjectForJob.example.Job.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final CompanyRepository companyRepository;
    private final CouplingRepository couplingRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    // Страница "В ожидании"
    @GetMapping("/waiting")
    public String waitingOrders(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                @RequestParam(required = false) String search,
                                HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("search", search);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderDto> ordersPage = orderService.getOrdersByStatus(OrderStatus.WAITING, search, pageable);


        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("status", "WAITING");
        return "orders/waiting";
    }

    // Страница "В производстве"
    @GetMapping("/production")
    public String productionOrders(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size,
                                   @RequestParam(required = false) String search,
                                   HttpServletRequest request) {
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
        model.addAttribute("companies", companyRepository.findAll());
        model.addAttribute("couplings", couplingRepository.findAll());
        model.addAttribute("additionalWorks", additionalWorkRepository.findAll());
        return "orders/create";
    }

    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("orderCreateDto") OrderCreateDto dto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("companies", companyRepository.findAll());
            model.addAttribute("couplings", couplingRepository.findAll());
            model.addAttribute("additionalWorks", additionalWorkRepository.findAll());
            return "orders/create";
        }
        orderService.createOrder(dto);
        return "redirect:/orders/waiting";
    }

    // Просмотр деталей заказа (кликабельный заказ)
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "orders/view";
    }

    // Изменение статуса
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam OrderStatus newStatus,
                               @RequestParam(required = false) String returnTo) {
        orderService.updateStatus(id, newStatus);
        // Возвращаемся на ту же страницу, откуда пришли (чтобы не терять контекст)
        if (returnTo != null && !returnTo.isEmpty()) {
            return "redirect:" + returnTo;
        }
        return "redirect:/orders/waiting";
    }

    // Удаление заказа
    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
                              @RequestParam(required = false) String returnTo) {
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
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setCompanyName(order.getCompanyName());
        updateDto.setCouplingId(order.getCouplingId()); // нужно в OrderDto добавить поле couplingId
        updateDto.setQuantity(order.getQuantity());
        updateDto.setDeadline(order.getDeadline());
        updateDto.setAdditionalWorkIds(order.getAdditionalWorkIds());
        // Для доп. работ нужно знать их id, чтобы в форме отметить выбранные
        // Лучше передать список id выбранных доп. работ
        // Поэтому добавим в OrderDto поле additionalWorkIds
        // Временно можно собрать из order.getAdditionalWorkNames, но это неудобно.
        // Поэтому изменим OrderDto, добавим поле List<Long> additionalWorkIds.
        // Для этого придётся изменить маппинг.

        model.addAttribute("order", order);
        model.addAttribute("orderUpdateDto", updateDto);
        model.addAttribute("companies", companyRepository.findAll());
        model.addAttribute("couplings", couplingRepository.findAll());
        model.addAttribute("additionalWorks", additionalWorkRepository.findAll());
        return "orders/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id,
                              @Valid @ModelAttribute("orderUpdateDto") OrderUpdateDto dto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            // при ошибке нужно снова загрузить справочники
            model.addAttribute("companies", companyRepository.findAll());
            model.addAttribute("couplings", couplingRepository.findAll());
            model.addAttribute("additionalWorks", additionalWorkRepository.findAll());
            return "orders/edit";
        }
        orderService.updateOrder(id, dto);
        return "redirect:/orders/" + id;
    }
}