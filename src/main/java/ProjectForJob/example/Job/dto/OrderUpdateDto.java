package ProjectForJob.example.Job.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderUpdateDto {
    @NotBlank(message = "Название компании обязательно")
    private String companyName;

    @NotNull(message = "Выберите тип продукции")
    private String productType; // "COUPLING" или "ADAPTER"

    private Long couplingId;
    private Long adapterId;

    private Integer quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    private List<Long> additionalWorkIds;

    // --- для труб / патрубков ---
    private Long externalThreadId;
    private String pipeProductKind;              // "PIPE" / "NIPPLE" / null
    private Double wallThicknessMm;
    private Double averagePipeLengthM;           // для PIPE, дефолт 10
    private Double orderTotalLengthMeters;       // для PIPE
    private Double orderTotalWeightKg;           // для PIPE
    private BigDecimal pricePerTon;              // для PIPE и NIPPLE
    private Double pieceLengthMm;                // для NIPPLE

    // === Группа прочности и марка стали  — оба опциональны ===
    private Long strengthGroupId;
    private Long steelGradeId;

    @AssertTrue(message = "Количество обязательно для муфты, переводника и патрубка")
    public boolean isQuantityValid() {
        // Для трубы (PIPE) количество вычисляется на сервере — не проверяем.
        if ("PIPE".equals(productType)) {
            return true;
        }
        return quantity != null && quantity > 0;
    }

    @AssertTrue(message = "Выберите ровно один продукт: муфту, переводник, трубу или патрубок")
    public boolean isProductValid() {
        boolean c = couplingId != null;
        boolean a = adapterId != null;
        boolean t = externalThreadId != null;

        int count = (c ? 1 : 0) + (a ? 1 : 0) + (t ? 1 : 0);
        return count == 1;
    }

    @AssertTrue(message = "Для трубы/патрубка укажите вид продукции (PIPE/NIPPLE)")
    public boolean isPipeKindValid() {
        if (externalThreadId == null) return true;
        return "PIPE".equals(pipeProductKind) || "NIPPLE".equals(pipeProductKind);
    }
}