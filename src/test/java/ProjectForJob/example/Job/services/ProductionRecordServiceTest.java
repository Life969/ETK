package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.ForOrders.ProductionRecordEntity;
import ProjectForJob.example.Job.repositories.ProductionRecordRepository;
import org.junit.jupiter.api.Test;



import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сервис записей производства (ProductionRecordService)")
class ProductionRecordServiceTest {

    @Mock
    private ProductionRecordRepository repository;

    @InjectMocks
    private ProductionRecordService service;

    @Test
    @DisplayName("findAll: должен вернуть все записи через findAllWithDetails()")
    void findAll_shouldReturnAllRecordsFromRepository() {
        // given
        List<ProductionRecordEntity> expected = List.of(new ProductionRecordEntity(), new ProductionRecordEntity());
        when(repository.findAllWithDetails()).thenReturn(expected);

        // when
        List<ProductionRecordEntity> result = service.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(repository, only()).findAllWithDetails();
    }

    @Test
    @DisplayName("findById: при существующем ID возвращает запись")
    void findById_whenIdExists_shouldReturnRecord() {
        // given
        Long id = 1L;
        ProductionRecordEntity expected = new ProductionRecordEntity();
        when(repository.findById(id)).thenReturn(Optional.of(expected));

        // when
        ProductionRecordEntity result = service.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(repository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID бросает NoSuchElementException")
    void findById_whenIdDoesNotExist_shouldThrowException() {
        // given
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Запись с id " + id + " не найдена");
        verify(repository, only()).findById(id);
    }

    @Test
    @DisplayName("save: делегирует сохранение репозиторию")
    void save_shouldDelegateToRepository() {
        // given
        ProductionRecordEntity input = new ProductionRecordEntity();
        ProductionRecordEntity saved = new ProductionRecordEntity();
        when(repository.save(input)).thenReturn(saved);

        // when
        ProductionRecordEntity result = service.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(repository, only()).save(input);
    }

    @Test
    @DisplayName("deleteById: вызывает deleteById репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 10L;

        // when
        service.deleteById(id);

        // then
        verify(repository, only()).deleteById(id);
    }

    @Test
    @DisplayName("findAllByFilters с Pageable: вызывает repository.findAll с Specification и Pageable")
    void findAllByFilters_withPageable_shouldCallRepositoryFindAllWithSpecAndPageable() {
        String productType = "COUPLING";
        Long couplingId = 1L;
        Long employeeId = 2L;
        Long adapterId = null;
        Long machineId = 3L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductionRecordEntity> expectedPage = new PageImpl<>(List.of(new ProductionRecordEntity()));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        Page<ProductionRecordEntity> result = service.findAllByFilters(
                productType, couplingId, adapterId, employeeId, machineId, startDate, endDate, pageable);

        // then
        assertThat(result).isSameAs(expectedPage);
        verify(repository, only()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("findAllByFilters с Sort: вызывает repository.findAll с Specification и Sort")
    void findAllByFilters_withSort_shouldCallRepositoryFindAllWithSpecAndSort() {
        // given
        Long couplingId = null;
        Long employeeId = null;
        Long machineId = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        Sort sort = Sort.by("productionDate").descending();
        List<ProductionRecordEntity> expectedList = List.of(new ProductionRecordEntity());
        when(repository.findAll(any(Specification.class), eq(sort))).thenReturn(expectedList);

        // when
        List<ProductionRecordEntity> result = service.findAllByFilters(couplingId, employeeId, machineId, startDate, endDate, sort);

        // then
        assertThat(result).isSameAs(expectedList);
        verify(repository, only()).findAll(any(Specification.class), eq(sort));
    }

    @Test
    @DisplayName("findLast5Records: возвращает последние 5 записей, отсортированных по createdAt DESC")
    void findLast5Records_shouldReturnLast5Records() {
        // given
        List<ProductionRecordEntity> expectedContent = List.of(new ProductionRecordEntity(), new ProductionRecordEntity());
        Page<ProductionRecordEntity> page = mock(Page.class);
        when(page.getContent()).thenReturn(expectedContent);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        // when
        List<ProductionRecordEntity> result = service.findLast5Records();

        assertThat(result).isSameAs(expectedContent);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, only()).findAll(captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(0);
        assertThat(captured.getPageSize()).isEqualTo(5);
        assertThat(captured.getSort()).isEqualTo(Sort.by("createdAt").descending());
    }
}