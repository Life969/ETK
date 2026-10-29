package ProjectForJob.example.Job.service.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.MachineEntity;
import ProjectForJob.example.Job.repository.handbook.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MachinesService {
    private final MachineRepository machineRepository;

    public List<MachineEntity> findAll() {
        return machineRepository.findAll();
    }

    public MachineEntity save(MachineEntity machine){
        return machineRepository.save(machine);
    }

    public MachineEntity findById(Long id){
        return machineRepository.findById(id).
                orElseThrow(() -> new NoSuchElementException("Станок id " + id + " не найден"));
    }

    public void deleteById(Long id){
        machineRepository.deleteById(id);
    }
    public void deleteAll(){
        machineRepository.deleteAll();
    }
}
