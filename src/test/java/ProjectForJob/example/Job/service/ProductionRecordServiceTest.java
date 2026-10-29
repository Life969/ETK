package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.domain.entity.handbook.*;
import ProjectForJob.example.Job.domain.entity.journal.ProductionRecordEntity;
import ProjectForJob.example.Job.domain.entity.order.PipeProductKind;
import ProjectForJob.example.Job.exception.BusinessException;
import ProjectForJob.example.Job.exception.service.journal.ProductionRecordErrorCode;
import ProjectForJob.example.Job.repository.journal.ProductionRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionRecordServiceTest {

    @Mock
    private ProductionRecordRepository repository;

    @InjectMocks
    private ProductionRecordService service;

    // ================================================================
    // findById
    // ================================================================

    @Test
    @DisplayName("findById: запись найдена — возвращает сущность из репозитория")
    void findById_found_returnsEntity() {
        ProductionRecordEntity entity = baseRecord().coupling(coupling()).build();
        when(repository.findById(42L)).thenReturn(Optional.of(entity));

        ProductionRecordEntity actual = service.findById(42L);

        assertThat(actual).isSameAs(entity);
        verify(repository).findById(42L);
    }

    @Test
    @DisplayName("findById: запись не найдена — BusinessException с кодом RECORD_NOT_FOUND")
    void findById_notFound_throwsBusinessExceptionWithPrc001() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("id=999")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.RECORD_NOT_FOUND));
    }

    // ================================================================
    // deleteById
    // ================================================================

    @Test
    @DisplayName("deleteById: запись существует — проверяет наличие и делегирует удаление")
    void deleteById_exists_delegatesToRepositoryDelete() {
        when(repository.existsById(42L)).thenReturn(true);

        service.deleteById(42L);

        verify(repository).existsById(42L);
        verify(repository).deleteById(42L);
    }

    @Test
    @DisplayName("deleteById: запись не найдена — BusinessException, deleteById не вызывается (фикс бага 500)")
    void deleteById_notExists_throwsBeforeTouchingDelete() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("id=999")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.RECORD_NOT_FOUND));

        // ЯКОРЬ: именно этот ассерт страхует фикс бага 500.
        // deleteById НЕ должен вызываться при отсутствии записи.
        verify(repository).existsById(999L);
        verify(repository, never()).deleteById(anyLong());
    }

    // ================================================================
    // save — happy-path по всем 4 типам продуктов
    // ================================================================

    @Test
    @DisplayName("save (COUPLING): валидация проходит, делегирует в репозиторий")
    void save_couplingValid_delegatesToRepository() {
        ProductionRecordEntity toSave = baseRecord().coupling(coupling()).build();
        ProductionRecordEntity saved = baseRecord().coupling(coupling()).build();
        when(repository.save(toSave)).thenReturn(saved);

        ProductionRecordEntity actual = service.save(toSave);

        assertThat(actual).isSameAs(saved);
        verify(repository).save(toSave);
    }

    @Test
    @DisplayName("save (ADAPTER): валидация проходит, делегирует в репозиторий")
    void save_adapterValid_delegatesToRepository() {
        ProductionRecordEntity record = baseRecord().adapter(adapter()).build();
        when(repository.save(record)).thenReturn(record);

        ProductionRecordEntity actual = service.save(record);

        assertThat(actual).isSameAs(record);
        verify(repository).save(record);
    }

    @Test
    @DisplayName("save (PIPE): валидация проходит без pieceLengthMm, делегирует в репозиторий")
    void save_pipeValid_delegatesToRepository() {
        // ЯКОРЬ: PIPE без pieceLengthMm — легитимный кейс. Если кто-то
        // "упростит" валидацию до "thread требует pieceLengthMm" — упадёт.
        ProductionRecordEntity record = baseRecord()
                .externalThread(thread())
                .pipeProductKind(PipeProductKind.PIPE)
                .build();
        when(repository.save(record)).thenReturn(record);

        ProductionRecordEntity actual = service.save(record);

        assertThat(actual).isSameAs(record);
        verify(repository).save(record);
    }

    @Test
    @DisplayName("save (NIPPLE): валидация проходит с положительным pieceLengthMm")
    void save_nippleValid_delegatesToRepository() {
        ProductionRecordEntity record = baseRecord()
                .externalThread(thread())
                .pipeProductKind(PipeProductKind.NIPPLE)
                .pieceLengthMm(2000.0)
                .build();
        when(repository.save(record)).thenReturn(record);

        ProductionRecordEntity actual = service.save(record);

        assertThat(actual).isSameAs(record);
        verify(repository).save(record);
    }

    // ================================================================
    // save — ошибки валидации продукта
    // ================================================================

    @Test
    @DisplayName("save: ни одного продукта — PRODUCT_TYPE_MISSING, репозиторий не вызывается")
    void save_withoutAnyProduct_throwsProductTypeMissing() {
        ProductionRecordEntity record = baseRecord().build();

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PRODUCT_TYPE_MISSING));

        // ЯКОРЬ: при ошибке валидации save() НЕ должен доходить до репозитория.
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save: coupling + adapter — PRODUCT_TYPE_INVALID, репозиторий не вызывается")
    void save_withCouplingAndAdapter_throwsProductTypeInvalid() {
        ProductionRecordEntity record = baseRecord()
                .coupling(coupling())
                .adapter(adapter())
                .build();

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PRODUCT_TYPE_INVALID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save: coupling + thread — PRODUCT_TYPE_INVALID, репозиторий не вызывается")
    void save_withCouplingAndThread_throwsProductTypeInvalid() {
        ProductionRecordEntity record = baseRecord()
                .coupling(coupling())
                .externalThread(thread())
                .pipeProductKind(PipeProductKind.PIPE)
                .build();

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PRODUCT_TYPE_INVALID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save: thread без pipeProductKind — PRODUCT_TYPE_MISSING")
    void save_threadWithoutPipeProductKind_throwsProductTypeMissing() {
        ProductionRecordEntity record = baseRecord()
                .externalThread(thread())
                .build(); // pipeProductKind == null

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PIPE/NIPPLE")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PRODUCT_TYPE_MISSING));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save: NIPPLE без pieceLengthMm — PIECE_LENGTH_REQUIRED")
    void save_nippleWithoutPieceLength_throwsPieceLengthRequired() {
        ProductionRecordEntity record = baseRecord()
                .externalThread(thread())
                .pipeProductKind(PipeProductKind.NIPPLE)
                .build(); // pieceLengthMm == null

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("null")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PIECE_LENGTH_REQUIRED));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save: NIPPLE с pieceLengthMm = 0 — PIECE_LENGTH_REQUIRED")
    void save_nippleWithZeroPieceLength_throwsPieceLengthRequired() {
        ProductionRecordEntity record = baseRecord()
                .externalThread(thread())
                .pipeProductKind(PipeProductKind.NIPPLE)
                .pieceLengthMm(0.0)
                .build();

        assertThatThrownBy(() -> service.save(record))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0.0")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isSameAs(ProductionRecordErrorCode.PIECE_LENGTH_REQUIRED));

        verify(repository, never()).save(any());
    }

    // ================================================================
    // простые делегирующие методы — страховка от регресса
    // ================================================================

    @Test
    @DisplayName("findAll: делегирует в findAllWithDetails репозитория")
    void findAll_delegatesToRepository() {
        List<ProductionRecordEntity> expected = List.of(
                baseRecord().id(1L).coupling(coupling()).build(),
                baseRecord().id(2L).coupling(coupling()).build());
        when(repository.findAllWithDetails()).thenReturn(expected);

        assertThat(service.findAll()).isSameAs(expected);
        verify(repository).findAllWithDetails();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("findLast5Records: делегирует в findTop5ByOrderByCreatedAtDesc репозитория")
    void findLast5Records_delegatesToRepository() {
        List<ProductionRecordEntity> expected = List.of(
                baseRecord().id(1L).coupling(coupling()).build());
        when(repository.findTop5ByOrderByCreatedAtDesc()).thenReturn(expected);

        assertThat(service.findLast5Records()).isSameAs(expected);
        verify(repository).findTop5ByOrderByCreatedAtDesc();
        verifyNoMoreInteractions(repository);
    }

    // ================================================================
    // Фикстуры
    // ================================================================

    private static ProductionRecordEntity.ProductionRecordEntityBuilder baseRecord() {
        return ProductionRecordEntity.builder()
                .id(42L)
                .productionDate(LocalDate.of(2026, 1, 15))
                .quantity(10)
                .employee(EmployeeEntity.builder().id(1L).name("Иванов И.И.").build())
                .machine(MachineEntity.builder().id(1L).name("Станок-1").build())
                .comment("Тестовая запись");
    }

    private static CouplingEntity coupling() {
        return CouplingEntity.builder()
                .id(1L).type("Муфта").conditionalDiameter("73")
                .manufacturingCost(BigDecimal.ONE).priceForEmployee(BigDecimal.ONE)
                .build();
    }

    private static PipeAdapterEntity adapter() {
        return PipeAdapterEntity.builder()
                .id(2L)
                .firstSideType("НКТ").firstSideDiameter("73")
                .secondSideType("НКТ").secondSideDiameter("89")
                .manufacturingCost(BigDecimal.ONE).priceForEmployee(BigDecimal.ONE)
                .build();
    }

    private static ExternalThreadEntity thread() {
        return ExternalThreadEntity.builder()
                .id(3L).type("НКТ").conditionalDiameter("73")
                .manufacturingCost(BigDecimal.ONE).priceForEmployee(BigDecimal.ONE)
                .build();
    }
}