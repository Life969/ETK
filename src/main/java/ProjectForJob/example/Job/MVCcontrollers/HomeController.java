package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.ProductionRecordEntity;
import ProjectForJob.example.Job.services.OrderService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final ProductionRecordService productionRecordService;
    private final OrderService orderService;

    // ===== Авторизованная зона =====

    @GetMapping("/layout")
    public String layout(Model model, HttpServletRequest request) {
        populateDashboard(model, request);
        return "welcomeToHome/layout";
    }

    @GetMapping("/fragments/home")
    public String homeFragment(Model model, HttpServletRequest request) {
        populateDashboard(model, request);
        return "fragments/home";
    }

    @GetMapping("/home")
    public String home() {
        return "redirect:/layout";
    }

    // ===== Публичная зона =====

    @GetMapping("/")
    public String root(@AuthenticationPrincipal UserDetails user) {
        return (user != null) ? "redirect:/layout" : "welcomeToHome/welcome";
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "welcomeToHome/welcome";
    }

    // ===== Общая логика наполнения модели =====

    private void populateDashboard(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());

        LocalDate today = LocalDate.now();
        model.addAttribute("todayDate", today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        // Последние 5 записей
        List<ProductionRecordEntity> lastRecords = productionRecordService.findLast5Records();
        List<Map<String, Object>> recordsWithProducts = new ArrayList<>();
        for (ProductionRecordEntity record : lastRecords) {
            Map<String, Object> item = new HashMap<>();
            item.put("record", record);
            if (record.getCoupling() != null) {
                item.put("product", record.getCoupling());
                item.put("productType", "COUPLING");
            } else if (record.getAdapter() != null) {
                item.put("product", record.getAdapter());
                item.put("productType", "ADAPTER");
            }
            recordsWithProducts.add(item);
        }
        model.addAttribute("recordsWithProducts", recordsWithProducts);

        // Срочные заказы
        List<OrderEntity> urgentOrderEntities = orderService.getUrgentProductionOrderEntities(8);
        List<HomeOrderDto> urgentOrders = new ArrayList<>();
        List<Map<String, Object>> ordersWithProducts = new ArrayList<>();

        for (OrderEntity order : urgentOrderEntities) {
            HomeOrderDto dto = new HomeOrderDto();
            dto.setId(order.getId());
            dto.setCompanyName(order.getCompany().getName());
            dto.setProductType(order.getProductType());
            dto.setProductName(order.getProductName());
            dto.setQuantity(order.getQuantity());

            long days = ChronoUnit.DAYS.between(today, order.getDeadline());
            dto.setDaysUntilDeadline(days);

            if (order.getCoupling() != null) {
                dto.setProductId(order.getCoupling().getId());
                dto.setProductType("COUPLING");
            } else if (order.getAdapter() != null) {
                dto.setProductId(order.getAdapter().getId());
                dto.setProductType("ADAPTER");
            }

            if (days < 5)       dto.setUrgencyClass("bg-danger text-white");
            else if (days < 14) dto.setUrgencyClass("bg-warning");
            else                dto.setUrgencyClass("bg-light");
            urgentOrders.add(dto);

            Map<String, Object> item = new HashMap<>();
            item.put("order", dto);
            if ("COUPLING".equals(dto.getProductType()) && order.getCoupling() != null) {
                item.put("product", order.getCoupling());
                item.put("productType", "COUPLING");
            } else if ("ADAPTER".equals(dto.getProductType()) && order.getAdapter() != null) {
                item.put("product", order.getAdapter());
                item.put("productType", "ADAPTER");
            }
            ordersWithProducts.add(item);
        }

        model.addAttribute("urgentOrders", urgentOrders);
        model.addAttribute("ordersWithProducts", ordersWithProducts);
    }
}