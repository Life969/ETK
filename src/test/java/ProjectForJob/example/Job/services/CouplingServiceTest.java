package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.repositories.Handbook.CouplingRepository;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сервис для работы с данными о изделиях")
class CouplingServiceTest {

    @Mock
    private CouplingRepository couplingRepository;

    @InjectMocks
    private CouplingService couplingService;

    @Test
    @DisplayName("findAll должен вернуть список муфт")
    void findAll_shouldReturnAllCouplingsFromRepository() {
        // given
        List<CouplingEntity> expectedCouplings = List.of(new CouplingEntity(), new CouplingEntity());
        when(couplingRepository.findAll()).thenReturn(expectedCouplings);

        // when
        List<CouplingEntity> result = couplingService.findAll();

        // then
        assertThat(result).isSameAs(expectedCouplings); // проверяем, что вернулся именно тот же список, который дал репозиторий
        verify(couplingRepository, only()).findAll();    // убеждаемся, что вызван только findAll и больше ничего
    }

    @Test
    @DisplayName("findById должен вернуть изделие по ID")
    void findById_whenIdExists_shouldReturnCoupling() {
        // given
        Long id = 1L;
        CouplingEntity expectedCoupling = new CouplingEntity();
        when(couplingRepository.findById(id)).thenReturn(Optional.of(expectedCoupling));

        // when
        CouplingEntity result = couplingService.findById(id);

        // then
        assertThat(result).isSameAs(expectedCoupling);
        verify(couplingRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById проверка на NPE")
    void findById_whenIdDoesNotExist_shouldThrowNoSuchElementException() {
        // given
        Long id = 999L;
        when(couplingRepository.findById(id)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> couplingService.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Муфта с id " + id + " не найдена");
        verify(couplingRepository, only()).findById(id);
    }

    @Test
    @DisplayName("save должен сохранить сущность")
    void save_shouldDelegateToRepositoryAndReturnSavedEntity() {
        // given
        CouplingEntity inputCoupling = new CouplingEntity();
        CouplingEntity savedCoupling = new CouplingEntity();
        when(couplingRepository.save(inputCoupling)).thenReturn(savedCoupling);

        // when
        CouplingEntity result = couplingService.save(inputCoupling);

        // then
        assertThat(result).isSameAs(savedCoupling);
        verify(couplingRepository, only()).save(inputCoupling);
    }

    @Test
    @DisplayName("удаление по ID")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        couplingService.deleteById(id);

        // then
        verify(couplingRepository, only()).deleteById(id);
    }

    @Test
    @DisplayName("Поиск типов изделия без дубликатов")
    void findAllDistinctTypes_shouldReturnListFromRepository() {
        // given
        List<String> expectedTypes = List.of("TYPE_A", "TYPE_B");
        when(couplingRepository.findAllDistinctTypes()).thenReturn(expectedTypes);

        // when
        List<String> result = couplingService.findAllDistinctTypes();

        // then
        assertThat(result).isSameAs(expectedTypes);
        verify(couplingRepository, only()).findAllDistinctTypes();
    }

    @Test
    @DisplayName("поиск по типу")
    void findByType_shouldReturnListFromRepository() {
        // given
        String type = "SOME_TYPE";
        List<CouplingEntity> expectedCouplings = List.of(new CouplingEntity(),
                new CouplingEntity());

        when(couplingRepository.findByType(type)).thenReturn(expectedCouplings);

        // when
        List<CouplingEntity> result = couplingService.findByType(type);

        // then
        assertThat(result).isSameAs(expectedCouplings);
        verify(couplingRepository, only()).findByType(type);
    }
}