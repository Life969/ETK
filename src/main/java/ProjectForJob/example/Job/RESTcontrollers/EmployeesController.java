package ProjectForJob.example.Job.RESTcontrollers;

import ProjectForJob.example.Job.DataTransferObject.EmployeeCreateDto;
import ProjectForJob.example.Job.DataTransferObject.EmployeeResponseDto;
import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.services.EmployeesService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
public class EmployeesController {
    private static final Logger log = LoggerFactory.getLogger(EmployeesController.class);
    private final EmployeesService employeesService;

    public EmployeesController(
            EmployeesService employeesService) {
        this.employeesService = employeesService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(
            @PathVariable("id") Long id){
        log.info("called method getEmployeeById id = " + id);
        try {
            EmployeesEntity entity = employeesService.findById(id);
            return ResponseEntity.ok(convertToDto(entity));

        }catch (NoSuchElementException e){
            return ResponseEntity.status(404).build();
        }

    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees(){
        log.info("called method findAllEmployees");
        List<EmployeesEntity> list = employeesService.findAll();
        List<EmployeeResponseDto> dtos = list.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);

    }

    @PostMapping
    public ResponseEntity<EmployeeResponseDto> createEmployee(@RequestBody
                                                                  @Valid EmployeeCreateDto createDto) {
        EmployeesEntity entity = convertToEntity(createDto);
        EmployeesEntity saved = employeesService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeesService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteEmployee() {
        employeesService.deleteAll();
        return ResponseEntity.noContent().build();
    }




    //Создание объектов DTO
    private EmployeeResponseDto convertToDto(EmployeesEntity entity) {
        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    private EmployeesEntity convertToEntity(EmployeeCreateDto dto) {
        EmployeesEntity entity = new EmployeesEntity();
        entity.setName(dto.getName());
        return entity;
    }

}
