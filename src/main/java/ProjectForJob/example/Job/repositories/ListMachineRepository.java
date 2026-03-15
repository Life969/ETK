package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.MachinesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListMachineRepository extends JpaRepository<MachinesEntity, Long> {
}
