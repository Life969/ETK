package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.Handbook.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.Handbook.MachinesEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.ProductionRecordEntity;
import ProjectForJob.example.Job.services.Handbook.EmployeesService;
import ProjectForJob.example.Job.services.Handbook.MachinesService;
import ProjectForJob.example.Job.services.ProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;   // ← НОВОЕ
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

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_EMPLOYEE = "fragments/reports/employee :: content";
    private static final String FRAGMENT_MACHINE = "fragments/reports/machine :: content";

    // ============================================================
    // ОТЧЁТ ПО СОТРУДНИКУ
    // ============================================================
    @GetMapping("/employee")
    @Transactional(readOnly = true)                                // ← НОВОЕ
    public String employeeReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        log.info("ReportController employeeReport employeeId={}, start={}, end={}",
                employeeId, startDate, endDate);

        // ← даты по умолчанию: с 1-го числа текущего месяца по сегодня
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        List<EmployeesEntity> employees = employeeService.findAll();
        model.addAttribute("employees", employees);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (employeeId != null) {
            EmployeesEntity selectedEmployee = employeeService.findById(employeeId);
            model.addAttribute("selectedEmployee", selectedEmployee);

            List<ProductionRecordEntity> records = recordService.findAllByFilters(
                    null, employeeId, null, startDate, endDate,
                    Sort.by(Sort.Direction.ASC, "productionDate"));

            BigDecimal totalEarned = BigDecimal.ZERO;
            int totalQuantity = 0;
            for (ProductionRecordEntity r : records) {
                BigDecimal price = r.getUnitPriceForEmployee() != null
                        ? r.getUnitPriceForEmployee() : BigDecimal.ZERO;
                int q = r.getQuantity() != null ? r.getQuantity() : 0;
                totalEarned = totalEarned.add(price.multiply(BigDecimal.valueOf(q)));
                totalQuantity += q;
            }

            model.addAttribute("records", records);
            model.addAttribute("totalEarned", totalEarned);
            model.addAttribute("totalRecords", records.size());
            model.addAttribute("totalQuantity", totalQuantity);
            model.addAttribute("selectedEmployeeId", employeeId);
        }

        model.addAttribute("contentTemplate", FRAGMENT_EMPLOYEE);
        return LAYOUT;
    }

    // ============================================================
    // ОТЧЁТ ПО СТАНКУ
    // ============================================================
    @GetMapping("/machine")
    @Transactional(readOnly = true)                                // ← НОВОЕ
    public String machineReport(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        log.info("ReportController machineReport machineId={}, start={}, end={}",
                machineId, startDate, endDate);

        // ← даты по умолчанию: с 1-го числа текущего месяца по сегодня
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        List<MachinesEntity> machines = machinesService.findAll();
        model.addAttribute("machines", machines);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (machineId != null) {
            MachinesEntity selectedMachine = machinesService.findById(machineId);
            model.addAttribute("selectedMachine", selectedMachine);

            List<ProductionRecordEntity> records = recordService.findAllByFilters(
                    null, null, machineId, startDate, endDate,
                    Sort.by(Sort.Direction.ASC, "productionDate"));

            BigDecimal totalCost = BigDecimal.ZERO;
            int totalQuantity = 0;
            for (ProductionRecordEntity r : records) {
                BigDecimal price = r.getUnitManufacturingCost() != null
                        ? r.getUnitManufacturingCost() : BigDecimal.ZERO;
                int q = r.getQuantity() != null ? r.getQuantity() : 0;
                totalCost = totalCost.add(price.multiply(BigDecimal.valueOf(q)));
                totalQuantity += q;
            }

            model.addAttribute("records", records);
            model.addAttribute("totalEarned", totalCost);
            model.addAttribute("totalRecords", records.size());
            model.addAttribute("totalQuantity", totalQuantity);
            model.addAttribute("selectedMachineId", machineId);
        }

        model.addAttribute("contentTemplate", FRAGMENT_MACHINE);
        return LAYOUT;
    }
}