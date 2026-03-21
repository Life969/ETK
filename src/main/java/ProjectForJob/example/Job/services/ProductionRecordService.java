package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.repositories.ProductionRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
@Service
public class ProductionRecordService {

    private final ProductionRecordRepository repository;

    public ProductionRecordService(ProductionRecordRepository repository) {
        this.repository = repository;
    }

    public List<ProductionRecordEntity> findAll() {
        return repository.findAllWithDetails();
    }

    public ProductionRecordEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запись с id " + id + " не найдена"));
    }

    public ProductionRecordEntity save(ProductionRecordEntity record) {
        return repository.save(record);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}