package ProjectForJob.example.Job.entityJob;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "production_records")
public class ProductionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Дата изготовления обязательна")
    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupling_id")
    private CouplingEntity coupling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adapter_id")
    private PipeAdapterEntity adapter;

    @Min(value = 1, message = "Количество должно быть не менее 1")
    @Max(value = 999999, message = "Количество не может превышать 999999")
    @NotNull(message = "Количество обязательно")
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeesEntity employee;

    @ManyToOne
    @JoinColumn(name = "machine_id", nullable = false)
    private MachinesEntity machine;

    private String comment;

    // Автоматическая установка даты записи @PrePersist — метод, который срабатывает перед сохранением в БД, устанавливает текущую дату.
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getProductType() {
        if (coupling != null) return "COUPLING";
        if (adapter != null) return "ADAPTER";
        return null;
    }
    public String getProductTypeDisplayName() {
        if (coupling != null) return "Муфта";
        if (adapter != null) return "Переводник";
        return "—";
    }

    // Метод для отображения названия изделия в таблице
    public String getProductName() {
        if (coupling != null) {
            return coupling.getName();
        } else if (adapter != null) {
            return adapter.getFullName();
        }
        return "";
    }

    // Метод для получения ID изделия (для скрытого поля формы)
    public Long getProductId() {
        if (coupling != null) return coupling.getId();
        if (adapter != null) return adapter.getId();
        return null;
    }

    public BigDecimal getUnitPriceForEmployee() {
        if (coupling != null) {
            return coupling.getPriceForEmployee();
        } else if (adapter != null) {
            return adapter.getPriceForEmployee();
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getUnitManufacturingCost() {
        if (coupling != null) {
            return coupling.getManufacturingCost();
        } else if (adapter != null) {
            return adapter.getManufacturingCost();
        }
        return BigDecimal.ZERO;
    }



}