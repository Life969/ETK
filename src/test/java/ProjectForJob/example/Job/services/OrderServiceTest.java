package ProjectForJob.example.Job.services;

import static org.junit.jupiter.api.Assertions.*;

import ProjectForJob.example.Job.DataTransferObject.HomeOrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderCreateDto;
import ProjectForJob.example.Job.DataTransferObject.OrderDto;
import ProjectForJob.example.Job.DataTransferObject.OrderUpdateDto;
import ProjectForJob.example.Job.entityJob.*;
import ProjectForJob.example.Job.repositories.AdditionalWorkRepository;
import ProjectForJob.example.Job.repositories.CompanyRepository;
import ProjectForJob.example.Job.repositories.CouplingRepository;
import ProjectForJob.example.Job.repositories.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сервис заказов (OrderService)")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CouplingRepository couplingRepository;
    @Mock private AdditionalWorkRepository additionalWorkRepository;

    @InjectMocks private OrderService orderService;

    // ==================== ТЕСТОВЫЕ ДАННЫЕ ====================
    private static final Long ORDER_ID = 1L;
    private static final Long COUPLING_ID = 100L;
    private static final Long ADD_WORK_ID_1 = 200L;
    private static final Long ADD_WORK_ID_2 = 201L;
    private static final String COMPANY_NAME = "ООО Ромашка";
    private static final String NEW_COMPANY_NAME = "Новая компания";
    private static final int QUANTITY = 5;
    private static final BigDecimal MANUFACTURING_COST = BigDecimal.valueOf(1000);
    private static final BigDecimal ADD_WORK_PRICE_1 = BigDecimal.valueOf(150);
    private static final BigDecimal ADD_WORK_PRICE_2 = BigDecimal.valueOf(250);
    private static final LocalDate DEADLINE = LocalDate.now().plusDays(10);

    private CouplingEntity createCoupling() {
        return CouplingEntity.builder()
                .id(COUPLING_ID)
                .type("Муфта А")
                .manufacturingCost(MANUFACTURING_COST)
                .build();
    }

    private CompanyEntity createCompany(String name, Long id) {
        return CompanyEntity.builder()
                .id(id)
                .name(name)
                .build();
    }

    private CompanyEntity createCompany(String name) {
        return createCompany(name, 1L);
    }

    private AdditionalWorkEntity createAddWork(Long id, String name, BigDecimal price) {
        return AdditionalWorkEntity.builder()
                .id(id)
                .name(name)
                .price(price)
                .build();
    }

    private OrderEntity createOrderEntity(OrderStatus status) {
        return OrderEntity.builder()
                .id(ORDER_ID)
                .createdAt(LocalDateTime.now())
                .company(createCompany(COMPANY_NAME))
                .coupling(createCoupling())
                .quantity(QUANTITY)
                .deadline(DEADLINE)
                .status(status)
                .additionalWorks(List.of(
                        createAddWork(ADD_WORK_ID_1, "Покраска", ADD_WORK_PRICE_1),
                        createAddWork(ADD_WORK_ID_2, "Упаковка", ADD_WORK_PRICE_2)
                ))
                .totalCost(calculateExpectedTotalCost())
                .build();
    }

    private BigDecimal calculateExpectedTotalCost() {
        BigDecimal addWorkSum = ADD_WORK_PRICE_1.add(ADD_WORK_PRICE_2);
        return MANUFACTURING_COST.add(addWorkSum).multiply(BigDecimal.valueOf(QUANTITY));
    }

    private OrderCreateDto createOrderCreateDto(String companyName, Long couplingId, List<Long> addWorkIds) {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setCompanyName(companyName);
        dto.setCouplingId(couplingId);
        dto.setQuantity(QUANTITY);
        dto.setDeadline(DEADLINE);
        dto.setAdditionalWorkIds(addWorkIds);
        return dto;
    }

    // ==================== ТЕСТЫ ====================

    @Test
    @DisplayName("createOrder: компания существует, доп. работы есть → заказ создаётся с корректным расчётом")
    void createOrder_whenCompanyExistsAndAddWorks_shouldSaveAndReturnDto() {
        // given
        OrderCreateDto dto = createOrderCreateDto(COMPANY_NAME, COUPLING_ID, List.of(ADD_WORK_ID_1, ADD_WORK_ID_2));
        CompanyEntity existingCompany = createCompany(COMPANY_NAME);
        CouplingEntity coupling = createCoupling();
        List<AdditionalWorkEntity> addWorks = List.of(
                createAddWork(ADD_WORK_ID_1, "Покраска", ADD_WORK_PRICE_1),
                createAddWork(ADD_WORK_ID_2, "Упаковка", ADD_WORK_PRICE_2)
        );

        when(companyRepository.findByNameIgnoreCase(COMPANY_NAME)).thenReturn(Optional.of(existingCompany));
        when(couplingRepository.findById(COUPLING_ID)).thenReturn(Optional.of(coupling));
        when(additionalWorkRepository.findAllById(List.of(ADD_WORK_ID_1, ADD_WORK_ID_2))).thenReturn(addWorks);

        OrderEntity savedOrder = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        // when
        OrderDto result = orderService.createOrder(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getCompanyName()).isEqualTo(COMPANY_NAME);
        assertThat(result.getTotalCost()).isEqualByComparingTo(calculateExpectedTotalCost());

        // Проверяем, что в сохраняемую сущность проставлены правильные поля
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity captured = captor.getValue();

        assertThat(captured.getCompany()).isSameAs(existingCompany);
        assertThat(captured.getCoupling()).isSameAs(coupling);
        assertThat(captured.getQuantity()).isEqualTo(QUANTITY);
        assertThat(captured.getDeadline()).isEqualTo(DEADLINE);
        assertThat(captured.getAdditionalWorks()).containsExactlyElementsOf(addWorks);
        assertThat(captured.getStatus()).isEqualTo(OrderStatus.WAITING);
        assertThat(captured.getCreatedAt()).isNotNull();
        assertThat(captured.getTotalCost()).isEqualByComparingTo(calculateExpectedTotalCost());

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: компания не найдена → создаётся новая компания")
    void createOrder_whenCompanyNotFound_shouldCreateNewCompany() {
        // given
        OrderCreateDto dto = createOrderCreateDto(NEW_COMPANY_NAME, COUPLING_ID, null);
        CouplingEntity coupling = createCoupling();
        CompanyEntity newCompany = createCompany(NEW_COMPANY_NAME, 2L); // id=2, чтобы отличить

        when(companyRepository.findByNameIgnoreCase(NEW_COMPANY_NAME)).thenReturn(Optional.empty());
        when(companyRepository.save(any(CompanyEntity.class))).thenReturn(newCompany);
        when(couplingRepository.findById(COUPLING_ID)).thenReturn(Optional.of(coupling));


        // Создаём сущность заказа, которая будет возвращена после сохранения (с новой компанией)
        OrderEntity savedOrder = OrderEntity.builder()
                .id(ORDER_ID)
                .createdAt(LocalDateTime.now())
                .company(newCompany)  // ← новая компания
                .coupling(coupling)
                .quantity(QUANTITY)
                .deadline(DEADLINE)
                .status(OrderStatus.WAITING)
                .additionalWorks(List.of())
                .totalCost(MANUFACTURING_COST.multiply(BigDecimal.valueOf(QUANTITY)))
                .build();
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        // when
        OrderDto result = orderService.createOrder(dto);

        // then
        assertThat(result.getCompanyName()).isEqualTo(NEW_COMPANY_NAME);
        assertThat(result.getId()).isEqualTo(ORDER_ID);

        ArgumentCaptor<CompanyEntity> companyCaptor = ArgumentCaptor.forClass(CompanyEntity.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getName()).isEqualTo(NEW_COMPANY_NAME.trim());
    }

    @Test
    @DisplayName("createOrder: продукция не найдена → исключение")
    void createOrder_whenCouplingNotFound_shouldThrowException() {
        // given
        OrderCreateDto dto = createOrderCreateDto(COMPANY_NAME, 999L, null);
        when(companyRepository.findByNameIgnoreCase(COMPANY_NAME)).thenReturn(Optional.of(createCompany(COMPANY_NAME)));
        when(couplingRepository.findById(999L)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Продукция не найдена");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: дополнительные работы не указаны → список пустой, стоимость только за муфту")
    void createOrder_whenNoAdditionalWorks_shouldCalculateCostWithoutAddWorks() {
        // given
        OrderCreateDto dto = createOrderCreateDto(COMPANY_NAME, COUPLING_ID, null);
        CompanyEntity existingCompany = createCompany(COMPANY_NAME);
        CouplingEntity coupling = createCoupling();

        when(companyRepository.findByNameIgnoreCase(COMPANY_NAME)).thenReturn(Optional.of(existingCompany));
        when(couplingRepository.findById(COUPLING_ID)).thenReturn(Optional.of(coupling));


        // Сохраняемая сущность должна иметь пустой список доп. работ
        OrderEntity savedOrder = OrderEntity.builder()
                .id(ORDER_ID)
                .createdAt(LocalDateTime.now())
                .company(existingCompany)
                .coupling(coupling)
                .quantity(QUANTITY)
                .deadline(DEADLINE)
                .status(OrderStatus.WAITING)
                .additionalWorks(List.of())   // ← явно пустой список, не null
                .totalCost(MANUFACTURING_COST.multiply(BigDecimal.valueOf(QUANTITY)))
                .build();
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        // when
        OrderDto result = orderService.createOrder(dto);

        // then
        assertThat(result.getTotalCost()).isEqualByComparingTo(MANUFACTURING_COST.multiply(BigDecimal.valueOf(QUANTITY)));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getAdditionalWorks()).isEmpty();
    }

    @Test
    @DisplayName("updateStatus: заказ найден → статус обновлён")
    void updateStatus_whenOrderExists_shouldUpdateAndReturnDto() {
        // given
        OrderEntity existingOrder = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        OrderDto result = orderService.updateStatus(ORDER_ID, OrderStatus.IN_PRODUCTION);

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("updateStatus: заказ не найден → исключение")
    void updateStatus_whenOrderNotFound_shouldThrowException() {
        // given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> orderService.updateStatus(ORDER_ID, OrderStatus.IN_PRODUCTION))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Заказ не найден");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrdersByStatus: с поиском по имени компании → вызывается метод с search")
    void getOrdersByStatus_withSearch_shouldCallCorrectRepositoryMethod() {
        // given
        OrderStatus status = OrderStatus.WAITING;
        String search = "Ромашка";
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderEntity> orderPage = new PageImpl<>(List.of(createOrderEntity(status)));
        when(orderRepository.findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(status), eq(search), eq(pageable))).thenReturn(orderPage);

        // when
        Page<OrderDto> result = orderService.getOrdersByStatus(status, search, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCompanyName()).isEqualTo(COMPANY_NAME);
        verify(orderRepository, never()).findByStatusOrderByCreatedAtDesc(any(), any());
        verify(orderRepository).findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(status, search, pageable);
    }

    @Test
    @DisplayName("getOrdersByStatus: без поиска → вызывается метод без search")
    void getOrdersByStatus_withoutSearch_shouldCallRepositoryWithoutSearch() {
        // given
        OrderStatus status = OrderStatus.WAITING;
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderEntity> orderPage = new PageImpl<>(List.of(createOrderEntity(status)));
        when(orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)).thenReturn(orderPage);

        // when
        Page<OrderDto> result = orderService.getOrdersByStatus(status, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, never()).findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(any(), any(), any());
        verify(orderRepository).findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Test
    @DisplayName("getOrderById: заказ найден → возвращает DTO")
    void getOrderById_whenExists_shouldReturnDto() {
        // given
        OrderEntity order = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // when
        OrderDto result = orderService.getOrderById(ORDER_ID);

        // then
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getCompanyName()).isEqualTo(COMPANY_NAME);
        assertThat(result.getAdditionalWorkIds()).containsExactly(ADD_WORK_ID_1, ADD_WORK_ID_2);
    }

    @Test
    @DisplayName("getOrderById: заказ не найден → исключение")
    void getOrderById_whenNotFound_shouldThrowException() {
        // given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Заказ не найден");
    }

    @Test
    @DisplayName("updateOrder: полное обновление всех полей с пересчётом стоимости")
    void updateOrder_shouldUpdateAllFieldsAndRecalculateCost() {
        // given
        OrderEntity existingOrder = createOrderEntity(OrderStatus.WAITING);
        OrderUpdateDto updateDto = new OrderUpdateDto();
        String newCompanyName = "Обновлённая компания";
        Long newCouplingId = 200L;
        int newQuantity = 10;
        LocalDate newDeadline = LocalDate.now().plusDays(20);
        List<Long> newAddWorkIds = List.of(ADD_WORK_ID_1);

        updateDto.setCompanyName(newCompanyName);
        updateDto.setCouplingId(newCouplingId);
        updateDto.setQuantity(newQuantity);
        updateDto.setDeadline(newDeadline);
        updateDto.setAdditionalWorkIds(newAddWorkIds);

        CompanyEntity newCompany = createCompany(newCompanyName, 2L);
        CouplingEntity newCoupling = CouplingEntity.builder()
                .id(newCouplingId)
                .type("Новая муфта")
                .manufacturingCost(BigDecimal.valueOf(2000))
                .build();
        AdditionalWorkEntity addWork = createAddWork(ADD_WORK_ID_1, "Покраска", BigDecimal.valueOf(300));

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(companyRepository.findByNameIgnoreCase(newCompanyName)).thenReturn(Optional.of(newCompany));
        when(couplingRepository.findById(newCouplingId)).thenReturn(Optional.of(newCoupling));
        when(additionalWorkRepository.findAllById(newAddWorkIds)).thenReturn(List.of(addWork));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        OrderDto result = orderService.updateOrder(ORDER_ID, updateDto);

        // then
        assertThat(result.getCompanyName()).isEqualTo(newCompanyName);
        assertThat(result.getCouplingId()).isEqualTo(newCouplingId);
        assertThat(result.getQuantity()).isEqualTo(newQuantity);
        assertThat(result.getDeadline()).isEqualTo(newDeadline);
        assertThat(result.getAdditionalWorkIds()).containsExactly(ADD_WORK_ID_1);

        BigDecimal expectedCost = BigDecimal.valueOf(2000).add(BigDecimal.valueOf(300)).multiply(BigDecimal.valueOf(10));
        assertThat(result.getTotalCost()).isEqualByComparingTo(expectedCost);

        // Проверяем, что сущность обновилась
        assertThat(existingOrder.getCompany()).isSameAs(newCompany);
        assertThat(existingOrder.getCoupling()).isSameAs(newCoupling);
        assertThat(existingOrder.getQuantity()).isEqualTo(newQuantity);
        assertThat(existingOrder.getDeadline()).isEqualTo(newDeadline);
        assertThat(existingOrder.getAdditionalWorks()).containsExactly(addWork);
    }

    @Test
    @DisplayName("updateOrder: компания не найдена → создаётся новая")
    void updateOrder_whenCompanyNotFound_shouldCreateNewCompany() {
        // given
        OrderEntity existingOrder = createOrderEntity(OrderStatus.WAITING);
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setCompanyName(NEW_COMPANY_NAME);
        updateDto.setCouplingId(COUPLING_ID);
        updateDto.setQuantity(QUANTITY);
        updateDto.setDeadline(DEADLINE);
        updateDto.setAdditionalWorkIds(null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(companyRepository.findByNameIgnoreCase(NEW_COMPANY_NAME)).thenReturn(Optional.empty());
        CompanyEntity newCompany = createCompany(NEW_COMPANY_NAME, 3L);
        when(companyRepository.save(any(CompanyEntity.class))).thenReturn(newCompany);
        when(couplingRepository.findById(COUPLING_ID)).thenReturn(Optional.of(createCoupling()));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        OrderDto result = orderService.updateOrder(ORDER_ID, updateDto);

        // then
        assertThat(result.getCompanyName()).isEqualTo(NEW_COMPANY_NAME);
        verify(companyRepository).save(any(CompanyEntity.class));
    }

    @Test
    @DisplayName("deleteOrder: заказ найден → удалён")
    void deleteOrder_whenExists_shouldDelete() {
        // given
        OrderEntity order = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // when
        orderService.deleteOrder(ORDER_ID);

        // then
        verify(orderRepository).delete(order);
    }

    @Test
    @DisplayName("deleteOrder: заказ не найден → исключение")
    void deleteOrder_whenNotFound_shouldThrowException() {
        // given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Заказ не найден");
        verify(orderRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getUrgentProductionOrders: возвращает limit заказов с правильным расчётом срочности")
    void getUrgentProductionOrders_shouldReturnLimitedOrdersWithUrgencyClass() {
        // given
        LocalDate today = LocalDate.now();
        // Создаём заказы с дедлайнами в нужном порядке (уже отсортированными по возрастанию)
        OrderEntity order4 = createOrderEntity(OrderStatus.IN_PRODUCTION);
        order4.setDeadline(today.plusDays(1));
        OrderEntity order1 = createOrderEntity(OrderStatus.IN_PRODUCTION);
        order1.setDeadline(today.plusDays(2));
        OrderEntity order2 = createOrderEntity(OrderStatus.IN_PRODUCTION);
        order2.setDeadline(today.plusDays(5));
        OrderEntity order3 = createOrderEntity(OrderStatus.IN_PRODUCTION);
        order3.setDeadline(today.plusDays(10));

        List<OrderEntity> allOrders = List.of(order4, order1, order2, order3); // уже отсортировано
        when(orderRepository.findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus.IN_PRODUCTION))
                .thenReturn(allOrders);

        int limit = 3;

        // when
        List<HomeOrderDto> result = orderService.getUrgentProductionOrders(limit);

        // then
        assertThat(result).hasSize(limit);
        // Проверяем, что взяты первые 3 (по возрастанию дедлайна)
        assertThat(result.get(0).getDaysUntilDeadline()).isEqualTo(1);
        assertThat(result.get(1).getDaysUntilDeadline()).isEqualTo(2);
        assertThat(result.get(2).getDaysUntilDeadline()).isEqualTo(5);

        assertThat(result.get(0).getUrgencyClass()).isEqualTo("bg-danger text-white");
        assertThat(result.get(1).getUrgencyClass()).isEqualTo("bg-danger text-white");
        assertThat(result.get(2).getUrgencyClass()).isEqualTo("bg-warning");
    }

    @Test
    @DisplayName("getUrgentProductionOrders: если заказов меньше limit → возвращаются все")
    void getUrgentProductionOrders_whenLessThanLimit_shouldReturnAll() {
        // given
        OrderEntity order = createOrderEntity(OrderStatus.IN_PRODUCTION);
        order.setDeadline(LocalDate.now().plusDays(3));
        when(orderRepository.findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus.IN_PRODUCTION))
                .thenReturn(List.of(order));

        // when
        List<HomeOrderDto> result = orderService.getUrgentProductionOrders(10);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDaysUntilDeadline()).isEqualTo(3);
        assertThat(result.get(0).getUrgencyClass()).isEqualTo("bg-danger text-white");
    }

    @Test
    @DisplayName("getUrgentProductionOrders: пустой список → возвращается пустой список")
    void getUrgentProductionOrders_whenNoOrders_shouldReturnEmptyList() {
        // given
        when(orderRepository.findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus.IN_PRODUCTION))
                .thenReturn(List.of());

        // when
        List<HomeOrderDto> result = orderService.getUrgentProductionOrders(5);

        // then
        assertThat(result).isEmpty();
    }

    // Дополнительно: проверка вспомогательных методов getOrderEntityById, getOrdersByIdsWithDetails, saveOrder
    // Можно написать короткие тесты для них, но они тривиальны. Приведём пару примеров:

    @Test
    @DisplayName("getOrderEntityById: делегирует репозиторию и пробрасывает исключение если не найден")
    void getOrderEntityById_shouldReturnEntityOrThrow() {
        // given
        OrderEntity order = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // when
        OrderEntity result = orderService.getOrderEntityById(ORDER_ID);

        // then
        assertThat(result).isSameAs(order);

        // when not found
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderEntityById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Заказ не найден, id=999");
    }

    @Test
    @DisplayName("getOrdersByIdsWithDetails: вызывает findByIdIn репозитория")
    void getOrdersByIdsWithDetails_shouldCallRepositoryFindByIdIn() {
        // given
        List<Long> ids = List.of(1L, 2L);
        List<OrderEntity> expected = List.of(createOrderEntity(OrderStatus.WAITING));
        when(orderRepository.findByIdIn(ids)).thenReturn(expected);

        // when
        List<OrderEntity> result = orderService.getOrdersByIdsWithDetails(ids);

        // then
        assertThat(result).isSameAs(expected);
        verify(orderRepository).findByIdIn(ids);
    }

    @Test
    @DisplayName("saveOrder: делегирует репозиторию")
    void saveOrder_shouldDelegateToRepository() {
        // given
        OrderEntity order = createOrderEntity(OrderStatus.WAITING);
        when(orderRepository.save(order)).thenReturn(order);

        // when
        OrderEntity result = orderService.saveOrder(order);

        // then
        assertThat(result).isSameAs(order);
        verify(orderRepository).save(order);
    }
}