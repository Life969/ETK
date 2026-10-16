package ProjectForJob.example.Job.MVCcontrollers.ForOrders;

import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
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
    private final CouplingService couplingService;
    private final PipeAdapterService pipeAdapterService;

    // ==== layout + фрагмент (единый стиль с остальными страницами) ====
    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_OFFER = "fragments/offer/commercial-offer :: content";

    // Отображение страницы КП
    @GetMapping
    public String showCommercialOffer(HttpSession session, Model model) {
        log.info("CommercialOfferController showCommercialOffer");
        Set<Long> orderIds = getOrderIdsFromSession(session);
        if (orderIds.isEmpty()) {
            return "redirect:/orders/waiting";
        }

        // Берём DTO (в нём есть поля для тултипов)
        List<OrderDto> validOrders = orderIds.stream()
                .map(orderService::getOrderById)          // ← метод, который отдаёт OrderDto
                .filter(dto -> dto.getStatus() == OrderStatus.WAITING)
                .toList();

        if (validOrders.isEmpty()) {
            session.removeAttribute("commercialOffer");
            return "redirect:/orders/waiting?error=empty";
        }

        Long firstCompanyId = null;
        // проверка «одна компания» — по имени как раньше
        // (в OrderDto нет companyId, есть companyName)
        String firstCompany = validOrders.get(0).getCompanyName();
        boolean sameCompany = validOrders.stream()
                .allMatch(o -> firstCompany.equals(o.getCompanyName()));
        if (!sameCompany) {
            model.addAttribute("error",
                    "В КП добавлены заказы разных компаний. Очистите КП и добавляйте заказы только одной компании.");
        }

        BigDecimal totalPrice = validOrders.stream()
                .map(OrderDto::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // === Собираем offerItems ровно как waitingItems в OrderController ===
        List<Map<String, Object>> offerItems = new ArrayList<>();
        for (OrderDto dto : validOrders) {
            Map<String, Object> item = new HashMap<>();
            item.put("order", dto);
            item.put("product", null);
            item.put("productType", null);

            String pt = dto.getProductType();
            if ("COUPLING".equals(pt) && dto.getProductId() != null) {
                CouplingEntity c = couplingService.findById(dto.getProductId());
                item.put("product", c);
                item.put("productType", "COUPLING");
            } else if ("ADAPTER".equals(pt) && dto.getProductId() != null) {
                PipeAdapterEntity a = pipeAdapterService.findById(dto.getProductId());
                item.put("product", a);
                item.put("productType", "ADAPTER");
            } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
                item.put("productType", pt);
            }
            offerItems.add(item);
        }

        model.addAttribute("offerItems", offerItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("companyName", firstCompany);

        model.addAttribute("contentTemplate", FRAGMENT_OFFER);
        return LAYOUT;
    }

    // AJAX: добавить/удалить заказ в КП
    @PostMapping("/add/{orderId}")
    @ResponseBody
    public Map<String, Object> toggleOrder(@PathVariable Long orderId, HttpSession session) {
        Set<Long> orderIds = getOrderIdsFromSession(session);
        Map<String, Object> response = new HashMap<>();
        String error;
        boolean added = false;

        try {
            if (!orderIds.isEmpty()) {
                Long firstOrderId = orderIds.iterator().next();
                OrderEntity firstOrder = orderService.getOrderEntityById(firstOrderId);
                OrderEntity newOrder = orderService.getOrderEntityById(orderId);
                if (!firstOrder.getCompany().getId().equals(newOrder.getCompany().getId())) {
                    error = "Нельзя добавить заказ другой компании. Текущее КП для компании: "
                            + firstOrder.getCompany().getName();
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

    // Очистка всей КП
    @PostMapping("/clear")
    public String clearCommercialOffer(HttpSession session) {
        session.removeAttribute("commercialOffer");
        return "redirect:/orders/waiting";
    }

    // Удаление одного заказа из КП
    @PostMapping("/remove/{orderId}")
    public String removeOrder(@PathVariable Long orderId, HttpSession session) {
        Set<Long> orderIds = getOrderIdsFromSession(session);
        orderIds.remove(orderId);
        if (orderIds.isEmpty()) {
            session.removeAttribute("commercialOffer");
            return "redirect:/orders/waiting";
        }
        session.setAttribute("commercialOffer", orderIds);
        return "redirect:/commercial-offer";
    }

    private Set<Long> getOrderIdsFromSession(HttpSession session) {
        @SuppressWarnings("unchecked")
        Set<Long> orderIds = (Set<Long>) session.getAttribute("commercialOffer");
        if (orderIds == null) {
            orderIds = new LinkedHashSet<>();
        }
        return orderIds;
    }
}