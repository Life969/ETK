package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.CouplingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CouplingRepository extends JpaRepository<CouplingEntity, Long> {
    @Query("SELECT DISTINCT c.type FROM CouplingEntity c WHERE c.type IS NOT NULL")
    List<String> findAllDistinctTypes();

    List<CouplingEntity> findByType(String type);
}
