package ProjectForJob.example.Job.controller.mvc.order;

import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.domain.entity.order.OrderEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderStatus;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.service.CostBreakdownService;
import ProjectForJob.example.Job.service.handbook.CouplingService;
import ProjectForJob.example.Job.service.handbook.PipeAdapterService;
import ProjectForJob.example.Job.service.OrderService;
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
    private final CostBreakdownService costBreakdownService;

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_OFFER = "fragments/offer/commercial-offer :: content";

    @GetMapping
    public String showCommercialOffer(HttpSession session, Model model) {
        log.info("CommercialOfferController showCommercialOffer");
        Set<Long> orderIds = getOrderIdsFromSession(session);
        if (orderIds.isEmpty()) {
            return "redirect:/orders/waiting";
        }

        List<OrderDto> validOrders = orderIds.stream()
                .map(orderService::getOrderById)
                .filter(dto -> dto.getStatus() == OrderStatus.WAITING)
                .toList();

        if (validOrders.isEmpty()) {
            session.removeAttribute("commercialOffer");
            return "redirect:/orders/waiting?error=empty";
        }

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

            // ← НОВОЕ: детальная расшифровка по позиции
            item.put("breakdown", costBreakdownService.buildBreakdown(dto));

            offerItems.add(item);
        }

        model.addAttribute("offerItems", offerItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("companyName", firstCompany);

        model.addAttribute("contentTemplate", FRAGMENT_OFFER);
        return LAYOUT;
    }

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

    @PostMapping("/clear")
    public String clearCommercialOffer(HttpSession session) {
        session.removeAttribute("commercialOffer");
        return "redirect:/orders/waiting";
    }

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