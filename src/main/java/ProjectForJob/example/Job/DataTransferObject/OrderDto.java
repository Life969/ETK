package ProjectForJob.example.Job.DataTransferObject;


import ProjectForJob.example.Job.entityJob.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private LocalDateTime createdAt;
    private String companyName;
    private String productName;
    private Integer quantity;
    private LocalDate deadline;
    private List<String> additionalWorkNames;
    private OrderStatus status;
    private BigDecimal totalCost;
}