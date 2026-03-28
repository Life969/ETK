package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.OrderEntity;
import ProjectForJob.example.Job.entityJob.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
