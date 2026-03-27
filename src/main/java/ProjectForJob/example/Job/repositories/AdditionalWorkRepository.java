package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.AdditionalWorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalWorkRepository extends JpaRepository<AdditionalWorkEntity, Long> {
}
