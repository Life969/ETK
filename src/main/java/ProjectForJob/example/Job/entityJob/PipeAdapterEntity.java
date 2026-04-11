package ProjectForJob.example.Job.entityJob;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pipe_adapters_data")
public class PipeAdapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Тип переводника обязателен")
    @Column(name = "first_side_type", nullable = false)
    private String firstSideType;

    @NotBlank(message = "Диаметр переводника обязателен")
    @Column(name = "first_side_diameter", nullable = false)
    private String firstSideDiameter;

    @NotBlank(message = "Тип переводника обязателен")
    @Column(name = "second_side_type", nullable = false)
    private String secondSideType;

    @NotBlank(message = "Диаметр переводника обязателен")
    @Column(name = "second_side_diameter", nullable = false)
    private String secondSideDiameter;

    @Positive(message = "Длинна должна быть положительна")
    @Column(name = "length_mm")
    private Double lengthMm;

    @Positive(message = "Вес должен быть положительным")
    @Column(name = "weight_kg")
    private Double weightKg;

    @Positive(message = "Наружный диаметр должен быть положительным")
    @Column(name = "outer_diameter_mm")
    private Double outerDiameterMm;

    @Positive(message = "Внутренний диаметр должен быть положительным")
    @Column(name = "inner_diameter_mm")
    private Double innerDiameterMm;

    @NotNull(message = "Стоимость изготовления обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "manufacturing_cost", nullable = false)
    private BigDecimal manufacturingCost;

    @NotNull(message = "Стоимость изготовления обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "price_for_employee", nullable = false)
    private BigDecimal priceForEmployee;

    @Column(name = "standart")
    private String standart;

    @Column(name = "description")
    private String description;

    @Column(name = "image_path")
    private String imagePath;

    // Метод для отображения полного наименования
    public String getFullName() {
        return String.format("%s %s x %s %s",
                firstSideType, firstSideDiameter, secondSideType, secondSideDiameter);
    }
}
