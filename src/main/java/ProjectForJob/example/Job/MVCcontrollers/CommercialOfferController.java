package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.OrderEntity;
import ProjectForJob.example.Job.entityJob.OrderStatus;
import ProjectForJob.example.Job.services.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/commercial-offer")
@RequiredArgsConstructor
@Slf4j
public class CommercialOfferController {

    private final OrderService orderService;

    // Отображение страницы КП
    @GetMapping
    public String showCommercialOffer(HttpSession session, Model model) {
        log.info("CommercialOfferController showCommercialOffer");
        Set<Long> orderIds = getOrderIdsFromSession(session);
        if (orderIds.isEmpty()) {
            return "redirect:/orders/waiting";
        }

        List<OrderEntity> orders = orderService.getOrdersByIdsWithDetails(new ArrayList<>(orderIds));
        // Оставляем только заказы в статусе WAITING (на случай, если статус изменился после добавления)
        List<OrderEntity> validOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING)
                .toList();

        if (validOrders.isEmpty()) {
            session.removeAttribute("commercialOffer");
            return "redirect:/orders/waiting?error=empty";
        }

        // Проверка принадлежности одной компании
        Long firstCompanyId = validOrders.get(0).getCompany().getId();
        boolean sameCompany = validOrders.stream()
                .allMatch(o -> o.getCompany().getId().equals(firstCompanyId));

        if (!sameCompany) {
            model.addAttribute("error", "В КП добавлены заказы разных компаний. Очистите КП и добавляйте заказы только одной компании.");
        }

        // Итоговые значения
        BigDecimal totalPrice = validOrders.stream()
                .map(OrderEntity::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        model.addAttribute("orders", validOrders);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("companyName", validOrders.get(0).getCompany().getName());

        return "offer/commercial-offer";
    }

    // AJAX: добавить/удалить заказ в КП
    @PostMapping("/add/{orderId}")
    @ResponseBody
    public Map<String, Object> toggleOrder(@PathVariable Long orderId, HttpSession session) {
        log.info("OrderController toggleOrder");
        Set<Long> orderIds = getOrderIdsFromSession(session);
        Map<String, Object> response = new HashMap<>();
        String error = null;
        boolean added = false;

        try {
            // Проверка компании, если КП не пусто
            if (!orderIds.isEmpty()) {
                Long firstOrderId = orderIds.iterator().next();
                OrderEntity firstOrder = orderService.getOrderEntityById(firstOrderId);
                OrderEntity newOrder = orderService.getOrderEntityById(orderId);
                if (!firstOrder.getCompany().getId().equals(newOrder.getCompany().getId())) {
                    error = "Нельзя добавить заказ другой компании. Текущее КП для компании: " + firstOrder.getCompany().getName();
                    response.put("error", error);
                    return response;
                }
            }

            if (orderIds.contains(orderId)) {
                orderIds.remove(orderId);
                added = false;
            } else {
                OrderEntity order = orderService.getOrderEntityById(orderId);
                if (order.getStatus() != OrderStatus.WAITING) {
                    error = "Заказ не в статусе ожидания";
                    response.put("error", error);
                    return response;
                }
                orderIds.add(orderId);
                added = true;
            }

            session.setAttribute("commercialOffer", orderIds);
            response.put("added", added);
            response.put("count", orderIds.size());
        } catch (Exception e) {
            response.put("error", "Ошибка при добавлении заказа: " + e.getMessage());
        }
        return response;
    }

    // Очистка всей КП (редирект на страницу ожидания)
    @PostMapping("/clear")
    public String clearCommercialOffer(HttpSession session) {
        log.info("OrderController clearCommercialOffer");
        session.removeAttribute("commercialOffer");
        return "redirect:/orders/waiting";
    }

    // Удаление одного заказа из КП (со страницы КП)
    @PostMapping("/remove/{orderId}")
    public String removeOrder(@PathVariable Long orderId, HttpSession session) {
        log.info("OrderController removeOrder");
        Set<Long> orderIds = getOrderIdsFromSession(session);
        orderIds.remove(orderId);
        if (orderIds.isEmpty()) {
            session.removeAttribute("commercialOffer");
        } else {
            session.setAttribute("commercialOffer", orderIds);
        }
        return "redirect:/commercial-offer";
    }

    private Set<Long> getOrderIdsFromSession(HttpSession session) {
        Set<Long> orderIds = (Set<Long>) session.getAttribute("commercialOffer");
        if (orderIds == null) {
            orderIds = new LinkedHashSet<>();
        }
        return orderIds;
    }
}