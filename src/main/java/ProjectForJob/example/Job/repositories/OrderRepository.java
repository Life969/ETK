package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.ForOrders.OrderEntity;
import ProjectForJob.example.Job.entityJob.ForOrders.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = {"company", "coupling", "adapter",
            "additionalWorks", "externalThread", "strengthGroup", "steelGrade"})
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks",
            "externalThread", "strengthGroup", "steelGrade"})
    Optional<OrderEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks",
            "strengthGroup", "steelGrade"})
    Page<OrderEntity> findByStatusAndCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(
            OrderStatus status, String companyName, Pageable pageable);

    /**
     * Для срочных заказов на главной — HomeOrderDto не показывает группу/марку,
     * поэтому здесь дополнительных join'ов нет.
     */
    @EntityGraph(attributePaths = {"company", "coupling", "adapter"})
    List<OrderEntity> findByStatusAndDeadlineIsNotNullOrderByDeadlineAsc(OrderStatus status);

    /**
     * Для КП — используется CommercialOfferController, который отдаёт OrderEntity
     * напрямую и показывает только company/количество/сумму.
     * Группа/марка тут не нужны, оставляем как было.
     */
    @EntityGraph(attributePaths = {"company", "coupling", "adapter", "additionalWorks"})
    List<OrderEntity> findByIdIn(List<Long> ids);
}
