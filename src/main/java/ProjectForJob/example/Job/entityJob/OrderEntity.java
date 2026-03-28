package ProjectForJob.example.Job.entityJob;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Дата формирования заказа
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Компания-заказчик
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    // Продукция (муфта)
    @ManyToOne
    @JoinColumn(name = "coupling_id", nullable = false)
    private CouplingEntity coupling;

    @Positive
    @Column(nullable = false)
    private Integer quantity; // количество изготавливаемой продукции

    // Дедлайн – опционально
    @Column(name = "deadline")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    // Список дополнительных работ
    @ManyToMany
    @JoinTable(
            name = "order_additional_work",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "additional_work_id")
    )
    private List<AdditionalWorkEntity> additionalWorks = new ArrayList<>();

    // Статус заказа
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Поле для хранения общей стоимости (можно вычислять, но лучше хранить для отчётов)
    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;
}