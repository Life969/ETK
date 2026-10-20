package ProjectForJob.example.Job.service;

import ProjectForJob.example.Job.domain.entity.handbook.MachineEntity;
import ProjectForJob.example.Job.repository.handbook.MachineRepository;
import ProjectForJob.example.Job.service.handbook.MachinesService;
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
@DisplayName("Сервис для учета станков (MachinesService)")
class MachinesServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @InjectMocks
    private MachinesService machinesService;

    @Test
    @DisplayName("findAll: должен вернуть всё оборудование из репозитория")
    void findAll_shouldReturnAllMachinesFromRepository() {
        // given
        List<MachineEntity> expected = List.of(new MachineEntity(), new MachineEntity());
        when(machineRepository.findAll()).thenReturn(expected);

        // when
        List<MachineEntity> result = machinesService.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(machineRepository, only()).findAll();
    }

    @Test
    @DisplayName("save: должен делегировать сохранение репозиторию и вернуть сохранённую сущность")
    void save_shouldDelegateAndReturnSavedEntity() {
        // given
        MachineEntity input = new MachineEntity();
        MachineEntity saved = new MachineEntity();
        when(machineRepository.save(input)).thenReturn(saved);

        // when
        MachineEntity result = machinesService.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(machineRepository, only()).save(input);
    }

    @Test
    @DisplayName("findById: при существующем ID должен вернуть оборудование")
    void findById_whenIdExists_shouldReturnMachine() {
        // given
        Long id = 1L;
        MachineEntity expected = new MachineEntity();
        when(machineRepository.findById(id)).thenReturn(Optional.of(expected));

        // when
        MachineEntity result = machinesService.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(machineRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID должен вернуть ошибку")
    void findById_whenIdDoesNotExist_shouldReturnNull() {
        // given
        Long id = 999L;
        when(machineRepository.findById(id)).thenReturn(Optional.empty());

        // when


        // then
        assertThatThrownBy(() -> machinesService.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Станок id " + id + " не найден");;
        verify(machineRepository, only()).findById(id);
    }

    @Test
    @DisplayName("deleteById: должен вызвать deleteById у репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        machinesService.deleteById(id);

        // then
        verify(machineRepository, only()).deleteById(id);
    }

    @Test
    @DisplayName("deleteAll: должен вызвать deleteAll у репозитория")
    void deleteAll_shouldCallRepositoryDeleteAll() {
        // when
        machinesService.deleteAll();

        // then
        verify(machineRepository, only()).deleteAll();
    }
}