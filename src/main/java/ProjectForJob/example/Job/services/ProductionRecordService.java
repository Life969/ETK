package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import ProjectForJob.example.Job.repositories.ProductionRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    public Page<ProductionRecordEntity> findAllByFilters(Long couplingId, Long employeeId, Long machineId,
                                                         LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return repository.findAll(buildSpecification(couplingId, employeeId, machineId, startDate, endDate), pageable);
    }

    public List<ProductionRecordEntity> findAllByFilters(Long couplingId, Long employeeId, Long machineId,
                                                         LocalDate startDate, LocalDate endDate, Sort sort) {
        return repository.findAll(buildSpecification(couplingId, employeeId, machineId, startDate, endDate), sort);
    }


    //Чтобы не дублировать код построения Specification, вынес его в отдельный метод
    private Specification<ProductionRecordEntity> buildSpecification(Long couplingId, Long employeeId, Long machineId,
                                                                     LocalDate startDate, LocalDate endDate) {
        return (root,
                query,
                cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (couplingId != null) {
                predicates.add(cb.equal(root.get("coupling").get("id"), couplingId));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (machineId != null) {
                predicates.add(cb.equal(root.get("machine").get("id"), machineId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}