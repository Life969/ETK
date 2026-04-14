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

    // Список с группировкой по firstSideType (аккордеон)
    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String message,
                       @RequestParam(required = false) String messageType) {
        List<String> firstSideTypes = pipeAdapterService.findAllDistinctFirstSideTypes();
        Map<String, List<PipeAdapterEntity>> groupedAdapters = new LinkedHashMap<>();
        for (String type : firstSideTypes) {
            groupedAdapters.put(type, pipeAdapterService.findByFirstSideType(type));
        }
        model.addAttribute("groupedAdapters", groupedAdapters);
        if (message != null) {
            model.addAttribute("message", message);
            model.addAttribute("messageType", messageType != null ? messageType : "info");
        }
        return "pipe-adapters/list";
    }

    // Форма создания нового переводника
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pipeAdapter", new PipeAdapterEntity());
        return "pipe-adapters/form";
    }

    // Сохранение (создание или обновление)
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("pipeAdapter") PipeAdapterEntity pipeAdapter,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageBase64", required = false) String imageBase64,
                       RedirectAttributes redirectAttributes) throws IOException {

        if (bindingResult.hasErrors()) {
            return "pipe-adapters/form";
        }

        // Обработка изображения из base64
        if (imageBase64 != null && !imageBase64.isEmpty() && imageBase64.startsWith("data:image")) {
            String base64Data = imageBase64.substring(imageBase64.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            String contentType = imageBase64.substring(5, imageBase64.indexOf(";"));
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };

            // Удаляем старый файл, если редактируем
            if (pipeAdapter.getId() != null) {
                PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
                String oldImagePath = existing.getImagePath();
                if (oldImagePath != null && !oldImagePath.isEmpty()) {
                    deleteImageFile(oldImagePath);
                }
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
            // Если файл не загружен, но это редактирование – оставляем старый путь
            if (pipeAdapter.getId() != null) {
                PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
                pipeAdapter.setImagePath(existing.getImagePath());
            }
        }

        pipeAdapterService.save(pipeAdapter);
        redirectAttributes.addAttribute("message", "Переводник успешно сохранён");
        redirectAttributes.addAttribute("messageType", "success");
        return "redirect:/pipe-adapters";
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

    // Форма редактирования
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        PipeAdapterEntity pipeAdapter = pipeAdapterService.findById(id);
        model.addAttribute("pipeAdapter", pipeAdapter);
        return "pipe-adapters/form";
    }

    // Удаление
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        pipeAdapterService.deleteById(id);
        redirectAttributes.addAttribute("message", "Переводник удалён");
        redirectAttributes.addAttribute("messageType", "warning");
        return "redirect:/pipe-adapters";
    }

    // Карточка переводника (просмотр)
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PipeAdapterEntity pipeAdapter = pipeAdapterService.findById(id);
        model.addAttribute("pipeAdapter", pipeAdapter);
        return "pipe-adapters/view";
    }
}