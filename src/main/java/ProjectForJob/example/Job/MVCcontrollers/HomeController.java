package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.OrderService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductionRecordService productionRecordService;
    private final OrderService orderService;

    @GetMapping({"/", "/home"})
    public String home(Model model, HttpServletRequest request) {
        // Текущий URI для подсветки активного пункта меню
        model.addAttribute("currentUri", request.getRequestURI());

        // Текущая дата
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        model.addAttribute("todayDate", today.format(formatter));

        // Последние 5 записей из журнала производства
        List<ProductionRecordEntity> lastRecords = productionRecordService.findLast5Records();
        model.addAttribute("lastRecords", lastRecords);

        // Срочные заказы в производстве (максимум 8 штук)
        List<HomeOrderDto> urgentOrders = orderService.getUrgentProductionOrders(8);
        model.addAttribute("urgentOrders", urgentOrders);

        return "home";
    }
}