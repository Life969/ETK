package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PipeAdapterRepository extends JpaRepository<PipeAdapterEntity, Long> {

    @Query("SELECT DISTINCT p.firstSideType FROM PipeAdapterEntity p ORDER BY p.firstSideType")
    List<String> findAllDistinctFirstSideTypes();

    List<PipeAdapterEntity> findByFirstSideType(String type);
}