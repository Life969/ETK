package ProjectForJob.example.Job.entityJob.Handbook;

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
@Table(name = "couplings_data")
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

    @NotNull(message = "Стоимость изготовления обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "manufacturing_cost", precision = 10, scale = 2)
    private BigDecimal manufacturingCost;

    @NotNull(message = "Цена для работника обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "price_for_employee", precision = 6, scale = 2)
    private BigDecimal priceForEmployee;

    @Column(name = "image_path")
    private String imagePath; //правильно ли? так реализовывать или лучше хранить в бд фото?


    private String standart;

    private String description;


    public String getName() {
        return (type != null ? type : "") + " " + (conditionalDiameter != null ? conditionalDiameter : "");
    }

}