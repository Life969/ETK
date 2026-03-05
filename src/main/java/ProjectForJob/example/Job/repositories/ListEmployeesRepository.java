package ProjectForJob.example.Job.repositories;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListEmployeesRepository extends JpaRepository<EmployeesEntity, Long> {
}
