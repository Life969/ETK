package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Калькулятор стоимости заказа.
 *
 * ВАЖНО: этот класс — единственное место, где живёт формула расчёта totalCost.
 * При изменении бизнес-правил менять ТОЛЬКО здесь и обновлять тесты.
 *
 * ============================================================
 * ФОРМУЛА (Фаза 2, с группой прочности и весозависимыми доп. работами):
 * ------------------------------------------------------------
 *   baseCost     = P_unit × Q
 *   markupCost   = baseCost × (M_pct / 100)
 *   productTotal = baseCost + markupCost
 *
 *   W_billing    = max(W_total, 100)   // кг, минимум 100 кг
 *   worksCost    = Σ_i ( W_i × W_billing / 1000 )   // W_i — руб/тонну
 *
 *   totalCost    = productTotal + worksCost
 *                → округление до scale=2, HALF_UP
 * ------------------------------------------------------------
 * Где:
 *   P_unit    — цена изготовления 1 шт (manufacturingCost муфты/переводника)
 *   Q         — количество, шт
 *   M_pct     — snapshot надбавки за группу прочности (appliedMarkupPercent),
 *               0 если группа не выбрана
 *   W_total   — теоретический вес заготовки заказа, кг
 *   W_i       — цена i-й доп.работы ЗА ТОННУ продукции, руб
 * ============================================================
 */
public final class OrderCostCalculator {

    /** Все денежные величины в заказе хранятся в рублях с копейками (scale=2). */
    private static final int MONEY_SCALE = 2;

    /** Промежуточное округление — защита от накопления дробей. */
    private static final int INTERMEDIATE_SCALE = 4;

    /** Минимум, до которого «подтягивается» вес при расчёте доп. работ. */
    private static final BigDecimal MIN_BILLING_WEIGHT_KG = new BigDecimal("100");

    /** Для перевода кг → тонн в формуле доп. работ. */
    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    private OrderCostCalculator() {
    }

    /**
     * @param unitCost         цена изготовления одной штуки, руб
     * @param additionalWorks  доп. работы (их price — цена за ТОННУ, руб)
     * @param quantity         количество продукции, шт
     * @param markupPercent    snapshot надбавки за группу прочности в %, null == 0
     * @param workpieceWeightKg теоретический вес заготовки заказа, кг; null == 0
     *                          (но при расчёте поднимается до минимум 100 кг)
     * @return итоговая стоимость заказа, руб (scale=2)
     */
    public static BigDecimal calculate(BigDecimal unitCost,
                                       List<AdditionalWorkEntity> additionalWorks,
                                       int quantity,
                                       BigDecimal markupPercent,
                                       Double workpieceWeightKg) {

        // ---- защита от null ----
        if (unitCost == null) unitCost = BigDecimal.ZERO;
        if (markupPercent == null) markupPercent = BigDecimal.ZERO;

        BigDecimal qty = BigDecimal.valueOf(quantity);

        // 1) baseCost = P_unit * Q
        BigDecimal baseCost = unitCost.multiply(qty);

        // 2) markupCost = baseCost * M_pct / 100
        BigDecimal markupCost = baseCost
                .multiply(markupPercent)
                .divide(new BigDecimal("100"), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 3) productTotal = baseCost + markupCost
        BigDecimal productTotal = baseCost.add(markupCost);

        // 4) W_billing = max(W_total, 100)
        BigDecimal billingWeight = (workpieceWeightKg == null || workpieceWeightKg <= 0)
                ? MIN_BILLING_WEIGHT_KG
                : BigDecimal.valueOf(workpieceWeightKg).max(MIN_BILLING_WEIGHT_KG);

        // 5) worksCost = Σ_i (W_i * W_billing / 1000)
        BigDecimal worksPerTonSum = additionalWorks == null
                ? BigDecimal.ZERO
                : additionalWorks.stream()
                .map(AdditionalWorkEntity::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal worksCost = worksPerTonSum
                .multiply(billingWeight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 6) totalCost = productTotal + worksCost
        return productTotal.add(worksCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}