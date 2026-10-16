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
    private String externalThreadName;
    private String pipeProductKind;
    private Double wallThicknessMm;
    private Double averagePipeLengthM;
    private Double orderTotalLengthMeters;
    private Double orderTotalWeightKg;
    private BigDecimal pricePerTon;
    private Double pieceLengthMm;

    private String externalThreadDiameter;
    private BigDecimal externalThreadManufacturingCost;
    private BigDecimal externalThreadPriceForEmployee;
    private String externalThreadImagePath;



    // === Группа прочности и марка стали ===
    private Long strengthGroupId;
    private String strengthGroupCode;
    private String strengthGroupName;
    private Long steelGradeId;
    private String steelGradeName;
    private BigDecimal appliedMarkupPercent;

    // === Расширенные данные для tooltip (из справочников продуктов) ===

    // Муфта / переводник — данные справочника
    private String productVariantType;
    private String productVariantDiameter;
    private Double productLengthMm;
    private Double productWeightKg;
    private Double productOuterDiameterMm;
    private Double productInnerDiameterMm;
    private String productImagePath;
    private String productDescription;
    private BigDecimal productManufacturingCost;
    private BigDecimal productPriceForEmployee;

    // Переводник — обе стороны
    private String productAdapterFirstSideType;
    private String productAdapterFirstSideDiameter;
    private String productAdapterSecondSideType;
    private String productAdapterSecondSideDiameter;



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