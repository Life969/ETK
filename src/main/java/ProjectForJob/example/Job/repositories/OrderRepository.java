package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.OrderEntity;
import ProjectForJob.example.Job.entityJob.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"company", "coupling", "additionalWorks"})
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "coupling", "additionalWorks"})
    Optional<OrderEntity> findById(Long id);

    @EntityGraph(attributePaths = {"coupling", "additionalWorks", "company"})
    List<OrderEntity> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"company", "coupling", "additionalWorks"})
    Page<OrderEntity> findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
            OrderStatus status, String companyName, Pageable pageable);
}
