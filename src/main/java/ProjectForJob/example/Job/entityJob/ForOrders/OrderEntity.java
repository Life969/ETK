package ProjectForJob.example.Job.entityJob.ForOrders;

import ProjectForJob.example.Job.entityJob.Handbook.*;
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
    @JoinColumn(name = "coupling_id")
    private CouplingEntity coupling;

    @ManyToOne
    @JoinColumn(name = "adapter_id")
    private PipeAdapterEntity adapter;

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

    public String getProductName() {
        if (coupling != null) return coupling.getName();
        if (adapter != null) return adapter.getFullName();
        if (externalThread != null) {
            if (pipeProductKind == PipeProductKind.NIPPLE) {
                String len = (pieceLengthMm != null)
                        ? " L-" + pieceLengthMm.intValue()
                        : "";
                return "Патрубок " + externalThread.getName() + len;
            }
            return "Труба " + externalThread.getName();
        }
        return "";
    }

    // Поле для хранения наружнего диаметра заготовки в мм
    @Positive
    @Column(name = "workpiece_outer_diameter")
    private Double workpieceOuterDiameter; // мм

    // Поле для хранения Толщины стенки заготовки в мм
    @Positive
    @Column(name = "workpiece_wall_thickness")
    private Double workpieceWallThickness; // мм

    // Поле для хранения Длины заготовки в метрах
    @Positive
    @Column(name = "workpiece_length_meters")
    private Double workpieceLengthMeters; // общая длина заготовки, м

    // Вес заготовки в кг (может вычисляться автоматически)
    @Positive
    @Column(name = "workpiece_weight_kg")
    private Double workpieceWeightKg; // вес, кг

    // ================================================================
    // Трубы и патрубки
    // ================================================================

    /**
     * Наружная резьба. Nullable: null для муфт и переводников.
     */
    @ManyToOne
    @JoinColumn(name = "external_thread_id")
    private ExternalThreadEntity externalThread;

    /**
     * PIPE / NIPPLE. Nullable: null для муфт и переводников.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pipe_product_kind")
    private PipeProductKind pipeProductKind;

    /**
     * Толщина стенки, мм. Задаётся в заказе.
     */
    @Positive
    @Column(name = "wall_thickness_mm")
    private Double wallThicknessMm;

    /**
     * Средняя длина одной трубы, м. Дефолт 10. Только для PIPE.
     */
    @Positive
    @Column(name = "average_pipe_length_m")
    private Double averagePipeLengthM;

    /**
     * Общая длина заказа, м. Для PIPE и NIPPLE.
     */
    @Positive
    @Column(name = "order_total_length_m")
    private Double orderTotalLengthMeters;

    /**
     * Общий вес заказа, кг. Для PIPE и NIPPLE.
     */
    @Positive
    @Column(name = "order_total_weight_kg")
    private Double orderTotalWeightKg;

    /**
     * Цена за 1 тонну металла, ₽. Вводит менеджер.
     */
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price_per_ton", precision = 12, scale = 2)
    private BigDecimal pricePerTon;

    /**
     * Длина одной штуки, мм. Только для NIPPLE.
     */
    @Positive
    @Column(name = "piece_length_mm")
    private Double pieceLengthMm;

    // ================================================================
    // Группа прочности и марка стали (Фаза 2)
    // ================================================================

    /**
     * Группа прочности стали. Nullable — заказ может быть без группы.
     * Влияет на стоимость через appliedMarkupPercent (снимок на момент расчёта).
     */
    @ManyToOne
    @JoinColumn(name = "strength_group_id")
    private SteelStrengthGroupEntity strengthGroup;

    /**
     * Марка стали. Nullable — марка опциональна и носит информационный характер,
     * на цену не влияет (по ТЗ).
     */
    @ManyToOne
    @JoinColumn(name = "steel_grade_id")
    private SteelGradeEntity steelGrade;

    /**
     * Снимок процента надбавки за группу прочности на момент расчёта.
     * Хранится, чтобы при изменении процента в справочнике старые заказы
     * не пересчитывались "сами по себе".
     * 0, если группа не выбрана.
     */
    @Column(name = "applied_markup_percent", precision = 6, scale = 2)
    private BigDecimal appliedMarkupPercent;

    public String getProductType() {
        if (coupling != null) return "COUPLING";
        if (adapter != null) return "ADAPTER";
        if (pipeProductKind != null) return pipeProductKind.name();  // "PIPE"/"NIPPLE"
        return null;
    }

    public BigDecimal getUnitManufacturingCost() {
        if (coupling != null) return coupling.getManufacturingCost();
        if (adapter != null) return adapter.getManufacturingCost();
        return BigDecimal.ZERO;
    }
}