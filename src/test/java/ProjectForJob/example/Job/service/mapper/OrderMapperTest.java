package ProjectForJob.example.Job.service.mapper;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelGradeEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelStrengthGroupEntity;
import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderStatus;
import ProjectForJob.example.Job.dto.OrderDto;
import ProjectForJob.example.Job.domain.entity.order.PipeProductKind;
import ProjectForJob.example.Job.dto.HomeOrderDto;
import ProjectForJob.example.Job.dto.kafka.AdditionalWorkDto;
import ProjectForJob.example.Job.dto.kafka.OrderStartedEvent;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты OrderMapper.
 *
 * Инварианты домена (ровно один продукт, company != null и т.п.) —
 * ответственность OrderService; здесь проверяем только корректность
 * маппинга при валидной сущности.
 */
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    // ================================================================
    // COUPLING
    // ================================================================

    @Test
    void toDto_coupling_mapsAllFields() {
        CouplingEntity coupling = couplingFixture();
        OrderEntity order = baseOrder()
                .coupling(coupling)
                .quantity(10)
                .workpieceOuterDiameter(100.0)
                .workpieceWallThickness(10.0)
                .workpieceLengthMeters(2.0)
                .workpieceWeightKg(15.5)
                .build();

        OrderDto dto = mapper.toDto(order);

        // ---- база ----
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCompanyName()).isEqualTo("ООО Ромашка");
        assertThat(dto.getCreatedAt()).isEqualTo(order.getCreatedAt());
        assertThat(dto.getQuantity()).isEqualTo(10);
        assertThat(dto.getDeadline()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(dto.getStatus()).isEqualTo(OrderStatus.WAITING);
        assertThat(dto.getTotalCost()).isEqualByComparingTo("15000.00");

        // ---- product (top-level) ----
        assertThat(dto.getProductType()).isEqualTo("COUPLING");
        assertThat(dto.getProductId()).isEqualTo(100L);
        assertThat(dto.getProductName()).isEqualTo("Муфта 73");

        // ---- additionalWorks ----
        assertThat(dto.getAdditionalWorkNames()).containsExactly("Покраска");
        assertThat(dto.getAdditionalWorkIds()).containsExactly(1L);

        // ---- workpiece ----
        assertThat(dto.getWorkpieceOuterDiameter()).isEqualTo(100.0);
        assertThat(dto.getWorkpieceWallThickness()).isEqualTo(10.0);
        assertThat(dto.getWorkpieceLengthMeters()).isEqualTo(2.0);
        assertThat(dto.getWorkpieceWeightKg()).isEqualTo(15.5);

        // ---- coupling-specific ----
        assertThat(dto.getProductVariantType()).isEqualTo("Муфта");
        assertThat(dto.getProductVariantDiameter()).isEqualTo("73");

        // ---- common product attrs ----
        assertThat(dto.getProductLengthMm()).isEqualTo(200.0);
        assertThat(dto.getProductWeightKg()).isEqualTo(15.5);
        assertThat(dto.getProductOuterDiameterMm()).isEqualTo(100.0);
        assertThat(dto.getProductInnerDiameterMm()).isEqualTo(80.0);
        assertThat(dto.getProductImagePath()).isEqualTo("/images/coupling.png");
        assertThat(dto.getProductDescription()).isEqualTo("Стандартная муфта");
        assertThat(dto.getProductManufacturingCost()).isEqualByComparingTo("1500.00");
        assertThat(dto.getProductPriceForEmployee()).isEqualByComparingTo("350.00");

        // ---- не должно протечь от adapter/pipe ----
        assertThat(dto.getProductAdapterFirstSideType()).isNull();
        assertThat(dto.getExternalThreadId()).isNull();
        assertThat(dto.getPipeProductKind()).isNull();
    }

    // ================================================================
    // ADAPTER
    // ================================================================

    @Test
    void toDto_adapter_mapsAllFields() {
        PipeAdapterEntity adapter = adapterFixture();
        OrderEntity order = baseOrder()
                .adapter(adapter)
                .quantity(5)
                .workpieceOuterDiameter(105.0)
                .workpieceWallThickness(10.0)
                .workpieceLengthMeters(1.25)
                .workpieceWeightKg(18.0)
                .build();

        OrderDto dto = mapper.toDto(order);

        // ---- top-level product ----
        assertThat(dto.getProductType()).isEqualTo("ADAPTER");
        assertThat(dto.getProductId()).isEqualTo(200L);
        assertThat(dto.getProductName()).isEqualTo("НКТ 73 x ОТТМ 89");

        // ---- adapter-specific (обе стороны) ----
        assertThat(dto.getProductAdapterFirstSideType()).isEqualTo("НКТ");
        assertThat(dto.getProductAdapterFirstSideDiameter()).isEqualTo("73");
        assertThat(dto.getProductAdapterSecondSideType()).isEqualTo("ОТТМ");
        assertThat(dto.getProductAdapterSecondSideDiameter()).isEqualTo("89");

        // ---- common product attrs (тот же код, что у coupling) ----
        assertThat(dto.getProductLengthMm()).isEqualTo(250.0);
        assertThat(dto.getProductWeightKg()).isEqualTo(18.0);
        assertThat(dto.getProductOuterDiameterMm()).isEqualTo(105.0);
        assertThat(dto.getProductInnerDiameterMm()).isEqualTo(85.0);
        assertThat(dto.getProductImagePath()).isEqualTo("/images/adapter.png");
        assertThat(dto.getProductDescription()).isEqualTo("Переходник");
        assertThat(dto.getProductManufacturingCost()).isEqualByComparingTo("2000.00");
        assertThat(dto.getProductPriceForEmployee()).isEqualByComparingTo("400.00");

        // ---- coupling-specific не заполняется ----
        assertThat(dto.getProductVariantType()).isNull();
        assertThat(dto.getProductVariantDiameter()).isNull();
    }

    // ================================================================
    // Strength: проверяем на coupling — код общий для всех продуктов
    // ================================================================

    @Test
    void toDto_withStrengthGroupAndGrade_mapsSnapshotAndNames() {
        SteelStrengthGroupEntity group = groupFixture();
        SteelGradeEntity grade = gradeFixture(group);

        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .quantity(1)
                .strengthGroup(group)
                .steelGrade(grade)
                .appliedMarkupPercent(new BigDecimal("12.50"))
                .build();

        OrderDto dto = mapper.toDto(order);

        assertThat(dto.getStrengthGroupId()).isEqualTo(10L);
        assertThat(dto.getStrengthGroupCode()).isEqualTo("D");
        assertThat(dto.getStrengthGroupName()).isEqualTo("Группа D");
        assertThat(dto.getSteelGradeId()).isEqualTo(20L);
        assertThat(dto.getSteelGradeName()).isEqualTo("40Х");
        assertThat(dto.getAppliedMarkupPercent()).isEqualByComparingTo("12.50");
    }

    // ================================================================
    // Фикстуры
    // ================================================================

    private static CompanyEntity companyFixture() {
        return CompanyEntity.builder()
                .id(1L)
                .name("ООО Ромашка")
                .build();
    }

    private static CouplingEntity couplingFixture() {
        return CouplingEntity.builder()
                .id(100L)
                .type("Муфта")
                .conditionalDiameter("73")
                .lengthMm(200.0)
                .weightKg(15.5)
                .outerDiameterMm(100.0)
                .innerDiameterMm(80.0)
                .manufacturingCost(new BigDecimal("1500.00"))
                .priceForEmployee(new BigDecimal("350.00"))
                .imagePath("/images/coupling.png")
                .description("Стандартная муфта")
                .build();
    }

    private static PipeAdapterEntity adapterFixture() {
        return PipeAdapterEntity.builder()
                .id(200L)
                .firstSideType("НКТ")
                .firstSideDiameter("73")
                .secondSideType("ОТТМ")
                .secondSideDiameter("89")
                .lengthMm(250.0)
                .weightKg(18.0)
                .outerDiameterMm(105.0)
                .innerDiameterMm(85.0)
                .manufacturingCost(new BigDecimal("2000.00"))
                .priceForEmployee(new BigDecimal("400.00"))
                .imagePath("/images/adapter.png")
                .description("Переходник")
                .build();
    }

    private static ExternalThreadEntity threadFixture() {
        return ExternalThreadEntity.builder()
                .id(300L)
                .type("НКТ")
                .conditionalDiameter("73")
                .manufacturingCost(new BigDecimal("500.00"))
                .priceForEmployee(new BigDecimal("150.00"))
                .imagePath("/images/thread.png")
                .build();
    }

    private static AdditionalWorkEntity additionalWorkFixture() {
        return AdditionalWorkEntity.builder()
                .id(1L)
                .name("Покраска")
                .price(new BigDecimal("500.00"))
                .build();
    }

    private static SteelStrengthGroupEntity groupFixture() {
        return SteelStrengthGroupEntity.builder()
                .id(10L)
                .code("D")
                .name("Группа D")
                .markupPercent(new BigDecimal("12.50"))
                .isDefault(false)
                .active(true)
                .build();
    }

    private static SteelGradeEntity gradeFixture(SteelStrengthGroupEntity group) {
        return SteelGradeEntity.builder()
                .id(20L)
                .strengthGroup(group)
                .name("40Х")
                .active(true)
                .build();
    }

    /**
     * Базовый заказ: всё, что не зависит от типа продукта.
     * Продукт, quantity и workpiece заполняются в конкретном тесте.
     */
    private static OrderEntity.OrderEntityBuilder baseOrder() {
        return OrderEntity.builder()
                .id(1L)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                .company(companyFixture())
                .deadline(LocalDate.of(2026, 2, 1))
                .status(OrderStatus.WAITING)
                .totalCost(new BigDecimal("15000.00"))
                .additionalWorks(List.of(additionalWorkFixture()));
    }

    // ================================================================
    // PIPE
    // ================================================================

    @Test
    void toDto_pipe_mapsAllFields() {
        OrderEntity order = baseOrder()
                .externalThread(threadFixture())
                .pipeProductKind(PipeProductKind.PIPE)
                .quantity(10)
                .wallThicknessMm(5.5)
                .averagePipeLengthM(10.0)
                .orderTotalLengthMeters(100.0)
                .orderTotalWeightKg(946.0)
                .pricePerTon(new BigDecimal("80000.00"))
                .workpieceOuterDiameter(73.0)
                .workpieceWallThickness(5.5)
                .workpieceLengthMeters(100.0)
                .workpieceWeightKg(946.0)
                .build();

        OrderDto dto = mapper.toDto(order);

        // ---- top-level product ----
        assertThat(dto.getProductType()).isEqualTo("PIPE");
        assertThat(dto.getProductName()).isEqualTo("Труба НКТ 73");
        assertThat(dto.getProductId()).isEqualTo(300L);

        // ---- thread ----
        assertThat(dto.getExternalThreadId()).isEqualTo(300L);
        assertThat(dto.getExternalThreadName()).isEqualTo("НКТ 73");
        assertThat(dto.getExternalThreadDiameter()).isEqualTo("73");
        assertThat(dto.getExternalThreadManufacturingCost()).isEqualByComparingTo("500.00");
        assertThat(dto.getExternalThreadPriceForEmployee()).isEqualByComparingTo("150.00");
        assertThat(dto.getExternalThreadImagePath()).isEqualTo("/images/thread.png");

        // ---- pipe fields ----
        assertThat(dto.getPipeProductKind()).isEqualTo("PIPE");
        assertThat(dto.getWallThicknessMm()).isEqualTo(5.5);
        assertThat(dto.getAveragePipeLengthM()).isEqualTo(10.0);
        assertThat(dto.getOrderTotalLengthMeters()).isEqualTo(100.0);
        assertThat(dto.getOrderTotalWeightKg()).isEqualTo(946.0);
        assertThat(dto.getPricePerTon()).isEqualByComparingTo("80000.00");
        assertThat(dto.getPieceLengthMm()).isNull();

        // ---- workpiece ----
        assertThat(dto.getWorkpieceOuterDiameter()).isEqualTo(73.0);
        assertThat(dto.getWorkpieceWallThickness()).isEqualTo(5.5);
        assertThat(dto.getWorkpieceLengthMeters()).isEqualTo(100.0);
        assertThat(dto.getWorkpieceWeightKg()).isEqualTo(946.0);

        // ---- ВАЖНО: common product attrs НЕ заполняются для pipe ----
        assertThat(dto.getProductLengthMm()).isNull();
        assertThat(dto.getProductWeightKg()).isNull();
        assertThat(dto.getProductManufacturingCost()).isNull();
        assertThat(dto.getProductVariantType()).isNull();
        assertThat(dto.getProductAdapterFirstSideType()).isNull();
    }

    // ================================================================
    // NIPPLE
    // ================================================================

    @Test
    void toDto_nipple_mapsAllFields() {
        OrderEntity order = baseOrder()
                .externalThread(threadFixture())
                .pipeProductKind(PipeProductKind.NIPPLE)
                .quantity(50)
                .wallThicknessMm(5.5)
                .orderTotalLengthMeters(100.0)
                .orderTotalWeightKg(946.0)
                .pricePerTon(new BigDecimal("80000.00"))
                .pieceLengthMm(2000.0)
                .build();

        OrderDto dto = mapper.toDto(order);

        assertThat(dto.getProductType()).isEqualTo("NIPPLE");
        assertThat(dto.getProductName()).isEqualTo("Патрубок НКТ 73 L-2000");
        assertThat(dto.getPipeProductKind()).isEqualTo("NIPPLE");
        assertThat(dto.getPieceLengthMm()).isEqualTo(2000.0);

        // averagePipeLengthM — только для PIPE
        assertThat(dto.getAveragePipeLengthM()).isNull();

        // резьба та же самая
        assertThat(dto.getExternalThreadId()).isEqualTo(300L);
        assertThat(dto.getExternalThreadName()).isEqualTo("НКТ 73");
    }

    // ================================================================
    // БЕЗ ПРОДУКТА
    // ================================================================

    @Test
    void toDto_withoutProduct_returnsNullsAndEmptyNames() {
        // Это невалидный по домену заказ, но mapper должен быть null-safe.
        // OrderService никогда такой не создаст, тест страхует от NPE при чтении "грязных" данных.
        OrderEntity order = baseOrder().quantity(0).build();

        OrderDto dto = mapper.toDto(order);

        assertThat(dto.getProductType()).isNull();
        assertThat(dto.getProductName()).isEmpty();
        assertThat(dto.getProductId()).isNull();

        assertThat(dto.getProductVariantType()).isNull();
        assertThat(dto.getProductAdapterFirstSideType()).isNull();
        assertThat(dto.getExternalThreadId()).isNull();
        assertThat(dto.getPipeProductKind()).isNull();

        assertThat(dto.getProductLengthMm()).isNull();
        assertThat(dto.getProductManufacturingCost()).isNull();
    }

    // ================================================================
    // toHomeOrderDto — urgency-классы
    // ================================================================

    @Test
    void toHomeOrderDto_deadlineIn3Days_returnsRedUrgency() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .deadline(today.plusDays(3))
                .build();

        HomeOrderDto dto = mapper.toHomeOrderDto(order, today);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCompanyName()).isEqualTo("ООО Ромашка");
        assertThat(dto.getProductType()).isEqualTo("COUPLING");
        assertThat(dto.getProductId()).isEqualTo(100L);
        assertThat(dto.getProductName()).isEqualTo("Муфта 73");
        assertThat(dto.getDaysUntilDeadline()).isEqualTo(3L);
        assertThat(dto.getUrgencyClass()).isEqualTo("bg-danger text-white");
    }

    @Test
    void toHomeOrderDto_deadlineIn5Days_returnsWarningUrgency() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .deadline(today.plusDays(5))
                .build();

        HomeOrderDto dto = mapper.toHomeOrderDto(order, today);

        assertThat(dto.getDaysUntilDeadline()).isEqualTo(5L);
        assertThat(dto.getUrgencyClass()).isEqualTo("bg-warning");
    }

    @Test
    void toHomeOrderDto_deadlineFarAway_returnsLightUrgency() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .deadline(today.plusDays(10))
                .build();

        HomeOrderDto dto = mapper.toHomeOrderDto(order, today);

        assertThat(dto.getDaysUntilDeadline()).isEqualTo(10L);
        assertThat(dto.getUrgencyClass()).isEqualTo("bg-light");
    }

    @Test
    void toHomeOrderDto_nullDeadline_treatedAsZeroDaysRedUrgency() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .deadline(null)
                .build();

        HomeOrderDto dto = mapper.toHomeOrderDto(order, today);

        assertThat(dto.getDaysUntilDeadline()).isZero();
        assertThat(dto.getUrgencyClass()).isEqualTo("bg-danger text-white");
    }

    // ================================================================
    // toStartedEvent
    // ================================================================

    @Test
    void toStartedEvent_coupling_mapsAllFields() {
        OrderEntity order = baseOrder()
                .coupling(couplingFixture())
                .quantity(10)
                .workpieceOuterDiameter(100.0)
                .workpieceWallThickness(10.0)
                .workpieceLengthMeters(2.0)
                .workpieceWeightKg(15.5)
                .build();

        OrderStartedEvent event = mapper.toStartedEvent(order);

        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getCreatedAt()).isEqualTo(order.getCreatedAt());
        assertThat(event.getCompanyName()).isEqualTo("ООО Ромашка");
        assertThat(event.getProductType()).isEqualTo("COUPLING");
        assertThat(event.getProductName()).isEqualTo("Муфта 73");
        assertThat(event.getProductDetails()).isEqualTo("Муфта 73"); // type + " " + conditionalDiameter
        assertThat(event.getUnitManufacturingCost()).isEqualByComparingTo("1500.00");
        assertThat(event.getWeightKg()).isEqualByComparingTo("15.5");
        assertThat(event.getLengthMm()).isEqualTo(200.0);
        assertThat(event.getImagePath()).isEqualTo("/images/coupling.png");
        assertThat(event.getQuantity()).isEqualTo(10);
        assertThat(event.getDeadline()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(event.getTotalCost()).isEqualByComparingTo("15000.00");

        assertThat(event.getAdditionalWorks()).hasSize(1);
        AdditionalWorkDto work = event.getAdditionalWorks().get(0);
        assertThat(work.getId()).isEqualTo(1L);
        assertThat(work.getName()).isEqualTo("Покраска");
        assertThat(work.getPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    void toStartedEvent_adapter_usesFullNameAsDetails() {
        OrderEntity order = baseOrder()
                .adapter(adapterFixture())
                .quantity(5)
                .build();

        OrderStartedEvent event = mapper.toStartedEvent(order);

        assertThat(event.getProductType()).isEqualTo("ADAPTER");
        assertThat(event.getProductName()).isEqualTo("НКТ 73 x ОТТМ 89");
        assertThat(event.getProductDetails()).isEqualTo("НКТ 73 x ОТТМ 89"); // getFullName()
        assertThat(event.getUnitManufacturingCost()).isEqualByComparingTo("2000.00");
        assertThat(event.getWeightKg()).isEqualByComparingTo("18.0");
        assertThat(event.getLengthMm()).isEqualTo(250.0);
    }

    @Test
    void toStartedEvent_pipe_usesTotalWeightAndNullLength() {
        // Для трубы ProductInfo: weightKg = orderTotalWeightKg, lengthMm = null,
        // productDetails = thread.name, productName = "Труба НКТ 73"
        OrderEntity order = baseOrder()
                .externalThread(threadFixture())
                .pipeProductKind(PipeProductKind.PIPE)
                .quantity(10)
                .orderTotalWeightKg(946.0)
                .build();

        OrderStartedEvent event = mapper.toStartedEvent(order);

        assertThat(event.getProductType()).isEqualTo("PIPE");
        assertThat(event.getProductName()).isEqualTo("Труба НКТ 73");
        assertThat(event.getProductDetails()).isEqualTo("НКТ 73");   // t.getName()
        assertThat(event.getWeightKg()).isEqualByComparingTo("946.0");
        assertThat(event.getLengthMm()).isNull();
        assertThat(event.getImagePath()).isEqualTo("/images/thread.png");
    }

    @Test
    void toStartedEvent_withoutProduct_returnsZeroCostAndEmptyNames() {
        OrderEntity order = baseOrder().quantity(0).build();

        OrderStartedEvent event = mapper.toStartedEvent(order);

        assertThat(event.getProductType()).isNull();
        assertThat(event.getProductName()).isEmpty();
        assertThat(event.getProductDetails()).isEmpty();
        assertThat(event.getUnitManufacturingCost()).isEqualByComparingTo("0");
        assertThat(event.getWeightKg()).isEqualByComparingTo("0");
        assertThat(event.getLengthMm()).isNull();
        assertThat(event.getImagePath()).isNull();
    }
}