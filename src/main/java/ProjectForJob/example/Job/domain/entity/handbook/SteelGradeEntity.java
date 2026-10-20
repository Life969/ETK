package ProjectForJob.example.Job.repository.entity.handbook;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "steel_grades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_steel_grades_group_name",
                columnNames = {"strength_group_id", "name"}))
public class SteelGradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Марка принадлежит группе. По ТЗ группы/марки опциональны в заказе,
     * но марка без группы существовать не может — это инвариант модели.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "strength_group_id", nullable = false)
    private SteelStrengthGroupEntity strengthGroup;

    @NotBlank(message = "Наименование марки обязательно")
    @Size(max = 100, message = "Наименование не должно превышать 100 символов")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Boolean active;
}