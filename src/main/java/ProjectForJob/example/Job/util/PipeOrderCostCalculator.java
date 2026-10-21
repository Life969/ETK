package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.exception.util.CalculatorErrors;
import ProjectForJob.example.Job.exception.util.CalculatorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Калькулятор стоимости заказа на трубу или патрубок.
 * <p>
 * Отличие от OrderCostCalculator (муфты/переводники):
 * базой служит НЕ unitCost × quantity, а сумма двух компонентов:
 * threadsCost = Q × 2 × manufacturingCost  (нарезка с двух сторон)
 * metalCost   = pricePerTon × weight_kg / 1000
 * <p>
 * ВАЖНО: этот класс — единственное место, где живёт формула
 * для труб/патрубков. При изменении — обновлять тесты.
 * <p>
 * ФОРМУЛА:
 * Q            = quantity (штук, должно быть > 0)
 * threadsCost  = Q × 2 × manufacturingCost
 * metalCost    = pricePerTon × weight_kg / 1000
 * baseCost     = threadsCost + metalCost
 * markupCost   = baseCost × markup_pct / 100
 * productTotal = baseCost + markupCost
 * billingW     = max(weight_kg, 100)   ← ТОЛЬКО для доп. работ
 * worksCost    = Σ W_i × billingW / 1000
 * totalCost    = productTotal + worksCost
 * → округление scale=2, HALF_UP
 * <p>
 * ВАЖНО: вес для metalCost и для worksCost разный:
 * - metalCost  считается от РЕАЛЬНОГО веса (0 → 0);
 * - worksCost  — от max(вес, 100 кг) — минимальная тарификация работ.
 */
public final class PipeOrderCostCalculator {

    private static final int MONEY_SCALE = 2;

    /**
     * Промежуточное округление — защита от накопления дробей.
     * Должно быть строго больше MONEY_SCALE.
     */
    private static final int INTERMEDIATE_SCALE = 4;

    private static final BigDecimal MIN_BILLING_WEIGHT_KG = new BigDecimal("100");
    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");
    private static final BigDecimal PERCENT_BASE = new BigDecimal("100");

    /**
     * Нарезка с двух сторон.
     */
    private static final BigDecimal THREADS_PER_PIECE = new BigDecimal("2");

    private PipeOrderCostCalculator() {
    }

    /**
     * @param threadManufacturingCost цена за 1 нарезку, руб (из справочника резьб); null == 0
     * @param pricePerTon             цена за 1 тонну металла, руб; null == 0
     * @param quantity                количество штук (труб или патрубков); должно быть > 0
     * @param weightKg                общий вес заказа, кг; null / <= 0 → 0 для металла,
     *                                но 100 для доп. работ
     * @param additionalWorks         доп. работы (их price — за ТОННУ)
     * @param markupPercent           snapshot надбавки за группу, %; null == 0
     * @return итоговая стоимость заказа, руб (scale=2)
     * @throws CalculatorException если quantity <= 0
     */
    public static BigDecimal calculate(BigDecimal threadManufacturingCost,
                                       BigDecimal pricePerTon,
                                       int quantity,
                                       Double weightKg,
                                       List<AdditionalWorkEntity> additionalWorks,
                                       BigDecimal markupPercent) {

        if (quantity <= 0) {
            throw CalculatorErrors.quantityNotPositive(quantity);
        }

        BigDecimal safeThreadCost = threadManufacturingCost != null
                ? threadManufacturingCost : BigDecimal.ZERO;
        BigDecimal safePricePerTon = pricePerTon != null
                ? pricePerTon : BigDecimal.ZERO;
        BigDecimal safeMarkup = markupPercent != null
                ? markupPercent : BigDecimal.ZERO;

        // Вес для металла — как есть; вес для работ — с минимумом 100 кг.
        BigDecimal metalWeight = toWeightOrZero(weightKg);
        BigDecimal billingWeight = metalWeight.max(MIN_BILLING_WEIGHT_KG);

        // 1) threadsCost = Q × 2 × manufacturingCost
        BigDecimal threadsCost = safeThreadCost
                .multiply(THREADS_PER_PIECE)
                .multiply(BigDecimal.valueOf(quantity));

        // 2) metalCost = pricePerTon × weight_kg / 1000
        BigDecimal metalCost = safePricePerTon
                .multiply(metalWeight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 3) baseCost
        BigDecimal baseCost = threadsCost.add(metalCost);

        // 4) markupCost = baseCost × markup / 100
        BigDecimal markupCost = baseCost
                .multiply(safeMarkup)
                .divide(PERCENT_BASE, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 5) productTotal = baseCost + markupCost
        BigDecimal productTotal = baseCost.add(markupCost);

        // 6) worksCost = Σ W_i × billingWeight / 1000
        BigDecimal worksPerTonSum = sumWorksPerTon(additionalWorks);
        BigDecimal worksCost = worksPerTonSum
                .multiply(billingWeight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 7) totalCost
        return productTotal.add(worksCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * null / неположительный вес → 0 (для металла; для работ отдельно max с 100).
     */
    private static BigDecimal toWeightOrZero(Double weightKg) {
        if (weightKg == null || weightKg <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(weightKg);
    }

    /**
     * Сумма цен доп. работ за тонну; null-список/элементы/цены игнорируются.
     */
    private static BigDecimal sumWorksPerTon(List<AdditionalWorkEntity> additionalWorks) {
        if (additionalWorks == null || additionalWorks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return additionalWorks.stream()
                .filter(Objects::nonNull)
                .map(AdditionalWorkEntity::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}