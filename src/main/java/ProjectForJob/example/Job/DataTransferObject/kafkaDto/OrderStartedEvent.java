package ProjectForJob.example.Job.DataTransferObject.kafkaDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStartedEvent {
    private Long orderId;
    private LocalDateTime createdAt;
    private String companyName;

    private String productType; // COUPLING, ADAPTER
    private String productName;
    private String productDetails; // собираем из атрибутов

    private BigDecimal unitManufacturingCost;
    private BigDecimal weightKg;
    private Double lengthMm;
    private String imagePath; // если нужно

    private Integer quantity;
    private LocalDate deadline;

    private List<AdditionalWorkDto> additionalWorks;

    private BigDecimal totalCost;

    // Можно добавить ещё поля, если понадобятся
}