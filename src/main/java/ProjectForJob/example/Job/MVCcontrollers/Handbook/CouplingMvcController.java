package ProjectForJob.example.Job.MVCcontrollers.Handbook;


import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/couplings")
@RequiredArgsConstructor
@Slf4j
public class CouplingMvcController {

    private final CouplingService couplingService;



    @GetMapping()
    public String listAllGrouped(Model model) {
        log.info("GET /couplings - группировка по типам");
        List<CouplingEntity> allCouplings = couplingService.findAll();

        // Группируем по типу
        Map<String, List<CouplingEntity>> groupedByType = allCouplings.stream()
                .collect(Collectors.groupingBy(
                        CouplingEntity::getType,
                        TreeMap::new,  // чтобы ключи (типы) были отсортированы
                        Collectors.toList()
                ));

        // Сортируем каждую группу по условному диаметру (как число)
        groupedByType.forEach((type, list) ->
                list.sort(Comparator.comparingInt(c -> Integer.parseInt(c.getConditionalDiameter())))
        );

        model.addAttribute("groupedCouplings", groupedByType);
        return "couplings/list"; // та же страница, но теперь с groupedCouplings
    }

    @GetMapping("/{id}")
    public String showCoupling(@PathVariable Long id, Model model) {
        log.info("GET /couplings/{} - карточка муфты", id);
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        return "couplings/detail";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("GET /couplings/new - форма создания");
        model.addAttribute("coupling", new CouplingEntity());
        return "couplings/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("coupling") CouplingEntity coupling,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageBase64", required = false) String imageBase64,
                       RedirectAttributes redirectAttributes) throws IOException {
        log.info("POST /couplings/save - сохранение муфты: {}", coupling);
        if (bindingResult.hasErrors()) {
            return "couplings/form";
        }

        // Обработка изображения из base64
        if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.startsWith("data:image")) {
            // Извлекаем часть после "base64,"
            String base64Data = imageBase64.substring(imageBase64.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Определяем расширение (можно из contentType)
            String contentType = imageBase64.substring(5, imageBase64.indexOf(";"));
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };

            // Удаляем старый файл, если редактируем
            if (coupling.getId() != null) {
                CouplingEntity existing = couplingService.findById(coupling.getId());
                String oldImagePath = existing.getImagePath();
                if (oldImagePath != null && !oldImagePath.isEmpty()) {
                    deleteImageFile(oldImagePath);
                }
            }

            // Сохраняем файл
            String newFileName = UUID.randomUUID().toString() + extension;
            String uploadDir = System.getProperty("user.dir") + "/uploads/couplings/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            File destFile = new File(uploadDir + newFileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(imageBytes);
            }

            coupling.setImagePath("/uploads/couplings/" + newFileName);
        } else {
            // Если файл не загружен, но это редактирование – оставляем старый путь
            if (coupling.getId() != null) {
                CouplingEntity existing = couplingService.findById(coupling.getId());
                coupling.setImagePath(existing.getImagePath());
            }
        }

        couplingService.save(coupling);
        redirectAttributes.addFlashAttribute("message", "Муфта успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/couplings";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("GET /couplings/edit/{}", id);
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        return "couplings/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /couplings/delete/{}", id);
        couplingService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Муфта удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/couplings";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }


    private void deleteImageFile(String imagePath) {
        if (imagePath != null && imagePath.startsWith("/uploads/")) {
            try {
                String filePath = System.getProperty("user.dir") + imagePath;
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                log.warn("Не удалось удалить старый файл: {}", imagePath, e);
            }
        }
    }
}