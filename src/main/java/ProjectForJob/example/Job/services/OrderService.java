package ProjectForJob.example.Job.services;


import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.DataTransferObject.kafkaDto.OrderStartedEvent;
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
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
import ProjectForJob.example.Job.services.mapping.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ProjectForJob.example.Job.util.WorkpieceCalculator.calculateWeight;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final CouplingRepository couplingRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private final PipeAdapterRepository pipeAdapterRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderMapper orderMapper;
    private final CouplingService couplingService;//    Разобраться что лучше использовать сервис или репозиторий!!!!!!!
    private final PipeAdapterService pipeAdapterService;//

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

        // считаем вес,длину и стенку
        if (dto.getCouplingId() != null) {
            CouplingEntity coupling = couplingService.findById(dto.getCouplingId());
            order.setCoupling(coupling);
            // Заполняем заготовку
            fillWorkpieceData(order,
                    coupling.getOuterDiameterMm(),
                    coupling.getInnerDiameterMm(),
                    coupling.getLengthMm(),
                    dto.getQuantity());
        } else if (dto.getAdapterId() != null) {
            PipeAdapterEntity adapter = pipeAdapterService.findById(dto.getAdapterId());
            order.setAdapter(adapter);
            fillWorkpieceData(order,
                    adapter.getOuterDiameterMm(),
                    adapter.getInnerDiameterMm(),
                    adapter.getLengthMm(),
                    dto.getQuantity());
        }

        order.setTotalCost(calculateTotalCost(order));
        return orderMapper.toDto(orderRepository.save(order));
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
        return orderMapper.toDto(orderRepository.save(order));
    }

    public Page<OrderDto> getOrdersByStatus(OrderStatus status, String search, Pageable pageable) {
        Page<OrderEntity> page;
        if (search != null && !search.isBlank()) {
            page = orderRepository.findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    status, search, pageable);
        } else {
            page = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return page.map(orderMapper::toDto);
    }

    public OrderDto getOrderById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        return orderMapper.toDto(order);
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

        if (dto.getCouplingId() != null) {
            CouplingEntity coupling = couplingService.findById(dto.getCouplingId());
            order.setCoupling(coupling);
            // Заполняем заготовку
            fillWorkpieceData(order,
                    coupling.getOuterDiameterMm(),
                    coupling.getInnerDiameterMm(),
                    coupling.getLengthMm(),
                    dto.getQuantity());
        } else if (dto.getAdapterId() != null) {
            PipeAdapterEntity adapter = pipeAdapterService.findById(dto.getAdapterId());
            order.setAdapter(adapter);
            fillWorkpieceData(order,
                    adapter.getOuterDiameterMm(),
                    adapter.getInnerDiameterMm(),
                    adapter.getLengthMm(),
                    dto.getQuantity());
        }

        order.setQuantity(dto.getQuantity());
        order.setDeadline(dto.getDeadline());
        order.setAdditionalWorks(dto.getAdditionalWorkIds() != null
                ? additionalWorkRepository.findAllById(dto.getAdditionalWorkIds()) : List.of());
        order.setTotalCost(calculateTotalCost(order));

        return orderMapper.toDto(orderRepository.save(order));
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

        if (orders.size() > limit) {
            orders = orders.subList(0, limit);
        }
        LocalDate today = LocalDate.now();

        return orders.stream()
                .map(order -> orderMapper.toHomeOrderDto(order, today))
                .collect(Collectors.toList());
    }

    public List<OrderEntity> getUrgentProductionOrderEntities(int limit) {
        List<OrderEntity> orders = orderRepository
                .findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus.IN_PRODUCTION);
        if (orders.size() > limit) {
            orders = orders.subList(0, limit);
        }
        return orders;
    }

    @Transactional
    public void startProduction(Long orderId, LocalDate deadline) {
        OrderEntity order = getOrderEntityById(orderId);
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new IllegalStateException("Заказ уже не в статусе ожидания");
        }
        if (deadline != null) {
            order.setDeadline(deadline);
        }
        order.setStatus(OrderStatus.IN_PRODUCTION);
        orderRepository.save(order);

        try {
            OrderStartedEvent event = orderMapper.toStartedEvent(order);  // было buildOrderStartedEvent
            kafkaProducerService.sendOrderStartedEvent(event);
        } catch (Exception e) {
            log.error("Не удалось отправить событие в Kafka для заказа {}", orderId, e);
        }
    }

    private void fillWorkpieceData(OrderEntity order,
                                   double outerDiameterMm,
                                   double innerDiameterMm,
                                   double pieceLengthMm, // длина одной штуки в мм
                                   int quantity) {
        double wall = (outerDiameterMm - innerDiameterMm) / 2.0;
        double totalLengthM = (pieceLengthMm * quantity) / 1000.0; // в метры
        double weight = calculateWeight(outerDiameterMm, wall, totalLengthM);

        order.setWorkpieceOuterDiameter(outerDiameterMm);
        order.setWorkpieceWallThickness(wall);
        order.setWorkpieceLengthMeters(totalLengthM);
        order.setWorkpieceWeightKg(weight);
    }

}