package ProjectForJob.example.Job.entityJob.Handbook;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "steel_strength_groups")
public class SteelStrengthGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Код группы обязателен")
    @Size(max = 10, message = "Код не должен превышать 10 символов")
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @NotBlank(message = "Наименование группы обязательно")
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "Процент надбавки обязателен")
    @DecimalMin(value = "0.0", message = "Процент не может быть отрицательным")
    @DecimalMax(value = "100.0", message = "Процент не может превышать 100")
    @Column(name = "markup_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal markupPercent;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private Boolean active;

    /**
     * Список марок этой группы.
     *
     * LAZY: в заказах нам группа нужна только для кода и процента,
     * марки туда не нужны. Тянем их только в справочнике групп —
     * через @EntityGraph на findAllByOrderByCodeAsc.
     */
    @OneToMany(mappedBy = "strengthGroup", fetch = FetchType.LAZY)
    @OrderBy("name ASC")
    private List<SteelGradeEntity> grades = new ArrayList<>();
}