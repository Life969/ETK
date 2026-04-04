package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.CouplingEntity;
import ProjectForJob.example.Job.repositories.CouplingRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CouplingService {

    private final CouplingRepository couplingRepository;

    public CouplingService(CouplingRepository couplingRepository) {
        this.couplingRepository = couplingRepository;
    }

    public List<CouplingEntity> findAll() {
        return couplingRepository.findAll();
    }

    public CouplingEntity findById(Long id) {
        return couplingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Муфта с id " + id + " не найдена"));
    }

    public CouplingEntity save(CouplingEntity coupling) {
        return couplingRepository.save(coupling);
    }

    public void deleteById(Long id) {
        couplingRepository.deleteById(id);
    }

    public List<String> findAllDistinctTypes() {
        return couplingRepository.findAllDistinctTypes();
    }

    public List<CouplingEntity> findByType(String type) {
        return couplingRepository.findByType(type);
    }
}