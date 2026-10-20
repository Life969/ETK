package ProjectForJob.example.Job.repository.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListMachineRepository extends JpaRepository<MachineEntity, Long> {
}
