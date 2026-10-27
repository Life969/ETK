package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelGradeEntity;
import ProjectForJob.example.Job.domain.entity.handbook.SteelStrengthGroupEntity;
import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderEntity;
import ProjectForJob.example.Job.domain.entity.order.OrderStatus;
import ProjectForJob.example.Job.dto.OrderCreateDto;
import ProjectForJob.example.Job.exception.BusinessException;
import ProjectForJob.example.Job.exception.service.order.OrderErrorCode;
import ProjectForJob.example.Job.repository.handbook.AdditionalWorkRepository;
import ProjectForJob.example.Job.repository.handbook.CouplingRepository;
import ProjectForJob.example.Job.repository.handbook.ExternalThreadRepository;
import ProjectForJob.example.Job.repository.handbook.PipeAdapterRepository;
import ProjectForJob.example.Job.repository.handbook.SteelGradeRepository;
import ProjectForJob.example.Job.repository.handbook.SteelStrengthGroupRepository;
import ProjectForJob.example.Job.repository.order.CompanyRepository;
import ProjectForJob.example.Job.repository.order.OrderRepository;
import ProjectForJob.example.Job.service.mapper.OrderMapper;
import ProjectForJob.example.Job.util.WorkpieceCalculator;
import ProjectForJob.example.Job.dto.OrderUpdateDto;
import ProjectForJob.example.Job.domain.entity.order.PipeProductKind;
import ProjectForJob.example.Job.util.PipeOrderCostCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ProjectForJob.example.Job.util.WorkpieceCalculator.calculateWeight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OrderService}.
 *
 * <p><b>Соглашения файла:</b>
 * <ul>
 *   <li>{@code repo.save(...)} стабим через {@code thenAnswer} — возвращаем тот же объект;</li>
 *   <li>результат createOrder/updateOrder ловим {@code ArgumentCaptor} на
 *       {@code OrderRepository.save} — не зависим от маппера;</li>
 *   <li>вес считаем по той же формуле, что {@code WorkpieceCalculator}:
 *       {@code M = π × 7850 × S × (D − S) × L / 1_000_000};</li>
 *   <li>{@code @MockitoSettings(LENIENT)} — потому что в тестах валидации
 *       сервис падает ДО save, и часть стабов из {@code stubCommonCreate()}
 *       остаётся неиспользованной. Это нормально для «character tests».</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService: бизнес-логика заказов")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CouplingRepository couplingRepository;
    @Mock private AdditionalWorkRepository additionalWorkRepository;
    @Mock private PipeAdapterRepository pipeAdapterRepository;
    @Mock private SteelStrengthGroupRepository strengthGroupRepository;
    @Mock private SteelGradeRepository steelGradeRepository;
    @Mock private ExternalThreadRepository externalThreadRepository;
    @Mock private OrderMapper orderMapper;

    @InjectMocks private OrderService service;


    // ХЕЛПЕРЫ — справочники


    private static final double STEEL_DENSITY = 7850.0;


    private static double expectedWeightKg(double outerMm, double wallMm, double lengthM) {
        return Math.PI * STEEL_DENSITY * wallMm * (outerMm - wallMm) * lengthM / 1_000_000.0;
    }

    private static CouplingEntity coupling(double outer, double inner, double length, String cost) {
        return CouplingEntity.builder()
                .type("Муфта")
                .conditionalDiameter("73")
                .outerDiameterMm(outer)
                .innerDiameterMm(inner)
                .lengthMm(length)
                .manufacturingCost(new BigDecimal(cost))
                .priceForEmployee(new BigDecimal("100"))
                .build();
    }

    private static PipeAdapterEntity adapter(double outer, double inner, double length, String cost) {
        return PipeAdapterEntity.builder()
                .firstSideType("НКТ").firstSideDiameter("73")
                .secondSideType("НКТ").secondSideDiameter("89")
                .outerDiameterMm(outer)
                .innerDiameterMm(inner)
                .lengthMm(length)
                .manufacturingCost(new BigDecimal(cost))
                .priceForEmployee(new BigDecimal("100"))
                .build();
    }

    private static ExternalThreadEntity thread(String conditionalDiameter) {
        return ExternalThreadEntity.builder()
                .type("НКТ")
                .conditionalDiameter(conditionalDiameter)
                .manufacturingCost(new BigDecimal("1000"))
                .priceForEmployee(new BigDecimal("2000"))
                .build();
    }

    private static SteelStrengthGroupEntity group(long id, String code, String markup) {
        return SteelStrengthGroupEntity.builder()
                .id(id)
                .code(code)
                .name("Группа " + code)
                .markupPercent(markup == null ? null : new BigDecimal(markup))
                .isDefault(false)
                .active(true)
                .build();
    }

    private static SteelGradeEntity grade(long id, String name, SteelStrengthGroupEntity g) {
        return SteelGradeEntity.builder()
                .id(id)
                .name(name)
                .strengthGroup(g)
                .active(true)
                .build();
    }

    private static AdditionalWorkEntity work(String name, String price) {
        return AdditionalWorkEntity.builder()
                .name(name)
                .price(new BigDecimal(price))
                .build();
    }


    // ХЕЛПЕРЫ — DTO (OrderCreateDto — @Data без @Builder)


    private static OrderCreateDto dto(String productType) {
        OrderCreateDto d = new OrderCreateDto();
        d.setCompanyName("ООО Тест");
        d.setProductType(productType);
        d.setAdditionalWorkIds(new ArrayList<>());
        return d;
    }

    private static OrderCreateDto couplingDto(Long couplingId, Integer qty) {
        OrderCreateDto d = dto("COUPLING");
        d.setCouplingId(couplingId);
        d.setQuantity(qty);
        return d;
    }


    // ХЕЛПЕРЫ — моки и захват сохранённой сущности


    /** Стабим базовый флоу: компания создаётся, order.save возвращает тот же объект. */
    private void stubCommonCreate() {
        when(companyRepository.findByNameIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(companyRepository.save(any(CompanyEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Ловим сущность, которую сервис сохранил через {@code orderRepository.save}. */
    private OrderEntity captureSaved(Runnable action) {
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        action.run();
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }



    @Nested
    @DisplayName("Валидация productType")
    class ProductTypeValidation {

        @Test
        @DisplayName("productType = null → PRODUCT_TYPE_MISSING")
        void null_product_type_throws() {
            // DTO-валидация @NotNull не сработает без @Valid в контроллере,
            // поэтому сервис должен сам защититься. Проверяем, что защищён.
            stubCommonCreate();
            OrderCreateDto d = dto(null);

            assertThatThrownBy(() -> service.createOrder(d))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Тип продукции")
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PRODUCT_TYPE_MISSING);
        }

        @Test
        @DisplayName("Неизвестный productType → PRODUCT_TYPE_INVALID")
        void invalid_product_type_throws() {
            stubCommonCreate();
            OrderCreateDto d = dto("WIDGET");

            assertThatThrownBy(() -> service.createOrder(d))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("WIDGET")
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PRODUCT_TYPE_INVALID);
        }
    }



    @Nested
    @DisplayName("Создание муфты (COUPLING) — smoke")
    class CouplingSmoke {

        @Test
        @DisplayName("Хэппи-пас: workpiece_* и quantity заполнены из справочника")
        void happy_path_fills_workpiece() {
            stubCommonCreate();
            CouplingEntity c = coupling(100.0, 80.0, 100.0, "500");
            when(couplingRepository.findById(1L)).thenReturn(Optional.of(c));

            OrderEntity saved = captureSaved(() -> service.createOrder(couplingDto(1L, 10)));

            double wall = (100.0 - 80.0) / 2.0;      // 10.0 мм
            double totalLenM = 100.0 * 10 / 1000.0;  // 1.0 м
            double expectedW = calculateWeight(100.0, wall, totalLenM);

            assertThat(saved.getCoupling()).as("coupling").isSameAs(c);
            assertThat(saved.getQuantity()).as("quantity").isEqualTo(10);
            assertThat(saved.getWorkpieceOuterDiameter()).as("outer diameter").isEqualTo(100.0);
            assertThat(saved.getWorkpieceWallThickness()).as("wall").isEqualTo(wall);
            assertThat(saved.getWorkpieceLengthMeters()).as("length").isEqualTo(totalLenM);
            assertThat(saved.getWorkpieceWeightKg()).as("weight").isCloseTo(expectedW, within(1e-9));
            assertThat(saved.getStatus()).as("status").isEqualTo(OrderStatus.WAITING);
            assertThat(saved.getCreatedAt()).as("createdAt").isNotNull();
        }

        @Test
        @DisplayName("totalCost = manufacturingCost × quantity (без надбавки и работ)")
        void total_cost_uses_unit_manufacturing_cost() {
            stubCommonCreate();
            // unitManufacturingCost — деривация: getCoupling().getManufacturingCost().
            // Значит 500 × 20 = 10 000. Никаких дыр в базовом расчёте.
            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));

            OrderEntity saved = captureSaved(() -> service.createOrder(couplingDto(1L, 20)));

            assertThat(saved.getTotalCost())
                    .as("baseCost = manufacturingCost × quantity = 500 × 20")
                    .isEqualByComparingTo("10000.00");
        }


    }



    @Nested
    @DisplayName("Создание муфты (COUPLING) — guards")
    class CouplingGuards {

        @Test
        @DisplayName("couplingId = null → COUPLING_NOT_SELECTED")
        void null_coupling_id_throws() {
            stubCommonCreate();
            // productType задан, но сам продукт не выбран. Сервис должен
            // упасть в applyCoupling ещё до похода в репозиторий.
            OrderCreateDto d = couplingDto(null, 10);

            assertThatThrownBy(() -> service.createOrder(d))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.COUPLING_NOT_SELECTED);
        }

        @Test
        @DisplayName("quantity = 0 → QUANTITY_NOT_POSITIVE")
        void zero_quantity_throws() {
            stubCommonCreate();
            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));

            assertThatThrownBy(() -> service.createOrder(couplingDto(1L, 0)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }

        @Test
        @DisplayName("quantity = null → QUANTITY_NOT_POSITIVE (без NPE при unboxing)")
        void null_quantity_throws() {
            stubCommonCreate();
            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));

            assertThatThrownBy(() -> service.createOrder(couplingDto(1L, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }

        @Test
        @DisplayName("quantity < 0 → QUANTITY_NOT_POSITIVE")
        void negative_quantity_throws() {
            stubCommonCreate();
            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));

            assertThatThrownBy(() -> service.createOrder(couplingDto(1L, -5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Муфта не найдена → COUPLING_NOT_FOUND")
        void coupling_not_found_throws() {
            stubCommonCreate();
            when(couplingRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(couplingDto(1L, 10)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.COUPLING_NOT_FOUND);
        }
    }


    //      ADAPTER: happy path + guards


    @Nested
    @DisplayName("Создание переводника (ADAPTER)")
    class AdapterCreation {

        /** Хелпер: DTO для переводника. */
        private OrderCreateDto adapterDto(Long adapterId, Integer qty) {
            OrderCreateDto d = dto("ADAPTER");
            d.setAdapterId(adapterId);
            d.setQuantity(qty);
            return d;
        }

        @Test
        @DisplayName("Хэппи-пас: workpiece_*, quantity и totalCost из manufacturingCost")
        void happy_path_fills_workpiece_and_cost() {
            stubCommonCreate();
            PipeAdapterEntity a = adapter(114.0, 90.0, 200.0, "750");
            when(pipeAdapterRepository.findById(2L)).thenReturn(Optional.of(a));

            OrderEntity saved = captureSaved(() -> service.createOrder(adapterDto(2L, 5)));

            // wall = (114 - 90) / 2 = 12 мм; L = 200 * 5 / 1000 = 1.0 м
            double wall = (114.0 - 90.0) / 2.0;
            double totalLenM = 200.0 * 5 / 1000.0;
            double expectedW = calculateWeight(114.0, wall, totalLenM);

            assertThat(saved.getAdapter()).as("adapter").isSameAs(a);
            assertThat(saved.getQuantity()).as("quantity").isEqualTo(5);
            assertThat(saved.getWorkpieceOuterDiameter()).as("D").isEqualTo(114.0);
            assertThat(saved.getWorkpieceWallThickness()).as("wall").isEqualTo(wall);
            assertThat(saved.getWorkpieceLengthMeters()).as("len").isEqualTo(totalLenM);
            assertThat(saved.getWorkpieceWeightKg()).as("weight").isCloseTo(expectedW, within(1e-9));
            assertThat(saved.getStatus()).as("status").isEqualTo(OrderStatus.WAITING);

            // totalCost = manufacturingCost × quantity = 750 × 5 = 3750.00
            // (без надбавки, без доп. работ — только база)
            assertThat(saved.getTotalCost())
                    .as("baseCost = 750 × 5")
                    .isEqualByComparingTo("3750.00");
        }

        @Test
        @DisplayName("adapterId = null → ADAPTER_NOT_SELECTED")
        void null_adapter_id_throws() {
            stubCommonCreate();

            assertThatThrownBy(() -> service.createOrder(adapterDto(null, 1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ADAPTER_NOT_SELECTED);
        }

        @Test
        @DisplayName("quantity = 0 → QUANTITY_NOT_POSITIVE")
        void zero_quantity_throws() {
            stubCommonCreate();
            when(pipeAdapterRepository.findById(2L))
                    .thenReturn(Optional.of(adapter(114.0, 90.0, 200.0, "750")));

            assertThatThrownBy(() -> service.createOrder(adapterDto(2L, 0)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }

        @Test
        @DisplayName("quantity = null → QUANTITY_NOT_POSITIVE")
        void null_quantity_throws() {
            stubCommonCreate();
            when(pipeAdapterRepository.findById(2L))
                    .thenReturn(Optional.of(adapter(114.0, 90.0, 200.0, "750")));

            assertThatThrownBy(() -> service.createOrder(adapterDto(2L, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Переводник не найден → ADAPTER_NOT_FOUND")
        void adapter_not_found_throws() {
            stubCommonCreate();
            when(pipeAdapterRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(adapterDto(2L, 1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ADAPTER_NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("Создание трубы (PIPE)")
    class PipeCreation {

        /** Хелпер: DTO для PIPE. Меняем только то, что нужно конкретному тесту. */
        private OrderCreateDto pipeDto(Long threadId, double wall, Double avgLen,
                                       Double totalLen, Double totalWeight, String pricePerTon) {
            OrderCreateDto d = dto("PIPE");
            d.setExternalThreadId(threadId);
            d.setWallThicknessMm(wall);
            d.setAveragePipeLengthM(avgLen);
            d.setOrderTotalLengthMeters(totalLen);
            d.setOrderTotalWeightKg(totalWeight);
            d.setPricePerTon(pricePerTon == null ? null : new BigDecimal(pricePerTon));
            return d;
        }

        @Test
        @DisplayName("Задан метраж → вес через WorkpieceCalculator, quantity = ceil(len / avgLen)")
        void by_length_computes_weight_and_quantity() {
            stubCommonCreate();
            ExternalThreadEntity t = thread("73");       // manufacturingCost = 1000
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(t));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, "50000")));

            // Вес считает утилита (с её округлением до 2 знаков), не формула теста.
            double expectedW = calculateWeight(73.0, 5.5, 100.0);

            assertThat(saved.getPipeProductKind()).as("kind").isEqualTo(PipeProductKind.PIPE);
            assertThat(saved.getExternalThread()).as("thread").isSameAs(t);
            assertThat(saved.getWallThicknessMm()).as("wall").isEqualTo(5.5);
            assertThat(saved.getPricePerTon()).as("pricePerTon").isEqualByComparingTo("50000");
            assertThat(saved.getAveragePipeLengthM()).as("avgLen").isEqualTo(10.0);
            assertThat(saved.getOrderTotalLengthMeters()).as("len").isEqualTo(100.0);
            assertThat(saved.getOrderTotalWeightKg()).as("weight").isCloseTo(expectedW, within(1e-9));
            assertThat(saved.getQuantity()).as("quantity = ceil(100/10)").isEqualTo(10);
            assertThat(saved.getWorkpieceOuterDiameter()).as("workpiece D").isEqualTo(73.0);
            assertThat(saved.getWorkpieceWallThickness()).as("workpiece wall").isEqualTo(5.5);
            assertThat(saved.getWorkpieceLengthMeters()).as("workpiece len").isEqualTo(100.0);
        }

        @Test
        @DisplayName("PIPE: totalCost идёт через PipeOrderCostCalculator (threads + metal)")
        void total_cost_uses_pipe_calculator() {
            stubCommonCreate();
            ExternalThreadEntity t = thread("73");
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(t));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, "50000")));

            // Ожидание считаем тем же калькулятором, что и сервис.
            // Если сервис случайно ушёл в OrderCostCalculator (COUPLING-ветку),
            // получилось бы unitCost × Q = 0 × 10 = 0 — тест упал бы.
            BigDecimal expected = PipeOrderCostCalculator.calculate(
                    t.getManufacturingCost(),                    // 1000
                    new BigDecimal("50000"),                     // pricePerTon
                    saved.getQuantity(),                         // 10
                    saved.getOrderTotalWeightKg(),               // ≈ 915.56
                    saved.getAdditionalWorks(),                  // []
                    saved.getAppliedMarkupPercent());            // 0

            assertThat(saved.getTotalCost()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Задан тоннаж → метраж через lengthFromWeight, quantity = ceil(L / avgLen)")
        void by_weight_computes_length() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, null, 1000.0, "50000")));

            // L = M × 1e6 / (π × ρ × S × (D − S))
            double denom = Math.PI * 7850.0 * 5.5 * (73.0 - 5.5) / 1_000_000.0;
            double expectedL = 1000.0 / denom;             // ≈ 109.22 м

            assertThat(saved.getOrderTotalWeightKg()).as("weight = M").isEqualTo(1000.0);
            assertThat(saved.getOrderTotalLengthMeters()).as("len").isCloseTo(expectedL, within(1e-6));
            assertThat(saved.getQuantity()).as("quantity = ceil(109.22/10)")
                    .isEqualTo(11);
            assertThat(saved.getPipeProductKind()).as("kind").isEqualTo(PipeProductKind.PIPE);
        }

        @Test
        @DisplayName("avgLength = null → дефолт 10 м")
        void null_avg_length_uses_default() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    pipeDto(3L, 5.5, null, 50.0, null, "50000")));

            assertThat(saved.getAveragePipeLengthM()).as("avgLen default").isEqualTo(10.0);
            assertThat(saved.getQuantity()).as("quantity = ceil(50/10)").isEqualTo(5);
        }

        @Test
        @DisplayName("avgLength ≤ 0 → тоже дефолт 10 м")
        void non_positive_avg_length_falls_back_to_default() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    pipeDto(3L, 5.5, 0.0, 50.0, null, "50000")));

            assertThat(saved.getAveragePipeLengthM()).as("avgLen default").isEqualTo(10.0);
        }

        @Test
        @DisplayName("quantity из DTO для PIPE игнорируется — считается через avgLength")
        void quantity_from_dto_ignored() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderCreateDto d = pipeDto(3L, 5.5, 10.0, 100.0, null, "50000");
            d.setQuantity(999);   // ← шум, должен быть перетёрт

            OrderEntity saved = captureSaved(() -> service.createOrder(d));

            assertThat(saved.getQuantity())
                    .as("quantity = ceil(100/10), а не 999 из DTO")
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("Нет ни метража, ни тоннажа → PIPE_LENGTH_OR_WEIGHT_REQUIRED")
        void no_length_no_weight_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, null, null, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PIPE_LENGTH_OR_WEIGHT_REQUIRED);
        }

        @Test
        @DisplayName("wallThicknessMm = null → WALL_THICKNESS_REQUIRED")
        void null_wall_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderCreateDto d = pipeDto(3L, 0.0, 10.0, 100.0, null, "50000");
            d.setWallThicknessMm(null);

            assertThatThrownBy(() -> service.createOrder(d))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.WALL_THICKNESS_REQUIRED);
        }

        @Test
        @DisplayName("pricePerTon = null → PRICE_PER_TON_REQUIRED")
        void null_price_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PRICE_PER_TON_REQUIRED);
        }

        @Test
        @DisplayName("pricePerTon = 0 → PRICE_PER_TON_REQUIRED")
        void zero_price_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, "0")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PRICE_PER_TON_REQUIRED);
        }

        @Test
        @DisplayName("Резьба не найдена → THREAD_NOT_FOUND")
        void thread_not_found_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.THREAD_NOT_FOUND);
        }

        @Test
        @DisplayName("Диаметр резьбы нечисловой → THREAD_DIAMETER_PARSE_ERROR")
        void bad_thread_diameter_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L))
                    .thenReturn(Optional.of(thread("НКТ")));

            assertThatThrownBy(() -> service.createOrder(
                    pipeDto(3L, 5.5, 10.0, 100.0, null, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.THREAD_DIAMETER_PARSE_ERROR);
        }
    }


    @Nested
    @DisplayName("Создание патрубка (NIPPLE)")
    class NippleCreation {

        /** Хелпер: DTO для NIPPLE. */
        private OrderCreateDto nippleDto(Long threadId, Integer qty, Double pieceLen,
                                         double wall, String pricePerTon) {
            OrderCreateDto d = dto("NIPPLE");
            d.setExternalThreadId(threadId);
            d.setQuantity(qty);
            d.setPieceLengthMm(pieceLen);
            d.setWallThicknessMm(wall);
            d.setPricePerTon(new BigDecimal(pricePerTon));
            return d;
        }

        @Test
        @DisplayName("Хэппи-пас: totalLen = pieceLengthMm × Q / 1000, quantity из DTO")
        void happy_path() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    nippleDto(3L, 20, 500.0, 5.5, "50000")));

            double expectedLen = 500.0 * 20 / 1000.0;    // 10.0 м
            double expectedW = calculateWeight(73.0, 5.5, expectedLen);

            assertThat(saved.getPipeProductKind()).as("kind").isEqualTo(PipeProductKind.NIPPLE);
            assertThat(saved.getQuantity()).as("quantity").isEqualTo(20);
            assertThat(saved.getPieceLengthMm()).as("pieceLength").isEqualTo(500.0);
            assertThat(saved.getOrderTotalLengthMeters()).as("len").isEqualTo(expectedLen);
            assertThat(saved.getOrderTotalWeightKg()).as("weight").isCloseTo(expectedW, within(1e-9));
            assertThat(saved.getAveragePipeLengthM()).as("avgLen для NIPPLE не задан").isNull();
        }

        @Test
        @DisplayName("pieceLengthMm = null → NIPPLE_LENGTH_REQUIRED")
        void null_piece_length_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    nippleDto(3L, 10, null, 5.5, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.NIPPLE_LENGTH_REQUIRED);
        }

        @Test
        @DisplayName("pieceLengthMm ≤ 0 → NIPPLE_LENGTH_REQUIRED")
        void non_positive_piece_length_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    nippleDto(3L, 10, 0.0, 5.5, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.NIPPLE_LENGTH_REQUIRED);
        }

        @Test
        @DisplayName("quantity = 0 → QUANTITY_NOT_POSITIVE")
        void zero_quantity_throws() {
            stubCommonCreate();
            when(externalThreadRepository.findById(3L)).thenReturn(Optional.of(thread("73")));

            assertThatThrownBy(() -> service.createOrder(
                    nippleDto(3L, 0, 500.0, 5.5, "50000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.QUANTITY_NOT_POSITIVE);
        }
    }



    @Nested
    @DisplayName("Группа прочности + марка (snapshot надбавки)")
    class StrengthAndGrade {

        /**
         * Хелпер: COUPLING-заказ с готовым продуктом в моке.
         * Используем муфту — она самая простая, нас интересует только
         * appliedMarkupPercent и его влияние на totalCost.
         */
        private OrderCreateDto couplingWith(long groupId, long gradeId, Long qty) {
            OrderCreateDto d = couplingDto(1L, qty.intValue());
            d.setStrengthGroupId(groupId == 0 ? null : groupId);
            d.setSteelGradeId(gradeId == 0 ? null : gradeId);
            return d;
        }

        private void stubCoupling() {
            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));
        }

        @Test
        @DisplayName("Оба null → группа/марка null, appliedMarkupPercent = 0")
        void both_null_no_markup() {
            stubCommonCreate();
            stubCoupling();

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(0L, 0L, 1L)));

            assertThat(saved.getStrengthGroup()).as("group").isNull();
            assertThat(saved.getSteelGrade()).as("grade").isNull();
            assertThat(saved.getAppliedMarkupPercent()).as("markup").isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Только группа → марка null, appliedMarkup = группы.markupPercent")
        void only_group() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g = group(1L, "D", "10");
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.of(g));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(1L, 0L, 1L)));

            assertThat(saved.getStrengthGroup()).as("group").isSameAs(g);
            assertThat(saved.getSteelGrade()).as("grade").isNull();
            assertThat(saved.getAppliedMarkupPercent()).as("markup").isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("Только марка → группа подтягивается из марки")
        void only_grade_pulls_group() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g = group(1L, "D", "15");
            SteelGradeEntity gr = grade(2L, "09Г2С", g);
            when(steelGradeRepository.findById(2L)).thenReturn(Optional.of(gr));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(0L, 2L, 1L)));

            assertThat(saved.getStrengthGroup()).as("group from grade").isSameAs(g);
            assertThat(saved.getSteelGrade()).as("grade").isSameAs(gr);
            assertThat(saved.getAppliedMarkupPercent()).as("markup").isEqualByComparingTo("15");
        }

        @Test
        @DisplayName("Оба заданы + марка из этой группы → ОК")
        void both_valid() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g = group(1L, "D", "12");
            SteelGradeEntity gr = grade(2L, "09Г2С", g);
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.of(g));
            when(steelGradeRepository.findById(2L)).thenReturn(Optional.of(gr));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(1L, 2L, 1L)));

            assertThat(saved.getStrengthGroup()).as("group").isSameAs(g);
            assertThat(saved.getSteelGrade()).as("grade").isSameAs(gr);
            assertThat(saved.getAppliedMarkupPercent()).as("markup").isEqualByComparingTo("12");
        }

        @Test
        @DisplayName("Марка из другой группы → GRADE_NOT_IN_GROUP")
        void grade_not_in_group_throws() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g1 = group(1L, "D", "10");
            SteelStrengthGroupEntity g2 = group(2L, "K", "20");
            SteelGradeEntity gr = grade(3L, "09Г2С", g2);
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.of(g1));
            when(steelGradeRepository.findById(3L)).thenReturn(Optional.of(gr));

            assertThatThrownBy(() -> service.createOrder(couplingWith(1L, 3L, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.GRADE_NOT_IN_GROUP);
        }

        @Test
        @DisplayName("Группа не найдена → GROUP_NOT_FOUND")
        void group_not_found_throws() {
            stubCommonCreate();
            stubCoupling();
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(couplingWith(1L, 0L, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("Марка не найдена → GRADE_NOT_FOUND")
        void grade_not_found_throws() {
            stubCommonCreate();
            stubCoupling();
            when(steelGradeRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(couplingWith(0L, 2L, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.GRADE_NOT_FOUND);
        }

        @Test
        @DisplayName("markupPercent = null → appliedMarkup = 0 (защита от NPE)")
        void null_markup_becomes_zero() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g = group(1L, "D", null);
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.of(g));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(1L, 0L, 1L)));

            assertThat(saved.getAppliedMarkupPercent()).as("markup").isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("ИНТЕГРАЦИЯ: надбавка доезжает до totalCost (500×20 + 10% = 11000)")
        void markup_lands_in_total_cost() {
            stubCommonCreate();
            stubCoupling();
            SteelStrengthGroupEntity g = group(1L, "D", "10");
            when(strengthGroupRepository.findById(1L)).thenReturn(Optional.of(g));

            OrderEntity saved = captureSaved(() -> service.createOrder(
                    couplingWith(1L, 0L, 20L)));

            // baseCost = 500 × 20 = 10 000
            // markupCost = 10 000 × 10% = 1 000
            // productTotal = 11 000
            // работ нет → totalCost = 11 000.00
            assertThat(saved.getAppliedMarkupPercent()).isEqualByComparingTo("10");
            assertThat(saved.getTotalCost()).isEqualByComparingTo("11000.00");
        }
    }



    @Nested
    @DisplayName("Статусы заказа (startProduction)")
    class StatusLifecycle {

        @Test
        @DisplayName("startProduction из WAITING → IN_PRODUCTION + дедлайн установлен")
        void start_from_waiting() {
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.WAITING).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalDate deadline = LocalDate.of(2025, 6, 1);
            service.startProduction(1L, deadline);

            assertThat(order.getStatus()).as("status").isEqualTo(OrderStatus.IN_PRODUCTION);
            assertThat(order.getDeadline()).as("deadline").isEqualTo(deadline);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("startProduction из IN_PRODUCTION → ORDER_NOT_WAITING")
        void start_from_production_throws() {
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.IN_PRODUCTION).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.startProduction(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_WAITING);
        }

        @Test
        @DisplayName("startProduction из COMPLETED → ORDER_NOT_WAITING")
        void start_from_completed_throws() {
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.COMPLETED).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.startProduction(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_WAITING);
        }

        @Test
        @DisplayName("startProduction без дедлайна — старый дедлайн не трогаем")
        void start_without_deadline_preserves_old() {
            LocalDate original = LocalDate.of(2026, 5, 1);
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.WAITING).deadline(original).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            service.startProduction(1L, null);

            assertThat(order.getDeadline()).as("deadline").isEqualTo(original);
            assertThat(order.getStatus()).as("status").isEqualTo(OrderStatus.IN_PRODUCTION);
        }

        @Test
        @DisplayName("startProduction: заказ не найден → ORDER_NOT_FOUND")
        void start_not_found_throws() {
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startProduction(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("Редактирование заказа (updateOrder)")
    class UpdateOrder {

        /** Хелпер: пустой update-DTO. */
        private OrderUpdateDto updateDto(String productType) {
            OrderUpdateDto d = new OrderUpdateDto();
            d.setCompanyName("ООО Тест");
            d.setProductType(productType);
            d.setAdditionalWorkIds(new ArrayList<>());
            return d;
        }

        @Test
        @DisplayName("Заказ не найден → ORDER_NOT_FOUND")
        void update_not_found_throws() {
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateOrder(1L, updateDto("COUPLING")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("productType = null → PRODUCT_TYPE_MISSING (до похода в репо продуктов)")
        void null_product_type_throws() {
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.WAITING).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(companyRepository.findByNameIgnoreCase("ООО Тест"))
                    .thenReturn(Optional.of(CompanyEntity.builder().name("ООО Тест").build()));

            assertThatThrownBy(() ->
                    service.updateOrder(1L, updateDto(null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.PRODUCT_TYPE_MISSING);
        }

        @Test
        @DisplayName("Смена типа COUPLING → ADAPTER: старые поля чистятся, новые заполнены")
        void switch_coupling_to_adapter_clears_old() {
            CompanyEntity comp = CompanyEntity.builder().name("ООО Тест").build();
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.WAITING).company(comp)
                    .coupling(coupling(100.0, 80.0, 100.0, "500"))
                    .quantity(10)
                    .workpieceOuterDiameter(100.0)
                    .workpieceWallThickness(10.0)
                    .workpieceLengthMeters(1.0)
                    .workpieceWeightKg(22.2)
                    .additionalWorks(new ArrayList<>())
                    .build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(companyRepository.findByNameIgnoreCase("ООО Тест"))
                    .thenReturn(Optional.of(comp));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            PipeAdapterEntity a = adapter(114.0, 90.0, 200.0, "750");
            when(pipeAdapterRepository.findById(2L)).thenReturn(Optional.of(a));

            OrderUpdateDto d = updateDto("ADAPTER");
            d.setAdapterId(2L);
            d.setQuantity(3);

            service.updateOrder(1L, d);

            verify(orderRepository).save(order);

            assertThat(order.getCoupling()).as("coupling сброшен").isNull();
            assertThat(order.getAdapter()).as("adapter установлен").isSameAs(a);
            assertThat(order.getQuantity()).as("quantity").isEqualTo(3);

            double wall = (114.0 - 90.0) / 2.0;
            double len = 200.0 * 3 / 1000.0;
            assertThat(order.getWorkpieceOuterDiameter()).as("workpiece D").isEqualTo(114.0);
            assertThat(order.getWorkpieceWallThickness()).as("workpiece wall").isEqualTo(wall);
            assertThat(order.getWorkpieceLengthMeters()).as("workpiece len").isEqualTo(len);
            assertThat(order.getWorkpieceWeightKg())
                    .as("workpiece weight").isCloseTo(calculateWeight(114.0, wall, len), within(1e-9));
        }

        @Test
        @DisplayName("update сбрасывает strengthGroup/grade, если они стали null")
        void update_clears_strength_and_grade_when_null() {
            SteelStrengthGroupEntity g = group(1L, "D", "10");
            OrderEntity order = OrderEntity.builder()
                    .id(1L).status(OrderStatus.WAITING)
                    .company(CompanyEntity.builder().name("ООО Тест").build())
                    .coupling(coupling(100.0, 80.0, 100.0, "500"))
                    .quantity(5)
                    .strengthGroup(g)
                    .appliedMarkupPercent(new BigDecimal("10"))
                    .additionalWorks(new ArrayList<>())
                    .build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(companyRepository.findByNameIgnoreCase("ООО Тест"))
                    .thenReturn(Optional.of(order.getCompany()));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));


            when(couplingRepository.findById(1L))
                    .thenReturn(Optional.of(coupling(100.0, 80.0, 100.0, "500")));

            OrderUpdateDto d = updateDto("COUPLING");
            d.setCouplingId(1L);
            d.setQuantity(5);
            // strengthGroupId и steelGradeId — null

            service.updateOrder(1L, d);

            assertThat(order.getStrengthGroup()).as("group сброшен").isNull();
            assertThat(order.getSteelGrade()).as("grade сброшен").isNull();
            assertThat(order.getAppliedMarkupPercent()).as("markup сброшен").isEqualByComparingTo("0");
        }
    }


    @Nested
    @DisplayName("Базовые CRUD-запросы")
    class OrderQueries {

        @Test
        @DisplayName("getOrderById: не найден → ORDER_NOT_FOUND")
        void get_by_id_not_found() {
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrderById(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("deleteOrder: не найден → ORDER_NOT_FOUND")
        void delete_not_found() {
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("deleteOrder: существующий заказ удаляется")
        void delete_existing() {
            OrderEntity order = OrderEntity.builder().id(1L).build();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            service.deleteOrder(1L);

            verify(orderRepository).delete(order);
        }
    }
}