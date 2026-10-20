package ProjectForJob.example.Job.repository.order;

import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
    Optional<CompanyEntity> findByNameIgnoreCase(String name);
}
