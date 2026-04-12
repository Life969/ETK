package ProjectForJob.example.Job.DataTransferObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductionRecordListDto {                      //для журнала
    private Long id;
    private LocalDate productionDate;
    private String productName;        // название муфты или переводника
    private Integer quantity;
    private String employeeName;
    private String machineName;
    private LocalDateTime createdAt;
    private String comment;
}