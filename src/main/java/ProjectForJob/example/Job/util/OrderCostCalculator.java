package ProjectForJob.example.Job.util;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.exception.util.CalculatorErrors;
import ProjectForJob.example.Job.exception.util.CalculatorException;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Калькулятор стоимости заказа.
 * <p>
 * ВАЖНО: этот класс — единственное место, где живёт формула расчёта totalCost.
 * При изменении бизнес-правил менять ТОЛЬКО здесь и обновлять тесты.
 * <p>
 * baseCost     = P_unit × Q
 * markupCost   = baseCost × (M_pct / 100)
 * productTotal = baseCost + markupCost
 * <p>
 * W_billing    = max(W_total, 100)   // кг, минимум 100 кг
 * worksCost    = Σ_i ( W_i × W_billing / 1000 )   // W_i — руб/тонну
 * <p>
 * totalCost    = productTotal + worksCost
 * → округление до scale=2, HALF_UP
 * <p>
 * Где:
 * P_unit    — цена изготовления 1 шт (manufacturingCost муфты/переводника)
 * Q         — количество, шт (должно быть > 0)
 * M_pct     — snapshot надбавки за группу прочности (appliedMarkupPercent),
 * 0 если группа не выбрана
 * W_total   — теоретический вес заготовки заказа, кг
 * W_i       — цена i-й доп.работы ЗА ТОННУ продукции, руб
 */
public final class OrderCostCalculator {

    /**
     * Все денежные величины в заказе хранятся в рублях с копейками (scale=2).
     */
    private static final int MONEY_SCALE = 2;

    /**
     * Промежуточное округление — защита от накопления дробей.
     * ВАЖНО: должно быть строго больше MONEY_SCALE, иначе потеряем точность
     * на промежуточном шаге при финальном округлении.
     */
    private static final int INTERMEDIATE_SCALE = 4;

    /**
     * Минимум, до которого «подтягивается» вес при расчёте доп. работ.
     */
    private static final BigDecimal MIN_BILLING_WEIGHT_KG = new BigDecimal("100");

    /**
     * Для перевода кг → тонн в формуле доп. работ.
     */
    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    /**
     * База процента: markupCost = baseCost × M_pct / PERCENT_BASE.
     */
    private static final BigDecimal PERCENT_BASE = new BigDecimal("100");

    private OrderCostCalculator() {
    }

    /**
     * @param unitCost          цена изготовления одной штуки, руб; null == 0
     * @param additionalWorks   доп. работы (их price — цена за ТОННУ, руб)
     * @param quantity          количество продукции, шт; должно быть > 0
     * @param markupPercent     snapshot надбавки за группу прочности в %, null == 0
     * @param workpieceWeightKg теоретический вес заготовки заказа, кг; null == 0
     *                          (но при расчёте поднимается до минимум 100 кг)
     * @return итоговая стоимость заказа, руб (scale=2)
     * @throws CalculatorException если quantity <= 0
     */
    public static BigDecimal calculate(BigDecimal unitCost,
                                       List<AdditionalWorkEntity> additionalWorks,
                                       int quantity,
                                       BigDecimal markupPercent,
                                       Double workpieceWeightKg) {

        if (quantity <= 0) {
            throw CalculatorErrors.quantityNotPositive(quantity);
        }

        BigDecimal safeUnitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        BigDecimal safeMarkup = markupPercent != null ? markupPercent : BigDecimal.ZERO;

        // 1) baseCost = P_unit * Q
        BigDecimal baseCost = safeUnitCost.multiply(BigDecimal.valueOf(quantity));

        // 2) markupCost = baseCost * M_pct / 100
        BigDecimal markupCost = baseCost
                .multiply(safeMarkup)
                .divide(PERCENT_BASE, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 3) productTotal = baseCost + markupCost
        BigDecimal productTotal = baseCost.add(markupCost);

        // 4) W_billing = max(W_total, 100)
        BigDecimal billingWeight = resolveBillingWeight(workpieceWeightKg);

        // 5) worksCost = Σ_i (W_i * W_billing / 1000)
        BigDecimal worksPerTonSum = sumWorksPerTon(additionalWorks);
        BigDecimal worksCost = worksPerTonSum
                .multiply(billingWeight)
                .divide(KG_PER_TON, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // 6) totalCost = productTotal + worksCost
        return productTotal.add(worksCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Бизнес-правило: вес для начисления доп. работ не может быть меньше 100 кг.
     * null / неположительный вес → 100 кг (нет данных о весе — платим минимум).
     */
    private static BigDecimal resolveBillingWeight(Double workpieceWeightKg) {
        if (workpieceWeightKg == null || workpieceWeightKg <= 0) {
            return MIN_BILLING_WEIGHT_KG;
        }
        return BigDecimal.valueOf(workpieceWeightKg).max(MIN_BILLING_WEIGHT_KG);
    }

    /**
     * Сумма цен доп. работ за тонну.
     * null-список, null-элементы и null-цены игнорируются.
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