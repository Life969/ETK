package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Калькулятор стоимости заказа на трубу или патрубок.
 *
 * Отличие от OrderCostCalculator (муфты/переводники):
 *   базой служит НЕ unitCost × quantity, а сумма двух компонентов:
 *     threadsCost = Q × 2 × manufacturingCost  (нарезка с двух сторон)
 *     metalCost   = pricePerTon × weight_kg / 1000
 *
 * ВАЖНО: этот класс — единственное место, где живёт формула
 * для труб/патрубков. При изменении — обновлять тесты.
 *
 * ФОРМУЛА:
 *   Q            = quantity (штук)
 *   threadsCost  = Q × 2 × manufacturingCost
 *   metalCost    = pricePerTon × weight_kg / 1000
 *   baseCost     = threadsCost + metalCost
 *   markupCost   = baseCost × markup_pct / 100
 *   productTotal = baseCost + markupCost
 *   billingW     = max(weight_kg, 100)
 *   worksCost    = Σ W_i × billingW / 1000
 *   totalCost    = productTotal + worksCost
 *   → округление scale=2, HALF_UP
 */
public final class PipeOrderCostCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int INTERMEDIATE_SCALE = 4;
    private static final BigDecimal MIN_BILLING_WEIGHT_KG = new BigDecimal("100");
    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");
    private static final BigDecimal TWO = new BigDecimal("2");

    private PipeOrderCostCalculator() {}

    /**
     * @param threadManufacturingCost цена за 1 нарезку, руб (из справочника резьб)
     * @param pricePerTon             цена за 1 тонну металла, руб
     * @param quantity                количество штук (труб или патрубков)
     * @param weightKg                общий вес заказа, кг
     * @param additionalWorks         доп. работы (их price — за ТОННУ)
     * @param markupPercent           snapshot надбавки за группу, %
     */
    public static BigDecimal calculate(BigDecimal threadManufacturingCost,
                                       BigDecimal pricePerTon,
                                       int quantity,
                                       Double weightKg,
                                       List<AdditionalWorkEntity> additionalWorks,
                                       BigDecimal markupPercent) {

        if (threadManufacturingCost == null) threadManufacturingCost = BigDecimal.ZERO;
        if (pricePerTon == null) pricePerTon = BigDecimal.ZERO;
        if (markupPercent == null) markupPercent = BigDecimal.ZERO;

        BigDecimal weight = (weightKg == null || weightKg <= 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(weightKg);

        // 1) threadsCost = Q × 2 × manufacturingCost
        BigDecimal threadsCost = threadManufacturingCost
                .multiply(TWO)
                .multiply(BigDecimal.valueOf(quantity));

        // 2) metalCost = pricePerTon × weight_kg / 1000
        BigDecimal metalCost = pricePerTon
                .multiply(weight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 3) baseCost
        BigDecimal baseCost = threadsCost.add(metalCost);

        // 4) markupCost
        BigDecimal markupCost = baseCost
                .multiply(markupPercent)
                .divide(new BigDecimal("100"), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 5) productTotal
        BigDecimal productTotal = baseCost.add(markupCost);

        // 6) billingWeight = max(weight, 100)
        BigDecimal billingWeight = weight.max(MIN_BILLING_WEIGHT_KG);

        // 7) worksCost = Σ W_i × billingWeight / 1000
        BigDecimal worksPerTonSum = additionalWorks == null
                ? BigDecimal.ZERO
                : additionalWorks.stream()
                .map(AdditionalWorkEntity::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal worksCost = worksPerTonSum
                .multiply(billingWeight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 8) totalCost
        return productTotal.add(worksCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}