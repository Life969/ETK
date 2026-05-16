package ProjectForJob.example.Job.DataTransferObject;


import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
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
    private String productType;
    private String productName;
    private Long productId;
    private Integer quantity;
    private LocalDate deadline;
    private List<String> additionalWorkNames;
    private OrderStatus status;
    private BigDecimal totalCost;
    private Long couplingId;
    private List<Long> additionalWorkIds;

    private Double workpieceOuterDiameter;
    private Double workpieceWallThickness;
    private Double workpieceLengthMeters;
    private Double workpieceWeightKg;

    public String getWorkpieceDescription() {
        if (workpieceOuterDiameter == null || workpieceWallThickness == null
                || workpieceLengthMeters == null || workpieceWeightKg == null) {
            return "";
        }
        return String.format("%.0f*%.0f %.1f м (%.2f кг)",
                workpieceOuterDiameter, workpieceWallThickness,
                workpieceLengthMeters, workpieceWeightKg);
    }
}