package ProjectForJob.example.Job.entityJob;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "couplings")
public class CouplingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Тип муфты обязателен")
    private String type;

    @NotBlank(message = "Условный диаметр обязателен")
    @Column(name = "conditional_diameter")
    private String conditionalDiameter;

    @Positive(message = "Длина должна быть положительной")
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

    @NotNull(message = "Цена изготовления обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "manufacturing_cost", precision = 10, scale = 2)
    private BigDecimal manufacturingCost;

    @Column(name = "image_path")
    private String imagePath;


    private String standart;

    private String description;


    public String getName() {
        return (type != null ? type : "") + " " + (conditionalDiameter != null ? conditionalDiameter : "");
    }

}