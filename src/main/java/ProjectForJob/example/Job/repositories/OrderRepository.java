package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks"})
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks"})
    Optional<OrderEntity> findById(Long id);

    @EntityGraph(attributePaths = {"coupling", "additionalWorks", "company"})
    List<OrderEntity> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"company", "coupling", "adapter"})
    List<OrderEntity> findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus status);

    @Query("SELECT o FROM OrderEntity o " +
            "LEFT JOIN FETCH o.company " +
            "LEFT JOIN FETCH o.coupling " +
            "LEFT JOIN FETCH o.adapter " +
            "WHERE o.status = :status AND o.deadline IS NOT NULL " +
            "ORDER BY o.deadline")
    List<OrderEntity> findUrgentOrdersWithDetails(@Param("status") OrderStatus status, Pageable pageable);



    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks"})
    Page<OrderEntity> findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
            OrderStatus status, String companyName, Pageable pageable);;
}

