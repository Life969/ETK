package ProjectForJob.example.Job.DataTransferObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ProductionRecordReportDto {                // для отчетов
    private Long id;
    private LocalDate productionDate;
    private String productName;
    private Integer quantity;
    private String employeeName;
    private String machineName;
    private BigDecimal priceForEmployee;      // цена для сотрудника
    private BigDecimal manufacturingCost;     // себестоимость для станка
}