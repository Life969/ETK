package ProjectForJob.example.Job.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HomeOrderDto {
    private Long id;
    private Long productId;
    private String companyName;
    private String productType;
    private String productName;
    private Integer quantity;
    private Long daysUntilDeadline;// количество дней до дедлайна
    private LocalDate deadline; // дедлайн
    private String urgencyClass;      // класс Bootstrap для фона
}