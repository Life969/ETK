package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.PipeAdapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PipeAdapterRepository extends JpaRepository<PipeAdapterEntity, Long> {

    @Query("SELECT DISTINCT p.firstSideType FROM PipeAdapterEntity p ORDER BY p.firstSideType")
    List<String> findAllDistinctFirstSideTypes();

    List<PipeAdapterEntity> findByFirstSideType(String type);
}