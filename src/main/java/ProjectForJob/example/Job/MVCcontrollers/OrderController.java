package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.entityJob.OrderStatus;
import ProjectForJob.example.Job.repositories.AdditionalWorkRepository;
import ProjectForJob.example.Job.repositories.CompanyRepository;
import ProjectForJob.example.Job.repositories.CouplingRepository;
import ProjectForJob.example.Job.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public String waitingOrders(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        List<OrderDto> orders = orderService.getOrdersByStatus(OrderStatus.WAITING);
        model.addAttribute("orders", orders);
        model.addAttribute("status", "WAITING");
        return "orders/waiting";
    }

    // Страница "В производстве"
    @GetMapping("/production")
    public String productionOrders(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        List<OrderDto> orders = orderService.getOrdersByStatus(OrderStatus.IN_PRODUCTION);
        model.addAttribute("orders", orders);
        model.addAttribute("status", "IN_PRODUCTION");
        return "orders/production";
    }

    // Страница "Выполненные"
    @GetMapping("/completed")
    public String completedOrders(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        List<OrderDto> orders = orderService.getOrdersByStatus(OrderStatus.COMPLETED);
        model.addAttribute("orders", orders);
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
}