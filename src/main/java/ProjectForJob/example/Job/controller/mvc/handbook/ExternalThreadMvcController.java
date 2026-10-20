package ProjectForJob.example.Job.controller.mvc.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.ExternalThreadEntity;
import ProjectForJob.example.Job.service.handbook.ExternalThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Controller
@RequestMapping("/external-threads")
@RequiredArgsConstructor
@Slf4j
public class ExternalThreadMvcController {

    private final ExternalThreadService service;

    private static final String LAYOUT = "welcomeToHome/layout";
    private static final String FRAGMENT_LIST   = "fragments/external-threads/list :: content";
    private static final String FRAGMENT_FORM   = "fragments/external-threads/form :: content";
    private static final String FRAGMENT_DETAIL = "fragments/external-threads/detail :: content";

    // ============================================================
    // Компаратор диаметров: числовая сортировка, если парсится.
    // "73" < "89" < "102" — а не "102" < "73" < "89" (лексикографика).
    // ============================================================
    private static final Comparator<String> DIAMETER_COMPARATOR = (a, b) -> {
        Integer ia = tryParseInt(a);
        Integer ib = tryParseInt(b);
        if (ia != null && ib != null) return Integer.compare(ia, ib);
        if (ia != null) return -1;   // числа идут раньше не-чисел
        if (ib != null) return 1;
        return String.CASE_INSENSITIVE_ORDER.compare(
                a == null ? "" : a,
                b == null ? "" : b);
    };

    private static Integer tryParseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ============================================================
    // Список — аккордеон по типу резьбы (НКТ, ОТТМ, ...)
    // ============================================================
    @GetMapping
    public String list(Model model) {
        List<ExternalThreadEntity> all = service.findAll();

        // LinkedHashMap сохраняет порядок, в котором пришли типы (уже ASC)
        Map<String, List<ExternalThreadEntity>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getType() != null ? e.getType() : "—",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Сортируем диаметры внутри каждой группы по возрастанию (числовая)
        grouped.forEach((type, list) ->
                list.sort(Comparator.comparing(
                        ExternalThreadEntity::getConditionalDiameter,
                        Comparator.nullsLast(DIAMETER_COMPARATOR))));

        model.addAttribute("groupedThreads", grouped);
        model.addAttribute("totalThreads", all.size());
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return LAYOUT;
    }

    // ============================================================
    // Создание
    // ============================================================
    @GetMapping("/new")
    public String showCreate(Model model) {
        model.addAttribute("externalThread", new ExternalThreadEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ============================================================
    // Редактирование
    // ============================================================
    @GetMapping("/edit/{id}")
    public String showEdit(@PathVariable Long id, Model model) {
        model.addAttribute("externalThread", service.findById(id));
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return LAYOUT;
    }

    // ============================================================
    // Сохранение
    // ============================================================
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("externalThread") ExternalThreadEntity entity,
                       BindingResult result,
                       @RequestParam(value = "imageBase64", required = false) String imageBase64,
                       Model model,
                       RedirectAttributes redirectAttributes) throws IOException {

        if (result.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return LAYOUT;
        }

        // ==== Картинка ====
        if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.startsWith("data:image")) {
            String base64Data = imageBase64.substring(imageBase64.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            String contentType = imageBase64.substring(5, imageBase64.indexOf(";"));
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/gif"  -> ".gif";
                default -> ".jpg";
            };

            // Удаляем старую картинку при редактировании
            if (entity.getId() != null) {
                ExternalThreadEntity existing = service.findById(entity.getId());
                deleteImageFile(existing != null ? existing.getImagePath() : null);
            }

            String newFileName = UUID.randomUUID() + extension;
            String uploadDir = System.getProperty("user.dir") + "/uploads/threads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            File destFile = new File(uploadDir + newFileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(imageBytes);
            }

            entity.setImagePath("/uploads/threads/" + newFileName);

        } else if (entity.getId() != null) {
            // Новая картинка не пришла — оставляем прежнюю
            ExternalThreadEntity existing = service.findById(entity.getId());
            entity.setImagePath(existing != null ? existing.getImagePath() : null);
        }

        service.save(entity);
        redirectAttributes.addFlashAttribute("message", "Резьба сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/external-threads";
    }

    // ============================================================
    // Удаление
    // ============================================================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("DELETE external thread id={}", id);

        ExternalThreadEntity existing = service.findById(id);
        deleteImageFile(existing != null ? existing.getImagePath() : null);

        service.deleteById(id);

        redirectAttributes.addFlashAttribute("message", "Резьба удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/external-threads";
    }

    // ============================================================
    // Просмотр — детальная страница
    // ВАЖНО: объявлен ПОСЛЕ всех literal-путей (/new, /edit, /api/*),
    //        чтобы /{id} не перехватил их.
    // ============================================================
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        ExternalThreadEntity thread = service.findById(id);
        model.addAttribute("externalThread", thread);
        model.addAttribute("contentTemplate", FRAGMENT_DETAIL);
        return LAYOUT;
    }

    // ============================================================
    // REST API — каскад «тип → диаметр» (для формы заказа в Фазе 2)
    // ============================================================

    @GetMapping("/api/types")
    @ResponseBody
    public List<String> getTypes() {
        return service.findAllDistinctTypes();
    }

    @GetMapping("/api/by-type")
    @ResponseBody
    public List<Map<String, Object>> getByType(@RequestParam String type) {
        return service.findByType(type).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("conditionalDiameter", t.getConditionalDiameter());
                    m.put("name", t.getName());
                    m.put("manufacturingCost", t.getManufacturingCost());
                    m.put("priceForEmployee", t.getPriceForEmployee());
                    return m;
                })
                .toList();
    }

    private void deleteImageFile(String imagePath) {
        if (imagePath != null && imagePath.startsWith("/uploads/")) {
            try {
                File file = new File(System.getProperty("user.dir") + imagePath);
                if (file.exists()) file.delete();
            } catch (Exception e) {
                log.warn("Не удалось удалить старый файл: {}", imagePath, e);
            }
        }
    }
}