package ProjectForJob.example.Job.service.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.MachineEntity;
import ProjectForJob.example.Job.repository.handbook.ListMachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MachinesService {
    private final ListMachineRepository listMachineRepository;

    public List<MachineEntity> findAll() {
        return listMachineRepository.findAll();
    }

    public MachineEntity save(MachineEntity machine){
        return listMachineRepository.save(machine);
    }

    public MachineEntity findById(Long id){
        return listMachineRepository.findById(id).
                orElseThrow(() -> new NoSuchElementException("Станок id " + id + " не найден"));
    }

    public void deleteById(Long id){
        listMachineRepository.deleteById(id);
    }
    public void deleteAll(){
        listMachineRepository.deleteAll();
    }
}
