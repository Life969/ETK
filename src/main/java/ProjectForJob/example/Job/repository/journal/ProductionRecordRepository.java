package ProjectForJob.example.Job.repository;

import ProjectForJob.example.Job.repository.entity.order.ProductionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductionRecordRepository
        extends JpaRepository<ProductionRecordEntity, Long>,
        JpaSpecificationExecutor<ProductionRecordEntity> {

    /**
     * Явный @Query — иначе Spring пытается вывести запрос из имени метода
     * и ищет свойство "withDetails" в сущности.
     */
    @EntityGraph(value = "ProductionRecord.details", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT p FROM ProductionRecordEntity p")
    List<ProductionRecordEntity> findAllWithDetails();

    @EntityGraph(value = "ProductionRecord.details", type = EntityGraph.EntityGraphType.LOAD)
    List<ProductionRecordEntity> findTop5ByOrderByCreatedAtDesc();

    /** Переопределяем базовый findAll, чтобы подтянуть связи. */
    @Override
    @EntityGraph(value = "ProductionRecord.details", type = EntityGraph.EntityGraphType.LOAD)
    Page<ProductionRecordEntity> findAll(Specification<ProductionRecordEntity> spec, Pageable pageable);
}