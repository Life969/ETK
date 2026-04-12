package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.repositories.Handbook.PipeAdapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PipeAdapterService {

    private final PipeAdapterRepository repository;

    public List<PipeAdapterEntity> findAll() {
        return repository.findAll();
    }

    public PipeAdapterEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Переводник с id " + id + " не найден"));
    }

    public PipeAdapterEntity save(PipeAdapterEntity adapter) {
        return repository.save(adapter);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<String> findAllDistinctFirstSideTypes() {
        return repository.findAllDistinctFirstSideTypes();
    }

    public List<PipeAdapterEntity> findByFirstSideType(String type) {
        return repository.findByFirstSideType(type);
    }
}