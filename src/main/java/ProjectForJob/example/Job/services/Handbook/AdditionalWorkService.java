package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.AdditionalWorkEntity;
import ProjectForJob.example.Job.repositories.Handbook.AdditionalWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdditionalWorkService {

    private final AdditionalWorkRepository additionalWorkRepository;


    public List<AdditionalWorkEntity> findAll() {
        return additionalWorkRepository.findAll();
    }

    public AdditionalWorkEntity findById(Long id) {
        return additionalWorkRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Дополнительная работа с  " + id + " не найдена"));
    }

    public AdditionalWorkEntity save(AdditionalWorkEntity additionalWork) {
        return additionalWorkRepository.save(additionalWork);
    }

    public void deleteById(Long id) {
        additionalWorkRepository.deleteById(id);
    }
}

