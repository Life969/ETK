package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.domain.Handbook.MachinesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListMachineRepository extends JpaRepository<MachinesEntity, Long> {
}
