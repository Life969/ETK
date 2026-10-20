package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExternalThreadRepository extends JpaRepository<ExternalThreadEntity, Long> {

    List<ExternalThreadEntity> findAllByOrderByTypeAscConditionalDiameterAsc();

    @Query("SELECT DISTINCT e.type FROM ExternalThreadEntity e " +
            "WHERE e.type IS NOT NULL ORDER BY e.type")
    List<String> findAllDistinctTypes();

    List<ExternalThreadEntity> findByTypeOrderByConditionalDiameterAsc(String type);
}