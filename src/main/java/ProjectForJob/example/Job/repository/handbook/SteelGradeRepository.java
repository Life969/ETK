package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.SteelGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SteelGradeRepository extends JpaRepository<SteelGradeEntity, Long> {
    List<SteelGradeEntity> findAllByStrengthGroupIdOrderByNameAsc(Long groupId);
    List<SteelGradeEntity> findAllByStrengthGroupIdAndActiveTrueOrderByNameAsc(Long groupId);
}