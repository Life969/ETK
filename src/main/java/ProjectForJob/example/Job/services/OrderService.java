package ProjectForJob.example.Job.services;


import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.ForOrders.CompanyEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.repositories.Handbook.AdditionalWorkRepository;
import ProjectForJob.example.Job.repositories.CompanyRepository;
import ProjectForJob.example.Job.repositories.Handbook.CouplingRepository;
import ProjectForJob.example.Job.repositories.Handbook.PipeAdapterRepository;
import ProjectForJob.example.Job.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final PipeAdapterRepository pipeAdapterRepository;

    @Transactional
    public OrderDto createOrder(OrderCreateDto dto) {
        CompanyEntity company = companyRepository.findByNameIgnoreCase(dto.getCompanyName())
                .orElseGet(() -> companyRepository.save(CompanyEntity.builder().name(dto.getCompanyName().trim()).build()));

        OrderEntity order = OrderEntity.builder()
                .createdAt(LocalDateTime.now())
                .company(company)
                .quantity(dto.getQuantity())
                .deadline(dto.getDeadline())
                .additionalWorks(dto.getAdditionalWorkIds() != null
                        ? additionalWorkRepository.findAllById(dto.getAdditionalWorkIds()) : List.of())
                .status(OrderStatus.WAITING)
                .build();

        if ("COUPLING".equals(dto.getProductType())) {
            CouplingEntity coupling = couplingRepository.findById(dto.getCouplingId())
                    .orElseThrow(() -> new IllegalArgumentException("Муфта не найдена"));
            order.setCoupling(coupling);
        } else if ("ADAPTER".equals(dto.getProductType())) {
            PipeAdapterEntity adapter = pipeAdapterRepository.findById(dto.getAdapterId())
                    .orElseThrow(() -> new IllegalArgumentException("Переводник не найден"));
            order.setAdapter(adapter);
        } else {
            throw new IllegalArgumentException("Неверный тип продукции");
        }

        order.setTotalCost(calculateTotalCost(order));
        return mapToDto(orderRepository.save(order));
    }

    private BigDecimal calculateTotalCost(OrderEntity order) {
        BigDecimal unitCost = order.getUnitManufacturingCost();
        BigDecimal additionalPerUnit = order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return unitCost.add(additionalPerUnit).multiply(BigDecimal.valueOf(order.getQuantity()));
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
        dto.setProductType(order.getProductType());
        dto.setProductName(order.getProductName());
        if (order.getCoupling() != null) {
            dto.setProductId(order.getCoupling().getId());
        } else if (order.getAdapter() != null) {
            dto.setProductId(order.getAdapter().getId());
        }
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setStatus(order.getStatus());
        dto.setTotalCost(order.getTotalCost());
        dto.setAdditionalWorkNames(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getName).collect(Collectors.toList()));
        dto.setAdditionalWorkIds(order.getAdditionalWorks().stream()
                .map(AdditionalWorkEntity::getId).collect(Collectors.toList()));
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

        // Компания
        CompanyEntity company = companyRepository.findByNameIgnoreCase(dto.getCompanyName())
                .orElseGet(() -> companyRepository.save(CompanyEntity.builder().name(dto.getCompanyName().trim()).build()));
        order.setCompany(company);

        // Продукт
        order.setCoupling(null);
        order.setAdapter(null);
        if ("COUPLING".equals(dto.getProductType())) {
            CouplingEntity coupling = couplingRepository.findById(dto.getCouplingId())
                    .orElseThrow(() -> new IllegalArgumentException("Муфта не найдена"));
            order.setCoupling(coupling);
        } else if ("ADAPTER".equals(dto.getProductType())) {
            PipeAdapterEntity adapter = pipeAdapterRepository.findById(dto.getAdapterId())
                    .orElseThrow(() -> new IllegalArgumentException("Переводник не найден"));
            order.setAdapter(adapter);
        }

        order.setQuantity(dto.getQuantity());
        order.setDeadline(dto.getDeadline());
        order.setAdditionalWorks(dto.getAdditionalWorkIds() != null
                ? additionalWorkRepository.findAllById(dto.getAdditionalWorkIds()) : List.of());
        order.setTotalCost(calculateTotalCost(order));

        return mapToDto(orderRepository.save(order));
    }

    public OrderEntity getOrderEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден, id=" + id));
    }

    public List<OrderEntity> getOrdersByIdsWithDetails(List<Long> ids) {
        return orderRepository.findByIdIn(ids);
    }

    @Transactional
    public OrderEntity saveOrder(OrderEntity order) {
        return orderRepository.save(order);
    }

    public List<HomeOrderDto> getUrgentProductionOrders(int limit) {
        List<OrderEntity> orders = orderRepository
                .findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus.IN_PRODUCTION);

        // Ограничиваем количество
        if (orders.size() > limit) {
            orders = orders.subList(0, limit);
        }

        LocalDate today = LocalDate.now();

        return orders.stream()
                .map(order -> {
                    HomeOrderDto dto = new HomeOrderDto();
                    dto.setId(order.getId());
                    dto.setCompanyName(order.getCompany().getName());
                    dto.setProductType(order.getProductType());
                    dto.setProductName(order.getProductName());
                    dto.setQuantity(order.getQuantity());

                    long days = ChronoUnit.DAYS.between(today, order.getDeadline());
                    dto.setDaysUntilDeadline(days);

                    // Определяем класс срочности
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
                })
                .collect(Collectors.toList());
    }
}