package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.SteelGradeEntity;
import ProjectForJob.example.Job.entityJob.Handbook.SteelStrengthGroupEntity;
import ProjectForJob.example.Job.repositories.Handbook.SteelGradeRepository;
import ProjectForJob.example.Job.repositories.Handbook.SteelStrengthGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteelStrengthGroupService {

    private final SteelStrengthGroupRepository groupRepository;
    private final SteelGradeRepository gradeRepository;

    // ================== ГРУППЫ ==================

    @Transactional(readOnly = true)
    public List<SteelStrengthGroupEntity> findAll() {
        return groupRepository.findAllByOrderByCodeAsc();
    }

    public List<SteelStrengthGroupEntity> findAllActive() {
        return groupRepository.findAllByActiveTrueOrderByCodeAsc();
    }

    public SteelStrengthGroupEntity findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Группа прочности " + id + " не найдена"));
    }

    /**
     * Сохранение группы.
     *
     * Инвариант: is_default=true одновременно может быть только у одной группы.
     *
     * - Если сохраняем новую дефолтную → снимаем флаг у предыдущей.
     * - Если снимаем флаг с единственной дефолтной → бросаем ошибку
     *   (дефолтная группа должна существовать всегда).
     */
    @Transactional
    public SteelStrengthGroupEntity save(SteelStrengthGroupEntity group) {
        if (Boolean.TRUE.equals(group.getIsDefault())) {
            // назначаем новую дефолтную — снимаем флаг с предыдущей
            groupRepository.findByIsDefaultTrue().ifPresent(prev -> {
                if (!prev.getId().equals(group.getId())) {
                    prev.setIsDefault(false);
                    groupRepository.save(prev);
                }
            });
        } else {
            // пытаются снять флаг. Проверяем, не единственная ли это дефолтная
            groupRepository.findByIsDefaultTrue().ifPresent(prev -> {
                if (prev.getId().equals(group.getId())) {
                    throw new IllegalStateException(
                            "Нельзя снять флаг «по умолчанию»: должна быть хотя бы одна " +
                                    "дефолтная группа. Сначала назначьте дефолтной другую группу.");
                }
            });
        }
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteById(Long id) {
        groupRepository.deleteById(id);
    }

    // ================== МАРКИ ==================

    public List<SteelGradeEntity> findGradesByGroup(Long groupId) {
        return gradeRepository.findAllByStrengthGroupIdOrderByNameAsc(groupId);
    }

    public List<SteelGradeEntity> findActiveGradesByGroup(Long groupId) {
        return gradeRepository.findAllByStrengthGroupIdAndActiveTrueOrderByNameAsc(groupId);
    }

    public SteelGradeEntity findGradeById(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Марка стали " + id + " не найдена"));
    }

    @Transactional
    public SteelGradeEntity saveGrade(SteelGradeEntity grade) {
        return gradeRepository.save(grade);
    }

    @Transactional
    public void deleteGradeById(Long id) {
        gradeRepository.deleteById(id);
    }
}