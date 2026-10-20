package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.repository.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.repository.handbook.ExternalThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ExternalThreadService {

    private final ExternalThreadRepository repository;

    public List<ExternalThreadEntity> findAll() {
        return repository.findAllByOrderByTypeAscConditionalDiameterAsc();
    }

    public ExternalThreadEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Наружная резьба с id " + id + " не найдена"));
    }

    @Transactional
    public ExternalThreadEntity save(ExternalThreadEntity entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<String> findAllDistinctTypes() {
        return repository.findAllDistinctTypes();
    }

    public List<ExternalThreadEntity> findByType(String type) {
        return repository.findByTypeOrderByConditionalDiameterAsc(type);
    }
}