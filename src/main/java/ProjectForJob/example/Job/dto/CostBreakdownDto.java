package ProjectForJob.example.Job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostBreakdownDto {

    private String productType;
    private Integer quantity;
    private BigDecimal totalCost;

    // COUPLING / ADAPTER
    private BigDecimal unitCost;

    // PIPE / NIPPLE
    private BigDecimal threadCost;      // за 1 нарезку
    private BigDecimal threadsCost;     // Q × 2 × threadCost
    private BigDecimal pricePerTon;
    private Double totalWeightKg;
    private BigDecimal metalCost;

    // Общее
    private BigDecimal baseCost;
    private BigDecimal markupPercent;
    private String strengthGroupCode;
    private BigDecimal markupCost;

    // Доп.работы
    private Double billingWeight;       // max(weight, 100)
    private List<WorkLine> works;
    private BigDecimal worksTotal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkLine {
        private String name;
        private BigDecimal pricePerTon;
        private BigDecimal cost;        // pricePerTon × billingWeight / 1000
    }
}