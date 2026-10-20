package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.domain.Handbook.SteelStrengthGroupEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SteelStrengthGroupRepository extends JpaRepository<SteelStrengthGroupEntity, Long> {

    /** Для страницы справочника: группы + марки одним запросом. */
    @EntityGraph(attributePaths = "grades")
    List<SteelStrengthGroupEntity> findAllByOrderByCodeAsc();

    /** Для формы заказа: только группы (марки не нужны). */
    List<SteelStrengthGroupEntity> findAllByActiveTrueOrderByCodeAsc();

    Optional<SteelStrengthGroupEntity> findByCodeIgnoreCase(String code);
    Optional<SteelStrengthGroupEntity> findByIsDefaultTrue();
}