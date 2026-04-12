package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.EmployeesEntity;
import ProjectForJob.example.Job.repositories.Handbook.ListEmployeesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

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
        return listEmployeesRepository.findById(id).
                orElseThrow(() -> new NoSuchElementException("Сотрудник с id " + id + " не найдена"));
    }

    public void deleteById(Long id){
        listEmployeesRepository.deleteById(id);
    }
    public void deleteAll(){
        listEmployeesRepository.deleteAll();
    }
}
