package ProjectForJob.example.Job.DataTransferObject;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderUpdateDto {
    @NotBlank(message = "Название компании обязательно")
    private String companyName;

    @NotNull(message = "Выберите продукцию")
    private Long couplingId;

    @NotNull(message = "Количество обязательно")
    @Positive(message = "Количество должно быть больше 0")
    private Integer quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    private List<Long> additionalWorkIds;
}