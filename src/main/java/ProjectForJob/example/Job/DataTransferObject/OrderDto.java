package ProjectForJob.example.Job.DataTransferObject;


import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // Трубы и патрубки и резьбы к ним
    private Long externalThreadId;
    private String externalThreadName;           // "НКТ 73"
    private String pipeProductKind;
    private Double wallThicknessMm;
    private Double averagePipeLengthM;
    private Double orderTotalLengthMeters;
    private Double orderTotalWeightKg;
    private BigDecimal pricePerTon;
    private Double pieceLengthMm;

    private String externalThreadDiameter;       // "73" — для тултипа
    private BigDecimal externalThreadManufacturingCost;  // цена 1 нарезки — для тултипа



    // === Группа прочности и марка стали ===
    private Long strengthGroupId;
    private String strengthGroupCode;
    private String strengthGroupName;
    private Long steelGradeId;
    private String steelGradeName;
    private BigDecimal appliedMarkupPercent;

    public String getWorkpieceDescription() {
        if (workpieceOuterDiameter == null || workpieceWallThickness == null
                || workpieceLengthMeters == null || workpieceWeightKg == null) {
            return "";
        }
        return String.format("%.0f*%.0f %.1f м (%.2f кг)",
                workpieceOuterDiameter, workpieceWallThickness,
                workpieceLengthMeters, workpieceWeightKg);
    }

    public Long getDaysUntilDeadline() {
        if (deadline == null) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }
}