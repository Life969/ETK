package ProjectForJob.example.Job.DataTransferObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRecordListDto {
    private Long id;
    private LocalDate productionDate;
    private String productName;
    private Integer quantity;
    private String employeeName;
    private String machineName;
    private LocalDateTime createdAt;
    private String comment;
    private String productType;   // COUPLING / ADAPTER / PIPE / NIPPLE
    private Double pieceLengthMm;

    /** Только для формы редактирования: id'шники, чтобы предзаполнить селекты. */
    private Long couplingId;
    private Long adapterId;
    private Long externalThreadId;
}