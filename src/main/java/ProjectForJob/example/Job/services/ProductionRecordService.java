package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.ForOrders.ProductionRecordEntity;
import ProjectForJob.example.Job.repositories.ProductionRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public Page<ProductionRecordEntity> findAllByFilters(String productType,
                                                         Long couplingId,
                                                         Long adapterId,
                                                         Long employeeId,
                                                         Long machineId,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Pageable pageable) {

        Specification<ProductionRecordEntity> spec = buildSpecification(productType, couplingId, adapterId, employeeId, machineId, startDate, endDate);
        return repository.findAll(spec, pageable);
    }

    public List<ProductionRecordEntity> findAllByFilters(Long couplingId,
                                                         Long employeeId,
                                                         Long machineId,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Sort sort) {
        // Передаём productType = null, adapterId = null для совместимости со старой сигнатурой
        Specification<ProductionRecordEntity> spec = buildSpecification(null, couplingId, null, employeeId, machineId, startDate, endDate);
        return repository.findAll(spec, sort);
    }


    // Универсальный метод построения спецификации
    private Specification<ProductionRecordEntity> buildSpecification(String productType,
                                                                     Long couplingId,
                                                                     Long adapterId,
                                                                     Long employeeId,
                                                                     Long machineId,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if ("COUPLING".equals(productType)) {
                predicates.add(cb.isNotNull(root.get("coupling")));
                predicates.add(cb.isNull(root.get("adapter")));
            } else if ("ADAPTER".equals(productType)) {
                predicates.add(cb.isNull(root.get("coupling")));
                predicates.add(cb.isNotNull(root.get("adapter")));
            }

            // Фильтр по конкретной муфте
            if (couplingId != null) {
                predicates.add(cb.equal(root.get("coupling").get("id"), couplingId));
            }

            // Фильтр по конкретному переходнику
            if (adapterId != null) {
                predicates.add(cb.equal(root.get("adapter").get("id"), adapterId));
            }

            // Фильтр по сотруднику
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }

            // Фильтр по станку
            if (machineId != null) {
                predicates.add(cb.equal(root.get("machine").get("id"), machineId));
            }

            // Фильтр по дате начала
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), startDate));
            }

            // Фильтр по дате окончания
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<ProductionRecordEntity> findLast5Records() {
        return repository.findTop5ByOrderByCreatedAtDesc();
    }
}