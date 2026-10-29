package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.dto.HomeOrderDto;
import ProjectForJob.example.Job.dto.OrderCreateDto;
import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.dto.OrderUpdateDto;
import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderStatus;
import ProjectForJob.example.Job.domain.entity.order.PipeProductKind;
import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelGradeEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelStrengthGroupEntity;
import ProjectForJob.example.Job.repository.order.CompanyRepository;
import ProjectForJob.example.Job.repository.handbook.AdditionalWorkRepository;
import ProjectForJob.example.Job.repository.handbook.CouplingRepository;
import ProjectForJob.example.Job.repository.handbook.ExternalThreadRepository;
import ProjectForJob.example.Job.repository.handbook.PipeAdapterRepository;
import ProjectForJob.example.Job.repository.handbook.SteelGradeRepository;
import ProjectForJob.example.Job.repository.handbook.SteelStrengthGroupRepository;
import ProjectForJob.example.Job.repository.order.OrderRepository;
import ProjectForJob.example.Job.service.mapper.OrderMapper;
import ProjectForJob.example.Job.util.OrderCostCalculator;
import ProjectForJob.example.Job.util.PipeOrderCostCalculator;
import ProjectForJob.example.Job.exception.BusinessException;
import ProjectForJob.example.Job.exception.service.order.OrderErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import static ProjectForJob.example.Job.util.WorkpieceCalculator.calculateWeight;

/**
 * Центральный сервис заказов.
 * <p>
 * Поддерживает четыре вида продукции:
 * 1) COUPLING  — муфта          (quantity штук, workpiece через D_нар/D_внутр)
 * 2) ADAPTER   — переводник     (quantity штук, workpiece через D_нар/D_внутр)
 * 3) PIPE      — труба          (метраж ИЛИ тоннаж; quantity вычисляется через avgLength)
 * 4) NIPPLE    — патрубок       (quantity штук × pieceLengthMm)
 * <p>
 * Цена:
 * - COUPLING/ADAPTER → {@link OrderCostCalculator}       (unitCost × Q)
 * - PIPE/NIPPLE      → {@link PipeOrderCostCalculator}   (threadsCost + metalCost)
 * <p>
 * Snapshot надбавки за группу прочности (appliedMarkupPercent) фиксируется
 * на момент создания/редактирования заказа и не пересчитывается при
 * изменении процента в справочнике.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final CouplingRepository couplingRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private final PipeAdapterRepository pipeAdapterRepository;
    private final SteelStrengthGroupRepository strengthGroupRepository;
    private final SteelGradeRepository steelGradeRepository;
    private final ExternalThreadRepository externalThreadRepository;
    private final OrderMapper orderMapper;

    /**
     * Дефолтная средняя длина одной трубы, м (если менеджер не указал).
     */
    private static final double DEFAULT_AVERAGE_PIPE_LENGTH_M = 10.0;

    /**
     * Плотность стали, кг/м³ — для обратного пересчёта тоннаж → метраж.
     */
    private static final double STEEL_DENSITY = 7850.0;

    // ================================================================
    // СОЗДАНИЕ
    // ================================================================

    /**
     * Создание нового заказа.
     * Все вычисления (вес, метраж, quantity для PIPE, snapshot надбавки)
     * происходят внутри — снаружи приходит только DTO из формы.
     */
    @Transactional
    public OrderDto createOrder(OrderCreateDto dto) {
        log.info("Создание заказа: productType={}, company={}",
                dto.getProductType(), dto.getCompanyName());

        CompanyEntity company = resolveCompany(dto.getCompanyName());

        OrderEntity order = OrderEntity.builder()
                .createdAt(LocalDateTime.now())
                .company(company)
                .deadline(dto.getDeadline())
                .additionalWorks(resolveAdditionalWorks(dto.getAdditionalWorkIds()))
                .status(OrderStatus.WAITING)
                .build();

        // 1) Продукт + пересчёт заготовки / веса / метража / quantity
        applyProductAndRecalc(order,
                dto.getProductType(),
                dto.getCouplingId(),
                dto.getAdapterId(),
                dto.getExternalThreadId(),
                dto.getPipeProductKind(),
                dto.getQuantity(),
                dto.getWallThicknessMm(),
                dto.getAveragePipeLengthM(),
                dto.getOrderTotalLengthMeters(),
                dto.getOrderTotalWeightKg(),
                dto.getPricePerTon(),
                dto.getPieceLengthMm());

        // 2) Группа прочности + марка + snapshot надбавки
        applyStrengthAndGrade(order, dto.getStrengthGroupId(), dto.getSteelGradeId());

        // 3) Финальный расчёт цены
        order.setTotalCost(calculateTotalCost(order));

        OrderEntity saved = orderRepository.save(order);
        log.info("Заказ создан: id={}, totalCost={}", saved.getId(), saved.getTotalCost());

        return orderMapper.toDto(saved);
    }

    // ================================================================
    // РЕДАКТИРОВАНИЕ
    // ================================================================

    /**
     * Полное редактирование заказа. Все поля пересчитываются заново —
     * так проще гарантировать консистентность, чем пытаться диффить.
     */
    @Transactional
    public OrderDto updateOrder(Long orderId, OrderUpdateDto dto) {
        log.info("Редактирование заказа: id={}, productType={}",
                orderId, dto.getProductType());

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND, "Заказ не найден: id=" + orderId));

        order.setCompany(resolveCompany(dto.getCompanyName()));
        order.setDeadline(dto.getDeadline());
        order.setAdditionalWorks(resolveAdditionalWorks(dto.getAdditionalWorkIds()));

        applyProductAndRecalc(order,
                dto.getProductType(),
                dto.getCouplingId(),
                dto.getAdapterId(),
                dto.getExternalThreadId(),
                dto.getPipeProductKind(),
                dto.getQuantity(),
                dto.getWallThicknessMm(),
                dto.getAveragePipeLengthM(),
                dto.getOrderTotalLengthMeters(),
                dto.getOrderTotalWeightKg(),
                dto.getPricePerTon(),
                dto.getPieceLengthMm());

        applyStrengthAndGrade(order, dto.getStrengthGroupId(), dto.getSteelGradeId());
        order.setTotalCost(calculateTotalCost(order));

        return orderMapper.toDto(orderRepository.save(order));
    }

    // ================================================================
    // ХЕЛПЕРЫ: компания, доп. работы
    // ================================================================

    private CompanyEntity resolveCompany(String companyName) {
        return companyRepository.findByNameIgnoreCase(companyName)
                .orElseGet(() -> companyRepository.save(
                        CompanyEntity.builder().name(companyName.trim()).build()));
    }

    private List<AdditionalWorkEntity> resolveAdditionalWorks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(additionalWorkRepository.findAllById(ids));
    }

    // ================================================================
    // ЯДРО: обработка продукта и пересчёт заготовки
    // ================================================================

    /**
     * Полностью пересобирает продуктовую часть заказа:
     * - очищает все продуктовые поля (чтобы при смене типа не осталось мусора)
     * - в зависимости от productType заполняет нужные поля
     * - пересчитывает workpiece_* и (для PIPE) order_total_* / quantity
     * <p>
     * ВАЖНО: quantity для PIPE не берётся из DTO — вычисляется через avgLength.
     * Для COUPLING/ADAPTER/NIPPLE quantity приходит из DTO.
     */
    private void applyProductAndRecalc(OrderEntity order,
                                       String productType,
                                       Long couplingId,
                                       Long adapterId,
                                       Long externalThreadId,
                                       String pipeProductKind,
                                       Integer quantity,
                                       Double wallThicknessMm,
                                       Double averagePipeLengthM,
                                       Double orderTotalLengthMeters,
                                       Double orderTotalWeightKg,
                                       BigDecimal pricePerTon,
                                       Double pieceLengthMm) {

        // ---- очищаем всё продуктовое ----
        order.setCoupling(null);
        order.setAdapter(null);
        order.setExternalThread(null);
        order.setPipeProductKind(null);
        order.setWallThicknessMm(null);
        order.setAveragePipeLengthM(null);
        order.setOrderTotalLengthMeters(null);
        order.setOrderTotalWeightKg(null);
        order.setPricePerTon(null);
        order.setPieceLengthMm(null);

        // заготовка тоже обнуляется — заполнится ниже
        order.setWorkpieceOuterDiameter(null);
        order.setWorkpieceWallThickness(null);
        order.setWorkpieceLengthMeters(null);
        order.setWorkpieceWeightKg(null);
        order.setQuantity(0);

        if (productType == null) {
            throw new BusinessException(OrderErrorCode.PRODUCT_TYPE_MISSING);
        }

        switch (productType) {
            case "COUPLING" -> applyCoupling(order, couplingId, quantity);
            case "ADAPTER" -> applyAdapter(order, adapterId, quantity);
            case "PIPE", "NIPPLE" -> applyPipeOrNipple(order,
                    productType,
                    externalThreadId,
                    pipeProductKind,
                    quantity,
                    wallThicknessMm,
                    averagePipeLengthM,
                    orderTotalLengthMeters,
                    orderTotalWeightKg,
                    pricePerTon,
                    pieceLengthMm);
            default -> throw new BusinessException(
                    OrderErrorCode.PRODUCT_TYPE_INVALID,
                    "Неверный тип продукции: " + productType);
        }
    }

    /**
     * Муфта: quantity штук, заготовка считается по D_нар / D_внутр из справочника.
     */
    private void applyCoupling(OrderEntity order, Long couplingId, Integer quantity) {
        if (couplingId == null) {
            throw new BusinessException(OrderErrorCode.COUPLING_NOT_SELECTED);
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(OrderErrorCode.QUANTITY_NOT_POSITIVE,
                    "Количество должно быть положительным, получено: " + quantity);
        }
        CouplingEntity coupling = couplingRepository.findById(couplingId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.COUPLING_NOT_FOUND, "Муфта не найдена: id=" + couplingId));

        order.setCoupling(coupling);
        order.setQuantity(quantity);

        fillWorkpieceFromDiameters(order,
                coupling.getOuterDiameterMm(),
                coupling.getInnerDiameterMm(),
                coupling.getLengthMm(),
                quantity);
    }

    /**
     * Переводник: аналогично муфте.
     */
    private void applyAdapter(OrderEntity order, Long adapterId, Integer quantity) {
        if (adapterId == null) {
            throw new BusinessException(OrderErrorCode.ADAPTER_NOT_SELECTED);
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(OrderErrorCode.QUANTITY_NOT_POSITIVE,
                    "Количество должно быть положительным, получено: " + quantity);
        }
        PipeAdapterEntity adapter = pipeAdapterRepository.findById(adapterId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ADAPTER_NOT_FOUND, "Переводник не найден: id=" + adapterId));

        order.setAdapter(adapter);
        order.setQuantity(quantity);

        fillWorkpieceFromDiameters(order,
                adapter.getOuterDiameterMm(),
                adapter.getInnerDiameterMm(),
                adapter.getLengthMm(),
                quantity);
    }

    /**
     * Труба или патрубок.
     * <p>
     * Общая логика:
     * 1) Находим наружную резьбу (тип + условный диаметр, напр. "НКТ 73").
     * 2) Парсим условный диаметр в число (73 → 73.0 мм).
     * 3) Считаем общий метраж и общий вес.
     * 4) Считаем quantity (для PIPE — ceil(totalLength / avgLength); для NIPPLE — из DTO).
     * 5) Заполняем workpiece_* и order_total_* / price_per_ton.
     * <p>
     * Для PIPE менеджер вводит ИЛИ метраж, ИЛИ тоннаж.
     * Второе значение вычисляется через WorkpieceCalculator.
     */
    private void applyPipeOrNipple(OrderEntity order,
                                   String productType,
                                   Long externalThreadId,
                                   String pipeProductKind,
                                   Integer quantity,
                                   Double wallThicknessMm,
                                   Double averagePipeLengthM,
                                   Double orderTotalLengthMeters,
                                   Double orderTotalWeightKg,
                                   BigDecimal pricePerTon,
                                   Double pieceLengthMm) {

        if (externalThreadId == null) {
            throw new BusinessException(OrderErrorCode.THREAD_NOT_SELECTED);
        }
            // <= 0 проверит сам WorkpieceCalculator (CALC-002), здесь — только null,
            // чтобы не было NPE при unboxing Double → double.
        if (wallThicknessMm == null) {
            throw new BusinessException(OrderErrorCode.WALL_THICKNESS_REQUIRED);
        }
        if (pricePerTon == null || pricePerTon.signum() <= 0) {
            throw new BusinessException(OrderErrorCode.PRICE_PER_TON_REQUIRED);
        }
        ExternalThreadEntity thread = externalThreadRepository.findById(externalThreadId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.THREAD_NOT_FOUND, "Резьба не найдена: id=" + externalThreadId));

        // "73" → 73.0. Если справочник кривой — не молчим, падаем явно.
        double outerDiameterMm;
        try {
            outerDiameterMm = Double.parseDouble(thread.getConditionalDiameter());
        } catch (NumberFormatException e) {
            throw new BusinessException(OrderErrorCode.THREAD_DIAMETER_PARSE_ERROR,
                    "Не удалось разобрать условный диаметр резьбы: '"
                            + thread.getConditionalDiameter() + "'", e);
        }

        double wall = wallThicknessMm;
        double totalLengthM;
        double weightKg;
        int finalQuantity;

        if ("PIPE".equals(productType)) {
            // ---- ТРУБА ----
            double avgLength = (averagePipeLengthM != null && averagePipeLengthM > 0)
                    ? averagePipeLengthM
                    : DEFAULT_AVERAGE_PIPE_LENGTH_M;

            if (orderTotalLengthMeters != null && orderTotalLengthMeters > 0) {
                // Менеджер задал метраж → считаем вес
                totalLengthM = orderTotalLengthMeters;
                weightKg = calculateWeight(outerDiameterMm, wall, totalLengthM);
            } else if (orderTotalWeightKg != null && orderTotalWeightKg > 0) {
                // Менеджер задал тоннаж → считаем метраж
                weightKg = orderTotalWeightKg;
                totalLengthM = lengthFromWeight(outerDiameterMm, wall, weightKg);
            } else {
                throw new BusinessException(OrderErrorCode.PIPE_LENGTH_OR_WEIGHT_REQUIRED);
            }

            // quantity труб — оценка через среднюю длину
            finalQuantity = (int) Math.ceil(totalLengthM / avgLength);
            if (finalQuantity < 1) finalQuantity = 1;

            order.setAveragePipeLengthM(avgLength);
            order.setOrderTotalLengthMeters(totalLengthM);
            order.setOrderTotalWeightKg(weightKg);

        } else {
            // ---- ПАТРУБОК ----
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(OrderErrorCode.QUANTITY_NOT_POSITIVE,
                        "Количество патрубков должно быть положительным, получено: " + quantity);
            }
            if (pieceLengthMm == null || pieceLengthMm <= 0) {
                throw new BusinessException(OrderErrorCode.NIPPLE_LENGTH_REQUIRED,
                        "Длина патрубка должна быть положительной, получено: " + pieceLengthMm);
            }

            finalQuantity = quantity;
            totalLengthM = (pieceLengthMm * quantity) / 1000.0;
            weightKg = calculateWeight(outerDiameterMm, wall, totalLengthM);

            order.setPieceLengthMm(pieceLengthMm);
            order.setOrderTotalLengthMeters(totalLengthM);
            order.setOrderTotalWeightKg(weightKg);
        }

        // ---- общие поля ----
        order.setExternalThread(thread);
        order.setPipeProductKind(PipeProductKind.valueOf(productType));
        order.setWallThicknessMm(wallThicknessMm);
        order.setPricePerTon(pricePerTon);
        order.setQuantity(finalQuantity);

        // ---- workpiece_* для совместимости с UI (тултипы, карточки) ----
        order.setWorkpieceOuterDiameter(outerDiameterMm);
        order.setWorkpieceWallThickness(wall);
        order.setWorkpieceLengthMeters(totalLengthM);
        order.setWorkpieceWeightKg(weightKg);

        log.debug("applyPipeOrNipple: kind={}, D={}, S={}, totalLen={}, weight={}, qty={}",
                productType, outerDiameterMm, wall, totalLengthM, weightKg, finalQuantity);
    }

    /**
     * Обратный пересчёт: вес (кг) → длина (м).
     * <p>
     * Формула выведена из WorkpieceCalculator.calculateWeight:
     * M = π × ρ × S × (D − S) × L / 1_000_000
     * ⇒ L = M × 1_000_000 / (π × ρ × S × (D − S))
     */
    private double lengthFromWeight(double outerDiameterMm,
                                    double wallThicknessMm,
                                    double weightKg) {
        double denom = Math.PI * STEEL_DENSITY
                * wallThicknessMm * (outerDiameterMm - wallThicknessMm)
                / 1_000_000.0;
        if (denom <= 0) {
            throw new BusinessException(OrderErrorCode.INVALID_PIPE_PARAMS,
                    "Некорректные параметры трубы: D=" + outerDiameterMm
                            + ", S=" + wallThicknessMm);
        }
        return weightKg / denom;
    }

    // ================================================================
    // Группа прочности + марка стали
    // ================================================================

    /**
     * Валидирует пару (группа, марка), подтягивает snapshot надбавки.
     * <p>
     * Правила:
     * 1) Оба null → группа = null, марка = null, appliedMarkup = 0.
     * 2) Задана только группа → марка = null, appliedMarkup = группы.markupPercent.
     * 3) Задана только марка → группа = марки.strengthGroup (автоподстановка),
     * appliedMarkup = группы.markupPercent.
     * 4) Заданы оба → марка обязана принадлежать указанной группе,
     * иначе IllegalArgumentException.
     * appliedMarkup = группы.markupPercent.
     */
    private void applyStrengthAndGrade(OrderEntity order, Long groupId, Long gradeId) {
        // сбрасываем предыдущее состояние
        order.setStrengthGroup(null);
        order.setSteelGrade(null);
        order.setAppliedMarkupPercent(BigDecimal.ZERO);

        // случай 1: ничего не задано
        if (groupId == null && gradeId == null) {
            return;
        }

        SteelStrengthGroupEntity group;
        SteelGradeEntity grade = null;

        if (groupId == null) {
            grade = steelGradeRepository.findById(gradeId)
                    .orElseThrow(() -> new BusinessException(
                            OrderErrorCode.GRADE_NOT_FOUND, "Марка стали не найдена: id=" + gradeId));
            group = grade.getStrengthGroup();
        } else {
            group = strengthGroupRepository.findById(groupId)
                    .orElseThrow(() -> new BusinessException(
                            OrderErrorCode.GROUP_NOT_FOUND, "Группа прочности не найдена: id=" + groupId));

            if (gradeId != null) {
                grade = steelGradeRepository.findById(gradeId)
                        .orElseThrow(() -> new BusinessException(
                                OrderErrorCode.GRADE_NOT_FOUND, "Марка стали не найдена: id=" + gradeId));
                if (!grade.getStrengthGroup().getId().equals(group.getId())) {
                    throw new BusinessException(OrderErrorCode.GRADE_NOT_IN_GROUP,
                            "Марка «" + grade.getName() + "» не принадлежит группе «"
                                    + group.getCode() + "»");
                }
            }
        }

        order.setStrengthGroup(group);
        order.setSteelGrade(grade);
        order.setAppliedMarkupPercent(
                group.getMarkupPercent() != null ? group.getMarkupPercent() : BigDecimal.ZERO);
    }

    // ================================================================
    // РАСЧЁТ СТОИМОСТИ
    // ================================================================

    /**
     * Развилка по типу продукции:
     * - муфта/переводник → OrderCostCalculator (unitCost × Q)
     * - труба/патрубок   → PipeOrderCostCalculator (threadsCost + metalCost)
     * <p>
     * Единственное место в сервисе, где вызываются калькуляторы.
     * Никаких формул "по месту" — всё в util-классах.
     */
    private BigDecimal calculateTotalCost(OrderEntity order) {
        if (order.getPipeProductKind() != null && order.getExternalThread() != null) {
            return PipeOrderCostCalculator.calculate(
                    order.getExternalThread().getManufacturingCost(),
                    order.getPricePerTon(),
                    order.getQuantity() != null ? order.getQuantity() : 0,
                    order.getOrderTotalWeightKg(),
                    order.getAdditionalWorks(),
                    order.getAppliedMarkupPercent());
        }
        return OrderCostCalculator.calculate(
                order.getUnitManufacturingCost(),
                order.getAdditionalWorks(),
                order.getQuantity(),
                order.getAppliedMarkupPercent(),
                order.getWorkpieceWeightKg());
    }

    // ================================================================
    // ЗАГОТОВКА ДЛЯ МУФТ И ПЕРЕВОДНИКОВ
    // ================================================================

    /**
     * Заполняет workpiece_* для муфты/переводника.
     * Для труб/патрубков используется свой путь внутри applyPipeOrNipple.
     */
    private void fillWorkpieceFromDiameters(OrderEntity order,
                                            double outerDiameterMm,
                                            double innerDiameterMm,
                                            double pieceLengthMm,
                                            int quantity) {
        double wall = (outerDiameterMm - innerDiameterMm) / 2.0;
        double totalLengthM = (pieceLengthMm * quantity) / 1000.0;
        double weight = calculateWeight(outerDiameterMm, wall, totalLengthM);

        order.setWorkpieceOuterDiameter(outerDiameterMm);
        order.setWorkpieceWallThickness(wall);
        order.setWorkpieceLengthMeters(totalLengthM);
        order.setWorkpieceWeightKg(weight);
    }

    // ================================================================
    // СТАТУСЫ / CRUD
    // ================================================================

    @Transactional
    public OrderDto updateStatus(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND, "Заказ не найден: id=" + orderId));
        order.setStatus(newStatus);
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND, "Заказ не найден: id=" + id));
        return orderMapper.toDto(order);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND, "Заказ не найден: id=" + orderId));
        orderRepository.delete(order);
    }

    public OrderEntity getOrderEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND, "Заказ не найден, id=" + id));
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
            throw new BusinessException(OrderErrorCode.ORDER_NOT_WAITING,
                    "Заказ уже не в статусе ожидания, текущий: " + order.getStatus());
        }
        if (deadline != null) {
            order.setDeadline(deadline);
        }
        order.setStatus(OrderStatus.IN_PRODUCTION);
        orderRepository.save(order);
    }
}