package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.Handbook.MachinesEntity;
import ProjectForJob.example.Job.repositories.Handbook.ListMachineRepository;
import ProjectForJob.example.Job.services.Handbook.MachinesService;
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
    private ListMachineRepository listMachineRepository;

    @InjectMocks
    private MachinesService machinesService;

    @Test
    @DisplayName("findAll: должен вернуть всё оборудование из репозитория")
    void findAll_shouldReturnAllMachinesFromRepository() {
        // given
        List<MachinesEntity> expected = List.of(new MachinesEntity(), new MachinesEntity());
        when(listMachineRepository.findAll()).thenReturn(expected);

        // when
        List<MachinesEntity> result = machinesService.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(listMachineRepository, only()).findAll();
    }

    @Test
    @DisplayName("save: должен делегировать сохранение репозиторию и вернуть сохранённую сущность")
    void save_shouldDelegateAndReturnSavedEntity() {
        // given
        MachinesEntity input = new MachinesEntity();
        MachinesEntity saved = new MachinesEntity();
        when(listMachineRepository.save(input)).thenReturn(saved);

        // when
        MachinesEntity result = machinesService.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(listMachineRepository, only()).save(input);
    }

    @Test
    @DisplayName("findById: при существующем ID должен вернуть оборудование")
    void findById_whenIdExists_shouldReturnMachine() {
        // given
        Long id = 1L;
        MachinesEntity expected = new MachinesEntity();
        when(listMachineRepository.findById(id)).thenReturn(Optional.of(expected));

        // when
        MachinesEntity result = machinesService.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(listMachineRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID должен вернуть ошибку")
    void findById_whenIdDoesNotExist_shouldReturnNull() {
        // given
        Long id = 999L;
        when(listMachineRepository.findById(id)).thenReturn(Optional.empty());

        // when


        // then
        assertThatThrownBy(() -> machinesService.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Станок id " + id + " не найден");;
        verify(listMachineRepository, only()).findById(id);
    }

    @Test
    @DisplayName("deleteById: должен вызвать deleteById у репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        machinesService.deleteById(id);

        // then
        verify(listMachineRepository, only()).deleteById(id);
    }

    @Test
    @DisplayName("deleteAll: должен вызвать deleteAll у репозитория")
    void deleteAll_shouldCallRepositoryDeleteAll() {
        // when
        machinesService.deleteAll();

        // then
        verify(listMachineRepository, only()).deleteAll();
    }
}