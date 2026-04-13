package ProjectForJob.example.Job.DataTransferObject;

import lombok.Data;

@Data
public class HomeOrderDto {
    private Long id;
    private String companyName;
    private String productType;
    private String productName;
    private Integer quantity;
    private Long daysUntilDeadline;   // количество дней до дедлайна
    private String urgencyClass;      // класс Bootstrap для фона
}