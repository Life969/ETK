package ProjectForJob.example.Job.DataTransferObject;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDto {
    @NotBlank(message = "Название компании обязательно")
    private String companyName;


    @NotNull(message = "Выберите тип продукции")
    private String productType; // "COUPLING" или "ADAPTER"

    private Long couplingId;
    private Long adapterId;

    @NotNull(message = "Количество обязательно")
    @Positive(message = "Количество должно быть больше 0")
    private Integer quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline; // необязательное

    private List<Long> additionalWorkIds; // id выбранных доп. работ

    @AssertTrue(message = "Необходимо выбрать продукцию соответствующего типа")
    public boolean isProductValid() {
        if ("COUPLING".equals(productType)) {
            return couplingId != null && adapterId == null;
        } else if ("ADAPTER".equals(productType)) {
            return adapterId != null && couplingId == null;
        }
        return false;
    }
}
