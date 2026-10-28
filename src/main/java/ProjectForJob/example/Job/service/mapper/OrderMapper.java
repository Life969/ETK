package ProjectForJob.example.Job.service.mapper;

import ProjectForJob.example.Job.dto.HomeOrderDto;
import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.dto.kafka.AdditionalWorkDto;
import ProjectForJob.example.Job.dto.kafka.OrderStartedEvent;
import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    // ================================================================
    // toDto
    // ================================================================

    public OrderDto toDto(OrderEntity order) {
        OrderDto dto = new OrderDto();
        ProductInfo info = extractProductInfo(order);

        fillBase(dto, order, info);
        fillAdditionalWorks(dto, order);
        fillWorkpiece(dto, order);
        fillProduct(dto, order);
        fillStrength(dto, order);

        return dto;
    }

    private void fillBase(OrderDto dto, OrderEntity order, ProductInfo info) {
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCompanyName(requireCompanyName(order));
        dto.setProductType(info.productType());
        dto.setProductName(info.productName());
        dto.setProductId(info.productId());
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setStatus(order.getStatus());
        dto.setTotalCost(order.getTotalCost());
    }

    private void fillAdditionalWorks(OrderDto dto, OrderEntity order) {
        List<AdditionalWorkEntity> works = order.getAdditionalWorks() != null
                ? order.getAdditionalWorks()
                : List.of();
        dto.setAdditionalWorkNames(works.stream()
                .map(AdditionalWorkEntity::getName).collect(Collectors.toList()));
        dto.setAdditionalWorkIds(works.stream()
                .map(AdditionalWorkEntity::getId).collect(Collectors.toList()));
    }

    private void fillWorkpiece(OrderDto dto, OrderEntity order) {
        dto.setWorkpieceOuterDiameter(order.getWorkpieceOuterDiameter());
        dto.setWorkpieceWallThickness(order.getWorkpieceWallThickness());
        dto.setWorkpieceLengthMeters(order.getWorkpieceLengthMeters());
        dto.setWorkpieceWeightKg(order.getWorkpieceWeightKg());
    }

    /**
     * Продуктовая развилка. Инвариант домена: ровно один из
     * coupling / adapter / thread задан (проверяется в
     * OrderService.applyProductAndRecalc при создании/редактировании).
     */
    private void fillProduct(OrderDto dto, OrderEntity order) {
        if (order.getCoupling() != null) {
            fillCoupling(dto, order.getCoupling());
        } else if (order.getAdapter() != null) {
            fillAdapter(dto, order.getAdapter());
        } else if (order.getExternalThread() != null) {
            fillThreadAndPipe(dto, order);
        }
    }

    private void fillCoupling(OrderDto dto, CouplingEntity c) {
        dto.setProductVariantType(c.getType());
        dto.setProductVariantDiameter(c.getConditionalDiameter());
        fillCommonProductAttributes(dto, new CommonProductAttributes(
                c.getLengthMm(), c.getWeightKg(),
                c.getOuterDiameterMm(), c.getInnerDiameterMm(),
                c.getImagePath(), c.getDescription(),
                c.getManufacturingCost(), c.getPriceForEmployee()));
    }

    private void fillAdapter(OrderDto dto, PipeAdapterEntity a) {
        dto.setProductAdapterFirstSideType(a.getFirstSideType());
        dto.setProductAdapterFirstSideDiameter(a.getFirstSideDiameter());
        dto.setProductAdapterSecondSideType(a.getSecondSideType());
        dto.setProductAdapterSecondSideDiameter(a.getSecondSideDiameter());
        fillCommonProductAttributes(dto, new CommonProductAttributes(
                a.getLengthMm(), a.getWeightKg(),
                a.getOuterDiameterMm(), a.getInnerDiameterMm(),
                a.getImagePath(), a.getDescription(),
                a.getManufacturingCost(), a.getPriceForEmployee()));
    }

    private void fillThreadAndPipe(OrderDto dto, OrderEntity order) {
        ExternalThreadEntity t = order.getExternalThread();
        dto.setExternalThreadId(t.getId());
        dto.setExternalThreadPriceForEmployee(t.getPriceForEmployee());
        dto.setExternalThreadName(t.getName());
        dto.setExternalThreadDiameter(t.getConditionalDiameter());
        dto.setExternalThreadManufacturingCost(t.getManufacturingCost());
        dto.setExternalThreadImagePath(t.getImagePath());

        if (order.getPipeProductKind() != null) {
            dto.setPipeProductKind(order.getPipeProductKind().name());
        }
        dto.setWallThicknessMm(order.getWallThicknessMm());
        dto.setAveragePipeLengthM(order.getAveragePipeLengthM());
        dto.setOrderTotalLengthMeters(order.getOrderTotalLengthMeters());
        dto.setOrderTotalWeightKg(order.getOrderTotalWeightKg());
        dto.setPricePerTon(order.getPricePerTon());
        dto.setPieceLengthMm(order.getPieceLengthMm());
    }

    private void fillCommonProductAttributes(OrderDto dto, CommonProductAttributes a) {
        dto.setProductLengthMm(a.lengthMm());
        dto.setProductWeightKg(a.weightKg());
        dto.setProductOuterDiameterMm(a.outerDiameterMm());
        dto.setProductInnerDiameterMm(a.innerDiameterMm());
        dto.setProductImagePath(a.imagePath());
        dto.setProductDescription(a.description());
        dto.setProductManufacturingCost(a.manufacturingCost());
        dto.setProductPriceForEmployee(a.priceForEmployee());
    }

    private void fillStrength(OrderDto dto, OrderEntity order) {
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
    }

    // ================================================================
    // Home DTO
    // ================================================================

    public HomeOrderDto toHomeOrderDto(OrderEntity order, LocalDate today) {
        ProductInfo info = extractProductInfo(order);

        HomeOrderDto dto = new HomeOrderDto();
        dto.setId(order.getId());
        dto.setCompanyName(requireCompanyName(order));
        dto.setProductType(info.productType());
        dto.setProductName(info.productName());
        dto.setProductId(info.productId());
        dto.setQuantity(order.getQuantity());

        long days = order.getDeadline() != null
                ? ChronoUnit.DAYS.between(today, order.getDeadline())
                : 0;
        dto.setDaysUntilDeadline(days);
        dto.setUrgencyClass(urgencyClassFor(days));
        return dto;
    }

    private static String urgencyClassFor(long days) {
        if (days <= 3) return "bg-danger text-white";
        if (days <= 7) return "bg-warning";
        return "bg-light";
    }

    // ================================================================
    // Kafka event
    // ================================================================

    public OrderStartedEvent toStartedEvent(OrderEntity order) {
        ProductInfo info = extractProductInfo(order);
        List<AdditionalWorkEntity> works = order.getAdditionalWorks() != null
                ? order.getAdditionalWorks()
                : List.of();
        List<AdditionalWorkDto> workDtos = works.stream()
                .map(w -> new AdditionalWorkDto(w.getId(), w.getName(), w.getPrice()))
                .collect(Collectors.toList());

        return OrderStartedEvent.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .companyName(requireCompanyName(order))
                .productType(info.productType())
                .productName(info.productName())
                .productDetails(info.productDetails())
                .unitManufacturingCost(info.unitCost())
                .weightKg(info.weightKg())
                .lengthMm(info.lengthMm())
                .imagePath(info.imagePath())
                .quantity(order.getQuantity())
                .deadline(order.getDeadline())
                .additionalWorks(workDtos)
                .totalCost(order.getTotalCost())
                .build();
    }

    // ================================================================
    // ProductInfo (record) + построение
    // ================================================================

    private record ProductInfo(
            String productType,
            String productName,
            Long productId,
            String productDetails,
            BigDecimal unitCost,
            BigDecimal weightKg,
            Double lengthMm,
            String imagePath) {
    }

    private record CommonProductAttributes(
            Double lengthMm, Double weightKg,
            Double outerDiameterMm, Double innerDiameterMm,
            String imagePath, String description,
            BigDecimal manufacturingCost, BigDecimal priceForEmployee) {
    }

    private ProductInfo extractProductInfo(OrderEntity order) {
        String productType = order.getProductType();
        String productName = order.getProductName();

        if (order.getCoupling() != null) {
            CouplingEntity c = order.getCoupling();
            return new ProductInfo(
                    productType, productName,
                    c.getId(),
                    c.getType() + " " + c.getConditionalDiameter(),
                    c.getManufacturingCost(),
                    toBigDecimal(c.getWeightKg()),
                    c.getLengthMm(),
                    c.getImagePath());
        }
        if (order.getAdapter() != null) {
            PipeAdapterEntity a = order.getAdapter();
            return new ProductInfo(
                    productType, productName,
                    a.getId(),
                    a.getFullName(),
                    a.getManufacturingCost(),
                    toBigDecimal(a.getWeightKg()),
                    a.getLengthMm(),
                    a.getImagePath());
        }
        if (order.getExternalThread() != null) {
            ExternalThreadEntity t = order.getExternalThread();
            return new ProductInfo(
                    order.getPipeProductKind() != null
                            ? order.getPipeProductKind().name()
                            : "PIPE",
                    productName,
                    t.getId(),
                    t.getName(),
                    t.getManufacturingCost(),
                    toBigDecimal(order.getOrderTotalWeightKg()),
                    null,
                    t.getImagePath());
        }
        return new ProductInfo(
                productType, productName,
                null, "",
                BigDecimal.ZERO, BigDecimal.ZERO,
                null, null);
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    // ================================================================
    // Company — data integrity guard (единый для всех методов)
    // ================================================================

    private static String requireCompanyName(OrderEntity order) {
        CompanyEntity company = Objects.requireNonNull(
                order.getCompany(),
                () -> "data integrity violation: order " + order.getId() + " has no company");
        return Objects.requireNonNull(
                company.getName(),
                () -> "data integrity violation: company " + company.getId() + " has no name");
    }
}