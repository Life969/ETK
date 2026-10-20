package ProjectForJob.example.Job.DataTransferObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EmployeeResponseDto {
    private Long id;
    private String name;

    public EmployeeResponseDto() {

    }
}
