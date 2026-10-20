package ProjectForJob.example.Job.domain.entity.handbook;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Справочник НАРУЖНЫХ резьб (трубы и патрубки).
 *
 * Используется как общий справочник и для труб, и для патрубков —
 * различие PIPE / NIPPLE определяется на уровне заказа.
 *
 * Цены:
 *  - manufacturingCost — за 1 нарезку (у трубы/патрубка их 2)
 *  - priceForEmployee  — за 1 готовое изделие (2 резьбы)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "external_threads_data")
public class ExternalThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Тип наружной резьбы: НКТ, ОТТМ, ОТТГ, ... */
    @NotBlank(message = "Тип резьбы обязателен")
    @Column(name = "type", nullable = false)
    private String type;

    @NotBlank(message = "Условный диаметр обязателен")
    @Column(name = "conditional_diameter", nullable = false)
    private String conditionalDiameter;

    /** Цена изготовления за 1 нарезку, руб */
    @NotNull(message = "Стоимость изготовления обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "manufacturing_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal manufacturingCost;

    /** Цена для работника за 1 готовое изделие (2 резьбы), руб */
    @NotNull(message = "Цена для работника обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Column(name = "price_for_employee", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceForEmployee;

    @Column(name = "standart")
    private String standart;

    @Column(name = "description")
    private String description;

    @Column(name = "image_path")
    private String imagePath;

    /**
     * Отображаемое имя: "НКТ 73".
     * replaceAll("\\s+", " ") — защита от ведущего пробела, если type == null.
     */
    public String getName() {
        String t = type != null ? type : "";
        String d = conditionalDiameter != null ? conditionalDiameter : "";
        return (t + " " + d).replaceAll("\\s+", " ").trim();
    }
}