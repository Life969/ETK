package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.dto.CostBreakdownDto;
import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.service.handbook.AdditionalWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Расчёт расшифровки стоимости заказа (для view.html и commercial-offer.html).
 * Формулы 1-в-1 совпадают с фронтовым live-калькулятором.
 */
@Service
@RequiredArgsConstructor
public class CostBreakdownService {

    private final AdditionalWorkService additionalWorkService;

    public CostBreakdownDto buildBreakdown(OrderDto order) {
        CostBreakdownDto b = new CostBreakdownDto();
        b.setProductType(order.getProductType());
        b.setQuantity(order.getQuantity());
        b.setTotalCost(order.getTotalCost());
        b.setStrengthGroupCode(order.getStrengthGroupCode());

        BigDecimal qty = BigDecimal.valueOf(order.getQuantity() != null ? order.getQuantity() : 0);
        BigDecimal markupPct = order.getAppliedMarkupPercent() != null
                ? order.getAppliedMarkupPercent() : BigDecimal.ZERO;
        b.setMarkupPercent(markupPct);

        String pt = order.getProductType();

        if ("COUPLING".equals(pt) || "ADAPTER".equals(pt)) {
            BigDecimal unit = order.getProductManufacturingCost() != null
                    ? order.getProductManufacturingCost() : BigDecimal.ZERO;
            b.setUnitCost(unit);
            b.setBaseCost(unit.multiply(qty));
            b.setTotalWeightKg(order.getWorkpieceWeightKg());
        } else if ("PIPE".equals(pt) || "NIPPLE".equals(pt)) {
            BigDecimal thread = order.getExternalThreadManufacturingCost() != null
                    ? order.getExternalThreadManufacturingCost() : BigDecimal.ZERO;
            b.setThreadCost(thread);
            BigDecimal threadsCost = thread.multiply(BigDecimal.valueOf(2)).multiply(qty);
            b.setThreadsCost(threadsCost);

            BigDecimal ppt = order.getPricePerTon() != null
                    ? order.getPricePerTon() : BigDecimal.ZERO;
            b.setPricePerTon(ppt);
            double wKg = order.getOrderTotalWeightKg() != null ? order.getOrderTotalWeightKg() : 0;
            b.setTotalWeightKg(wKg);

            BigDecimal metalCost = ppt
                    .multiply(BigDecimal.valueOf(wKg))
                    .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            b.setMetalCost(metalCost);
            b.setBaseCost(threadsCost.add(metalCost));
        } else {
            b.setBaseCost(BigDecimal.ZERO);
        }

        BigDecimal markup = b.getBaseCost()
                .multiply(markupPct)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        b.setMarkupCost(markup);

        double weight = b.getTotalWeightKg() != null ? b.getTotalWeightKg() : 0;
        double billing = Math.max(weight, 100);
        b.setBillingWeight(billing);

        List<CostBreakdownDto.WorkLine> lines = new ArrayList<>();
        BigDecimal worksTotal = BigDecimal.ZERO;
        if (order.getAdditionalWorkIds() != null) {
            for (Long wid : order.getAdditionalWorkIds()) {
                AdditionalWorkEntity w = additionalWorkService.findById(wid);
                if (w == null || w.getPrice() == null) continue;
                BigDecimal cost = w.getPrice()
                        .multiply(BigDecimal.valueOf(billing))
                        .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                lines.add(new CostBreakdownDto.WorkLine(w.getName(), w.getPrice(), cost));
                worksTotal = worksTotal.add(cost);
            }
        }
        b.setWorks(lines);
        b.setWorksTotal(worksTotal);

        return b;
    }
}