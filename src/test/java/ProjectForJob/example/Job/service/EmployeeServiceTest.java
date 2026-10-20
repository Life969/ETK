package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.domain.entity.handbook.EmployeeEntity;
import ProjectForJob.example.Job.repository.handbook.EmployeeRepository;
import ProjectForJob.example.Job.service.handbook.EmployeeService;
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
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("findAll: должен вернуть всех сотрудников из репозитория")
    void findAll_shouldReturnAllEmployeesFromRepository() {
        // given
        List<EmployeeEntity> expected = List.of(new EmployeeEntity(), new EmployeeEntity());
        when(employeeRepository.findAll()).thenReturn(expected);

        // when
        List<EmployeeEntity> result = employeeService.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(employeeRepository, only()).findAll();
    }

    @Test
    @DisplayName("save: должен делегировать сохранение репозиторию и вернуть сохранённую сущность")
    void save_shouldDelegateAndReturnSavedEntity() {
        // given
        EmployeeEntity input = new EmployeeEntity();
        EmployeeEntity saved = new EmployeeEntity();
        when(employeeRepository.save(input)).thenReturn(saved);

        // when
        EmployeeEntity result = employeeService.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(employeeRepository, only()).save(input);
    }

    @Test
    @DisplayName("findById: при существующем ID должен вернуть сотрудника из репозитория")
    void findById_whenIdExists_shouldReturnEmployee() {
        // given
        Long id = 1L;
        EmployeeEntity expected = new EmployeeEntity();
        when(employeeRepository.findById(id)).thenReturn(Optional.of(expected));

        // when
        EmployeeEntity result = employeeService.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(employeeRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID должен бросить NoSuchElementException с правильным сообщением")
    void findById_whenIdDoesNotExist_shouldThrowNoSuchElementException() {
    // given
    Long id = 999L;
    when(employeeRepository.findById(id)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> employeeService.findById(id))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Сотрудник с id " + id + " не найдена");

    verify(employeeRepository, only()).findById(id);
}

    @Test
    @DisplayName("deleteById: должен вызвать deleteById у репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        employeeService.deleteById(id);

        // then
        verify(employeeRepository, only()).deleteById(id);
    }

    @Test
    @DisplayName("deleteAll: должен вызвать deleteAll у репозитория")
    void deleteAll_shouldCallRepositoryDeleteAll() {
        // when
        employeeService.deleteAll();

        // then
        verify(employeeRepository, only()).deleteAll();
    }
}
