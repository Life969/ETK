package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.domain.entity.handbook.EmployeeEntity;
import ProjectForJob.example.Job.repository.handbook.ListEmployeeRepository;
import ProjectForJob.example.Job.service.handbook.EmployeesService;
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
@DisplayName("Сервис для работы с сотрудниками (EmployeesService)")
class EmployeesServiceTest {

    @Mock
    private ListEmployeeRepository listEmployeeRepository;

    @InjectMocks
    private EmployeesService employeesService;

    @Test
    @DisplayName("findAll: должен вернуть всех сотрудников из репозитория")
    void findAll_shouldReturnAllEmployeesFromRepository() {
        // given
        List<EmployeeEntity> expected = List.of(new EmployeeEntity(), new EmployeeEntity());
        when(listEmployeeRepository.findAll()).thenReturn(expected);

        // when
        List<EmployeeEntity> result = employeesService.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(listEmployeeRepository, only()).findAll();
    }

    @Test
    @DisplayName("save: должен делегировать сохранение репозиторию и вернуть сохранённую сущность")
    void save_shouldDelegateAndReturnSavedEntity() {
        // given
        EmployeeEntity input = new EmployeeEntity();
        EmployeeEntity saved = new EmployeeEntity();
        when(listEmployeeRepository.save(input)).thenReturn(saved);

        // when
        EmployeeEntity result = employeesService.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(listEmployeeRepository, only()).save(input);
    }

    @Test
    @DisplayName("findById: при существующем ID должен вернуть сотрудника из репозитория")
    void findById_whenIdExists_shouldReturnEmployee() {
        // given
        Long id = 1L;
        EmployeeEntity expected = new EmployeeEntity();
        when(listEmployeeRepository.findById(id)).thenReturn(Optional.of(expected));

        // when
        EmployeeEntity result = employeesService.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(listEmployeeRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID должен бросить NoSuchElementException с правильным сообщением")
    void findById_whenIdDoesNotExist_shouldThrowNoSuchElementException() {
    // given
    Long id = 999L;
    when(listEmployeeRepository.findById(id)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> employeesService.findById(id))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Сотрудник с id " + id + " не найдена");

    verify(listEmployeeRepository, only()).findById(id);
}

    @Test
    @DisplayName("deleteById: должен вызвать deleteById у репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        employeesService.deleteById(id);

        // then
        verify(listEmployeeRepository, only()).deleteById(id);
    }

    @Test
    @DisplayName("deleteAll: должен вызвать deleteAll у репозитория")
    void deleteAll_shouldCallRepositoryDeleteAll() {
        // when
        employeesService.deleteAll();

        // then
        verify(listEmployeeRepository, only()).deleteAll();
    }
}
