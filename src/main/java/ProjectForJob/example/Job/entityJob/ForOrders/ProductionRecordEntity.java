package ProjectForJob.example.Job.entityJob.ForOrders;

import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.entityJob.Handbook.EmployeesEntity;
import ProjectForJob.example.Job.entityJob.Handbook.ExternalThreadEntity;
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
                @NamedAttributeNode("adapter"),
                @NamedAttributeNode("externalThread")
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

    // ================================================================
    // Трубы и патрубки
    // ================================================================

    /** Наружная резьба. Nullable: null для муфт и переводников. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_thread_id")
    private ExternalThreadEntity externalThread;

    /** PIPE / NIPPLE. Nullable: null для муфт и переводников. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pipe_product_kind")
    private PipeProductKind pipeProductKind;

    /** Длина патрубка, мм. Заполняется только для NIPPLE. */
    @Positive(message = "Длина патрубка должна быть положительной")
    @Column(name = "piece_length_mm")
    private Double pieceLengthMm;

    // ================================================================

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Тип продукции: COUPLING / ADAPTER / PIPE / NIPPLE. */
    public String getProductType() {
        if (coupling != null) return "COUPLING";
        if (adapter != null) return "ADAPTER";
        if (pipeProductKind != null) return pipeProductKind.name();
        return null;
    }

    /** Цена для работника за 1 изделие (для отчёта по сотруднику). */
    public BigDecimal getUnitPriceForEmployee() {
        if (coupling != null) return coupling.getPriceForEmployee();
        if (adapter != null) return adapter.getPriceForEmployee();
        if (externalThread != null) return externalThread.getPriceForEmployee();
        return BigDecimal.ZERO;
    }

    /** Цена изготовления за 1 изделие (для отчёта по станку). */
    public BigDecimal getUnitManufacturingCost() {
        if (coupling != null) return coupling.getManufacturingCost();
        if (adapter != null) return adapter.getManufacturingCost();
        if (externalThread != null) return externalThread.getManufacturingCost();
        return BigDecimal.ZERO;
    }

    /** Название изделия для отображения в таблице. */
    public String getProductName() {
        if (coupling != null) return coupling.getName();
        if (adapter != null) return adapter.getFullName();
        if (externalThread != null) return externalThread.getName();  // "НКТ 73"
        return "";
    }
}