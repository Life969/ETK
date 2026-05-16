package ProjectForJob.example.Job.services.mapping;

import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.kafkaDto.AdditionalWorkDto;
import ProjectForJob.example.Job.DataTransferObject.kafkaDto.OrderStartedEvent;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
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
        // Определяем тип и имя продукта (методы сущности)
        info.productType = order.getProductType();   // вернёт "COUPLING"/"ADAPTER"/null
        info.productName = order.getProductName();   // вернёт название или ""

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

    // Внутренний класс (или можно статический)
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