package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.ProductionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionRecordRepository extends JpaRepository<ProductionRecordEntity, Long>, JpaSpecificationExecutor<ProductionRecordEntity> {
    @Query("SELECT pr FROM ProductionRecordEntity pr " +
            "JOIN FETCH pr.coupling " +
            "JOIN FETCH pr.employee " +
            "JOIN FETCH pr.machine")
    List<ProductionRecordEntity> findAllWithDetails();

    Page<ProductionRecordEntity> findAll(Pageable pageable);
    Page<ProductionRecordEntity> findAll(Specification<ProductionRecordEntity> spec, Pageable pageable);
}
