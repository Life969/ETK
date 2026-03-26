package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.EmployeesService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ProductionRecordService recordService;
    private final EmployeesService employeeService;

    @GetMapping("/employee")
    public String employeeReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        log.info("GET /reports/employee: employeeId={}, start={}, end={}", employeeId, startDate, endDate);

        // Если даты не указаны, задаём разумные значения
        if (startDate == null) {
            startDate = LocalDate.of(2025, 1, 1); // или можно использовать год назад
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Получаем всех сотрудников для выпадающего списка
        List<EmployeesEntity> employees = employeeService.findAll();
        model.addAttribute("employees", employees);

        if (employeeId != null) {
            // Находим выбранного сотрудника
            EmployeesEntity selectedEmployee = employeeService.findById(employeeId);
            model.addAttribute("selectedEmployee", selectedEmployee);


            List<ProductionRecordEntity> records = recordService.findAllByFilters(
                    null, employeeId, null, startDate, endDate, Sort.by(Sort.Direction.ASC, "productionDate"));

            // Рассчитываем суммы и заполняем модель
            BigDecimal total = BigDecimal.ZERO;
            for (ProductionRecordEntity record : records) {
                BigDecimal price = record.getCoupling().getPriceForEmployee();
                BigDecimal sum = price.multiply(BigDecimal.valueOf(record.getQuantity()));
                total = total.add(sum);
                // Можно добавить вычисленную сумму в сам объект или передавать отдельно
            }

            model.addAttribute("records", records);
            model.addAttribute("totalEarned", total);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        }

        return "reports/employee";
    }
}