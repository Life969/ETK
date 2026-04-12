package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.MachinesEntity;
import ProjectForJob.example.Job.repositories.Handbook.ListMachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MachinesService {
    private final ListMachineRepository listMachineRepository;

    public List<MachinesEntity> findAll() {
        return listMachineRepository.findAll();
    }

    public MachinesEntity save(MachinesEntity machine){
        return listMachineRepository.save(machine);
    }

    public MachinesEntity findById(Long id){
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
