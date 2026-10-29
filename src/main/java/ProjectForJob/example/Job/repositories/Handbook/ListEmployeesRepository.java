package ProjectForJob.example.Job.repositories.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.EmployeesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListEmployeesRepository extends JpaRepository<EmployeesEntity, Long> {
}
