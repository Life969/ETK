package ProjectForJob.example.Job.entityJob.ForOrders;

import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.Handbook.MachinesEntity;
import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
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
@NamedEntityGraph(
        name = "ProductionRecord.details",
        attributeNodes = {
                @NamedAttributeNode("employee"),
                @NamedAttributeNode("machine"),
                @NamedAttributeNode("coupling"),
                @NamedAttributeNode("adapter")
        }
)
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

    // Метод для отображения названия изделия в таблице
    public BigDecimal getUnitPriceForEmployee() {
        if (coupling != null) return coupling.getPriceForEmployee();
        if (adapter != null) return adapter.getPriceForEmployee();
        return BigDecimal.ZERO;
    }

    public BigDecimal getUnitManufacturingCost() {
        if (coupling != null) return coupling.getManufacturingCost();
        if (adapter != null) return adapter.getManufacturingCost();
        return BigDecimal.ZERO;
    }

    public String getProductName() {
        if (coupling != null) return coupling.getName();
        if (adapter != null) return adapter.getFullName();
        return "";
    }



}