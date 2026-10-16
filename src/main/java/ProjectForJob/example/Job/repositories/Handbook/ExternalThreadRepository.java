package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.ExternalThreadEntity;
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