package ProjectForJob.example.Job.dto.kafkaDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalWorkDto {
    private Long id;
    private String name;
    private BigDecimal price;
}