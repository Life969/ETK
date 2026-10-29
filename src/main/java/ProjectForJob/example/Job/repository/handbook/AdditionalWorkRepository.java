package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.AdditionalWorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalWorkRepository extends JpaRepository<AdditionalWorkEntity, Long> {
}
