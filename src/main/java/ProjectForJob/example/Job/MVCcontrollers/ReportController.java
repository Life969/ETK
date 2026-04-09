package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.MachinesEntity;
import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.services.EmployeesService;
import ProjectForJob.example.Job.services.MachinesService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ReportController {

    private final ProductionRecordService recordService;
    private final EmployeesService employeeService;
    private final MachinesService machinesService;

    @GetMapping("/employee")
    public String employeeReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        log.info("ReportController employeeReport employeeId={}, start={}, end={}", employeeId, startDate, endDate);

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

    @GetMapping("/machine")
    public String machineReport(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        log.info("ReportController machineReport machineId={}, start={}, end={}", machineId, startDate, endDate);

        if (startDate == null) {
            startDate = LocalDate.of(2025, 1, 1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<MachinesEntity> machines = machinesService.findAll();
        model.addAttribute("machines", machines);

        if (machineId != null) {
            MachinesEntity selectedMachine = machinesService.findById(machineId);
            model.addAttribute("selectedMachine", selectedMachine);

            List<ProductionRecordEntity> records = recordService.findAllByFilters(
                    null, null, machineId, startDate, endDate, Sort.by(Sort.Direction.ASC, "productionDate"));

            BigDecimal total = BigDecimal.ZERO;
            for (ProductionRecordEntity rec : records) {
                BigDecimal price = rec.getCoupling().getManufacturingCost();
                BigDecimal sum = price.multiply(BigDecimal.valueOf(rec.getQuantity()));
                total = total.add(sum);
            }

            model.addAttribute("records", records);
            model.addAttribute("totalEarned", total);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        }

        return "reports/machine";
    }
}