package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.AdditionalWorkEntity;
import ProjectForJob.example.Job.repositories.AdditionalWorkRepository;
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
@DisplayName("Сервис для дополнительных работ (AdditionalWorkService)")
class AdditionalWorkServiceTest {

    @Mock
    private AdditionalWorkRepository additionalWorkRepository;

    @InjectMocks
    private AdditionalWorkService additionalWorkService;

    @Test
    @DisplayName("findAll: должен вернуть все дополнительные работы из репозитория")
    void findAll_shouldReturnAllAdditionalWorksFromRepository() {
        // given
        List<AdditionalWorkEntity> expected = List.of(new AdditionalWorkEntity(), new AdditionalWorkEntity());
        when(additionalWorkRepository.findAll()).thenReturn(expected);

        // when
        List<AdditionalWorkEntity> result = additionalWorkService.findAll();

        // then
        assertThat(result).isSameAs(expected);
        verify(additionalWorkRepository, only()).findAll();
    }

    @Test
    @DisplayName("findById: при существующем ID должен вернуть дополнительную работу")
    void findById_whenIdExists_shouldReturnAdditionalWork() {
        // given
        Long id = 1L;
        AdditionalWorkEntity expected = new AdditionalWorkEntity();
        when(additionalWorkRepository.findById(id)).thenReturn(Optional.of(expected));

        // when
        AdditionalWorkEntity result = additionalWorkService.findById(id);

        // then
        assertThat(result).isSameAs(expected);
        verify(additionalWorkRepository, only()).findById(id);
    }

    @Test
    @DisplayName("findById: при отсутствующем ID должен бросить NoSuchElementException с правильным сообщением")
    void findById_whenIdDoesNotExist_shouldThrowNoSuchElementException() {
        // given
        Long id = 999L;
        when(additionalWorkRepository.findById(id)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> additionalWorkService.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Дополнительная работа с  " + id + " не найдена");

        verify(additionalWorkRepository, only()).findById(id);
    }

    @Test
    @DisplayName("save: должен делегировать сохранение репозиторию и вернуть сохранённую сущность")
    void save_shouldDelegateAndReturnSavedEntity() {
        // given
        AdditionalWorkEntity input = new AdditionalWorkEntity();
        AdditionalWorkEntity saved = new AdditionalWorkEntity();
        when(additionalWorkRepository.save(input)).thenReturn(saved);

        // when
        AdditionalWorkEntity result = additionalWorkService.save(input);

        // then
        assertThat(result).isSameAs(saved);
        verify(additionalWorkRepository, only()).save(input);
    }

    @Test
    @DisplayName("deleteById: должен вызвать deleteById у репозитория")
    void deleteById_shouldCallRepositoryDeleteById() {
        // given
        Long id = 5L;

        // when
        additionalWorkService.deleteById(id);

        // then
        verify(additionalWorkRepository, only()).deleteById(id);
    }
}