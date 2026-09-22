package ProjectForJob.example.Job.MVCcontrollers.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.PipeAdapterEntity;
import ProjectForJob.example.Job.services.Handbook.PipeAdapterService;
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

@Controller
@RequestMapping("/pipe-adapters")
@RequiredArgsConstructor
@Slf4j
public class PipeAdapterMvcController {

    private final PipeAdapterService pipeAdapterService;

    private static final String FRAGMENT_LIST   = "fragments/adapters/list :: content";
    private static final String FRAGMENT_FORM   = "fragments/adapters/form :: content";
    private static final String FRAGMENT_DETAIL = "fragments/adapters/detail :: content";

    // ===== Список с группировкой по firstSideType =====
    @GetMapping
    public String list(Model model) {
        log.info("GET /pipe-adapters - группировка по firstSideType");

        List<PipeAdapterEntity> all = pipeAdapterService.findAll();  // если такого метода нет — см. ниже
        Map<String, List<PipeAdapterEntity>> groupedAdapters = new LinkedHashMap<>();

        // Порядок ключей — по алфавиту
        List<String> firstSideTypes = pipeAdapterService.findAllDistinctFirstSideTypes();
        for (String type : firstSideTypes) {
            List<PipeAdapterEntity> group = pipeAdapterService.findByFirstSideType(type);
            // Сортировка внутри группы по диаметру первой стороны (если поле строковое — лексикографически)
            group.sort(Comparator.comparing(
                    PipeAdapterEntity::getFirstSideDiameter,
                    Comparator.nullsLast(String::compareToIgnoreCase)));
            groupedAdapters.put(type, group);
        }

        model.addAttribute("groupedAdapters", groupedAdapters);
        model.addAttribute("totalAdapters", all.size());
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return "welcomeToHome/layout";
    }

    // ===== Форма создания =====
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("pipeAdapter", new PipeAdapterEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    // ===== Сохранение =====
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("pipeAdapter") PipeAdapterEntity pipeAdapter,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageBase64", required = false) String imageBase64,
                       Model model,
                       RedirectAttributes redirectAttributes) throws IOException {

        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return "welcomeToHome/layout";
        }

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

            if (pipeAdapter.getId() != null) {
                PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
                deleteImageFile(existing.getImagePath());
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            String uploadDir = System.getProperty("user.dir") + "/uploads/adapters/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            File destFile = new File(uploadDir + newFileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(imageBytes);
            }
            pipeAdapter.setImagePath("/uploads/adapters/" + newFileName);
        } else {
            if (pipeAdapter.getId() != null) {
                PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
                pipeAdapter.setImagePath(existing.getImagePath());
            }
        }

        pipeAdapterService.save(pipeAdapter);
        redirectAttributes.addFlashAttribute("message", "Переводник успешно сохранён");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/pipe-adapters";
    }

    // ===== Форма редактирования =====
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        PipeAdapterEntity pipeAdapter = pipeAdapterService.findById(id);
        model.addAttribute("pipeAdapter", pipeAdapter);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    // ===== Удаление =====
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PipeAdapterEntity existing = pipeAdapterService.findById(id);
        deleteImageFile(existing.getImagePath());
        pipeAdapterService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Переводник удалён");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/pipe-adapters";
    }

    // ===== Просмотр =====
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PipeAdapterEntity pipeAdapter = pipeAdapterService.findById(id);
        model.addAttribute("pipeAdapter", pipeAdapter);
        model.addAttribute("contentTemplate", FRAGMENT_DETAIL);
        return "welcomeToHome/layout";
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