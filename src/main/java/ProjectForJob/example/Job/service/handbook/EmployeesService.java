package ProjectForJob.example.Job.service.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.EmployeeEntity;
import ProjectForJob.example.Job.repository.handbook.ListEmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EmployeesService {
    private final ListEmployeeRepository listEmployeeRepository;

    public List<EmployeeEntity> findAll() {
        return listEmployeeRepository.findAll();
    }

    public EmployeeEntity save(EmployeeEntity employee){
        return listEmployeeRepository.save(employee);
    }

    public EmployeeEntity findById(Long id){
        return listEmployeeRepository.findById(id).
                orElseThrow(() -> new NoSuchElementException("Сотрудник с id " + id + " не найдена"));
    }

    public void deleteById(Long id){
        listEmployeeRepository.deleteById(id);
    }
    public void deleteAll(){
        listEmployeeRepository.deleteAll();
    }
}
