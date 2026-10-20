package ProjectForJob.example.Job.service.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.EmployeeEntity;
import ProjectForJob.example.Job.repository.handbook.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public List<EmployeeEntity> findAll() {
        return employeeRepository.findAll();
    }

    public EmployeeEntity save(EmployeeEntity employee){
        return employeeRepository.save(employee);
    }

    public EmployeeEntity findById(Long id){
        return employeeRepository.findById(id).
                orElseThrow(() -> new NoSuchElementException("Сотрудник с id " + id + " не найдена"));
    }

    public void deleteById(Long id){
        employeeRepository.deleteById(id);
    }
    public void deleteAll(){
        employeeRepository.deleteAll();
    }
}
