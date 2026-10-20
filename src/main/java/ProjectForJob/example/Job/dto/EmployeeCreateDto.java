package ProjectForJob.example.Job.DataTransferObject;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class EmployeeCreateDto {

    @NotBlank(message = "Имя не может быть пустым")
    private String name;

}
