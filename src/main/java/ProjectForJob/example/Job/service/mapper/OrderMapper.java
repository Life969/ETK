package ProjectForJob.example.Job.service.mapper;

import ProjectForJob.example.Job.dto.HomeOrderDto;
import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.dto.kafka.AdditionalWorkDto;
import ProjectForJob.example.Job.dto.kafka.OrderStartedEvent;
import ProjectForJob.example.Job.domain.entity.order.OrderEntity;
import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    // Основной метод для списка заказов
    public OrderDto toDto(OrderEntity order) {
        ProductInfo info = extractProductInfo(order);
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCompanyName(order.getCompany().getName());
        dto.setProductType(info.productType);
        dto.setProductName(info.productName);
        dto.setProductId(info.productId);
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setStatus(order.getStatus());
        dto.setTotalCost(order.getTotalCost());
        dto.setAdditionalWorkNames(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getName).collect(Collectors.toList()));
        dto.setAdditionalWorkIds(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getId).collect(Collectors.toList()));

        dto.setWorkpieceOuterDiameter(order.getWorkpieceOuterDiameter());
        dto.setWorkpieceWallThickness(order.getWorkpieceWallThickness());
        dto.setWorkpieceLengthMeters(order.getWorkpieceLengthMeters());
        dto.setWorkpieceWeightKg(order.getWorkpieceWeightKg());

        // === Расширенные данные для tooltip ===
        if (order.getCoupling() != null) {
            CouplingEntity c = order.getCoupling();
            dto.setProductVariantType(c.getType());
            dto.setProductVariantDiameter(c.getConditionalDiameter());
            dto.setProductLengthMm(c.getLengthMm());
            dto.setProductWeightKg(c.getWeightKg());
            dto.setProductOuterDiameterMm(c.getOuterDiameterMm());
            dto.setProductInnerDiameterMm(c.getInnerDiameterMm());
            dto.setProductImagePath(c.getImagePath());
            dto.setProductDescription(c.getDescription());
            dto.setProductManufacturingCost(c.getManufacturingCost());
            dto.setProductPriceForEmployee(c.getPriceForEmployee());
        } else if (order.getAdapter() != null) {
            PipeAdapterEntity a = order.getAdapter();
            dto.setProductAdapterFirstSideType(a.getFirstSideType());
            dto.setProductAdapterFirstSideDiameter(a.getFirstSideDiameter());
            dto.setProductAdapterSecondSideType(a.getSecondSideType());
            dto.setProductAdapterSecondSideDiameter(a.getSecondSideDiameter());
            dto.setProductLengthMm(a.getLengthMm());
            dto.setProductWeightKg(a.getWeightKg());
            dto.setProductOuterDiameterMm(a.getOuterDiameterMm());
            dto.setProductInnerDiameterMm(a.getInnerDiameterMm());
            dto.setProductImagePath(a.getImagePath());
            dto.setProductDescription(a.getDescription());
            dto.setProductManufacturingCost(a.getManufacturingCost());
            dto.setProductPriceForEmployee(a.getPriceForEmployee());
        }

        // === Трубы / патрубки ===
        if (order.getExternalThread() != null) {
            ExternalThreadEntity t = order.getExternalThread();

            dto.setExternalThreadId(t.getId());
            dto.setExternalThreadPriceForEmployee(t.getPriceForEmployee());
            dto.setExternalThreadName(t.getName());
            dto.setExternalThreadDiameter(t.getConditionalDiameter());
            dto.setExternalThreadManufacturingCost(t.getManufacturingCost());
            dto.setExternalThreadImagePath(t.getImagePath());
        }
        if (order.getPipeProductKind() != null) {
            dto.setPipeProductKind(order.getPipeProductKind().name());
        }
        dto.setWallThicknessMm(order.getWallThicknessMm());
        dto.setAveragePipeLengthM(order.getAveragePipeLengthM());
        dto.setOrderTotalLengthMeters(order.getOrderTotalLengthMeters());
        dto.setOrderTotalWeightKg(order.getOrderTotalWeightKg());
        dto.setPricePerTon(order.getPricePerTon());
        dto.setPieceLengthMm(order.getPieceLengthMm());

        // === Группа прочности и марка стали ===
        if (order.getStrengthGroup() != null) {
            dto.setStrengthGroupId(order.getStrengthGroup().getId());
            dto.setStrengthGroupCode(order.getStrengthGroup().getCode());
            dto.setStrengthGroupName(order.getStrengthGroup().getName());
        }
        if (order.getSteelGrade() != null) {
            dto.setSteelGradeId(order.getSteelGrade().getId());
            dto.setSteelGradeName(order.getSteelGrade().getName());
        }
        dto.setAppliedMarkupPercent(order.getAppliedMarkupPercent());

        return dto;
    }

    // Для домашней страницы "срочные заказы"
    public HomeOrderDto toHomeOrderDto(OrderEntity order, LocalDate today) {
        HomeOrderDto dto = new HomeOrderDto();
        dto.setId(order.getId());
        dto.setCompanyName(order.getCompany().getName());

        ProductInfo info = extractProductInfo(order);
        dto.setProductType(info.productType);
        dto.setProductName(info.productName);
        dto.setProductId(info.productId);
        dto.setQuantity(order.getQuantity());

        long days = order.getDeadline() != null ?
                ChronoUnit.DAYS.between(today, order.getDeadline()) : 0;
        dto.setDaysUntilDeadline(days);

        String urgencyClass;
        if (days <= 3) {
            urgencyClass = "bg-danger text-white";
        } else if (days <= 7) {
            urgencyClass = "bg-warning";
        } else {
            urgencyClass = "bg-light";
        }
        dto.setUrgencyClass(urgencyClass);
        return dto;
    }

    // Для Kafka-события при старте производства
    public OrderStartedEvent toStartedEvent(OrderEntity order) {
        ProductInfo info = extractProductInfo(order);
        List<AdditionalWorkDto> works = order.getAdditionalWorks().stream()
                .map(w -> new AdditionalWorkDto(w.getId(), w.getName(), w.getPrice()))
                .collect(Collectors.toList());

        return OrderStartedEvent.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .companyName(order.getCompany().getName())
                .productType(info.productType)
                .productName(info.productName)
                .productDetails(info.productDetails)
                .unitManufacturingCost(info.unitCost)
                .weightKg(info.weightKg)
                .lengthMm(info.lengthMm)
                .imagePath(info.imagePath)
                .quantity(order.getQuantity())
                .deadline(order.getDeadline())
                .additionalWorks(works)
                .totalCost(order.getTotalCost())
                .build();
    }

    // Выделенный метод, чтобы не дублировать проверки на тип продукта
    private ProductInfo extractProductInfo(OrderEntity order) {
        ProductInfo info = new ProductInfo();
        info.productType = order.getProductType();   // COUPLING / ADAPTER / null
        info.productName = order.getProductName();   // "" для труб/патрубков

        if (order.getCoupling() != null) {
            CouplingEntity c = order.getCoupling();
            info.productId = c.getId();
            info.productDetails = c.getType() + " " + c.getConditionalDiameter();
            info.unitCost = c.getManufacturingCost();
            info.weightKg = c.getWeightKg() != null ? BigDecimal.valueOf(c.getWeightKg()) : BigDecimal.ZERO;
            info.lengthMm = c.getLengthMm();
            info.imagePath = c.getImagePath();

        } else if (order.getAdapter() != null) {
            PipeAdapterEntity a = order.getAdapter();
            info.productId = a.getId();
            info.productDetails = a.getFullName();
            info.unitCost = a.getManufacturingCost();
            info.weightKg = a.getWeightKg() != null ? BigDecimal.valueOf(a.getWeightKg()) : BigDecimal.ZERO;
            info.lengthMm = a.getLengthMm();
            info.imagePath = a.getImagePath();

        } else if (order.getExternalThread() != null) {
            ExternalThreadEntity t = order.getExternalThread();
            info.productType = order.getPipeProductKind() != null
                    ? order.getPipeProductKind().name()
                    : "PIPE";
            // productName остаётся из order.getProductName() — "Труба НКТ 73" / "Патрубок НКТ 73 L-2000"
            info.productId = t.getId();
            info.productDetails = t.getName();
            info.unitCost = t.getManufacturingCost();
            info.weightKg = order.getOrderTotalWeightKg() != null
                    ? BigDecimal.valueOf(order.getOrderTotalWeightKg())
                    : BigDecimal.ZERO;
            info.lengthMm = null;
            info.imagePath = t.getImagePath();

        } else {
            // нет продукта
            info.productId = null;
            info.productDetails = "";
            info.unitCost = BigDecimal.ZERO;
            info.weightKg = BigDecimal.ZERO;
            info.lengthMm = null;
            info.imagePath = null;
        }
        return info;
    }

    private static class ProductInfo {
        String productType;
        String productName;
        Long productId;
        String productDetails;
        BigDecimal unitCost;
        BigDecimal weightKg;
        Double lengthMm;
        String imagePath;
    }
}