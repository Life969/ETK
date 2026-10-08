package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.SteelGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SteelGradeRepository extends JpaRepository<SteelGradeEntity, Long> {
    List<SteelGradeEntity> findAllByStrengthGroupIdOrderByNameAsc(Long groupId);
    List<SteelGradeEntity> findAllByStrengthGroupIdAndActiveTrueOrderByNameAsc(Long groupId);
}