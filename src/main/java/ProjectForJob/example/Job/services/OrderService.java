package ProjectForJob.example.Job.services;


import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.*;
import ProjectForJob.example.Job.repositories.AdditionalWorkRepository;
import ProjectForJob.example.Job.repositories.CompanyRepository;
import ProjectForJob.example.Job.repositories.CouplingRepository;
import ProjectForJob.example.Job.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final CouplingRepository couplingRepository;
    private final AdditionalWorkRepository additionalWorkRepository;

    @Transactional
    public OrderDto createOrder(OrderCreateDto dto) {
        log.info("Создание заказа: компания={}, муфта={}, кол-во={}",
                dto.getCompanyName(), dto.getCouplingId(), dto.getQuantity());
        CompanyEntity company = companyRepository.findByNameIgnoreCase(dto.getCompanyName())
                .orElseGet(() -> {
                    CompanyEntity newCompany = CompanyEntity.builder()
                            .name(dto.getCompanyName().trim())
                            .build();
                    return companyRepository.save(newCompany);
                });

        CouplingEntity coupling = couplingRepository.findById(dto.getCouplingId())
                .orElseThrow(() -> new RuntimeException("Продукция не найдена"));

        List<AdditionalWorkEntity> additionalWorks = dto.getAdditionalWorkIds() != null ?
                additionalWorkRepository.findAllById(dto.getAdditionalWorkIds()) : List.of();

        OrderEntity order = OrderEntity.builder()
                .createdAt(LocalDateTime.now())
                .company(company)
                .coupling(coupling)
                .quantity(dto.getQuantity())
                .deadline(dto.getDeadline())
                .additionalWorks(additionalWorks)
                .status(OrderStatus.WAITING)
                .build();

        // Расчёт стоимости
        order.setTotalCost(calculateTotalCost(order));

        OrderEntity saved = orderRepository.save(order);
        return mapToDto(saved);
    }

    private BigDecimal calculateTotalCost(OrderEntity order) {
        // Стоимость муфты * количество
        BigDecimal unitManufacturingCost = order.getCoupling().getManufacturingCost();

        // Сумма дополнительных работ на одну единицу продукции
        BigDecimal additionalCostPerUnit = order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Общая стоимость одной единицы (изготовление + доп. работы)
        BigDecimal totalCostPerUnit = unitManufacturingCost.add(additionalCostPerUnit);

        // Умножаем на количество
        return totalCostPerUnit.multiply(BigDecimal.valueOf(order.getQuantity()));
    }

    @Transactional
    public OrderDto updateStatus(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Можно добавить проверки допустимости перехода, но по заданию разрешено любое изменение
        order.setStatus(newStatus);
        return mapToDto(orderRepository.save(order));
    }

    public Page<OrderDto> getOrdersByStatus(OrderStatus status, String search, Pageable pageable) {
        Page<OrderEntity> page;
        if (search != null && !search.isBlank()) {
            page = orderRepository.findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    status, search, pageable);
        } else {
            page = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return page.map(this::mapToDto);
    }

    public OrderDto getOrderById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        return mapToDto(order);
    }

    private OrderDto mapToDto(OrderEntity order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCompanyName(order.getCompany().getName());
        dto.setCouplingId(order.getCoupling().getId()); //new
        dto.setProductName(order.getCoupling().getName());
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setStatus(order.getStatus());
        dto.setTotalCost(order.getTotalCost());
        dto.setAdditionalWorkNames(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getName)
                .collect(Collectors.toList()));
        dto.setAdditionalWorkIds(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getId)
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        // можно добавить проверку, что удалять можно только заказы в статусе WAITING, но по желанию
        orderRepository.delete(order);
    }

    @Transactional
    public OrderDto updateOrder(Long orderId, OrderUpdateDto dto) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Обновляем компанию
        CompanyEntity company = companyRepository.findByNameIgnoreCase(dto.getCompanyName())
                .orElseGet(() -> {
                    CompanyEntity newCompany = CompanyEntity.builder()
                            .name(dto.getCompanyName().trim())
                            .build();
                    return companyRepository.save(newCompany);
                });
        order.setCompany(company);

        // Обновляем продукцию
        CouplingEntity coupling = couplingRepository.findById(dto.getCouplingId())
                .orElseThrow(() -> new RuntimeException("Продукция не найдена"));
        order.setCoupling(coupling);

        // Обновляем количество
        order.setQuantity(dto.getQuantity());

        // Обновляем дедлайн
        order.setDeadline(dto.getDeadline());

        // Обновляем список доп. работ
        List<AdditionalWorkEntity> additionalWorks = dto.getAdditionalWorkIds() != null ?
                additionalWorkRepository.findAllById(dto.getAdditionalWorkIds()) : List.of();
        order.setAdditionalWorks(additionalWorks);

        // Пересчитываем стоимость
        order.setTotalCost(calculateTotalCost(order));

        OrderEntity saved = orderRepository.save(order);
        return mapToDto(saved);
    }
}