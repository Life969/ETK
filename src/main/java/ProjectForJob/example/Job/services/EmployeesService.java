package ProjectForJob.example.Job.services;

import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.repositories.ListEmployeesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeesService {
    private final ListEmployeesRepository listEmployeesRepository;

    public List<EmployeesEntity> findAll() {
        return listEmployeesRepository.findAll();
    }

    public EmployeesEntity save(EmployeesEntity employee){
        return listEmployeesRepository.save(employee);
    }

    public EmployeesEntity findById(Long id){
        return listEmployeesRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id){
        listEmployeesRepository.deleteById(id);
    }
}
