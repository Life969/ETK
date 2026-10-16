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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                       @RequestParam(value = "drawingBase64", required = false) String drawingBase64,
                       @RequestParam(value = "drawingOriginalName", required = false) String drawingOriginalName,
                       Model model,
                       RedirectAttributes redirectAttributes) throws IOException {

        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return "welcomeToHome/layout";
        }

        // ─── Фото ───
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
                deleteFile(existing.getImagePath());
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            saveFile("uploads/adapters/", newFileName, imageBytes);
            pipeAdapter.setImagePath("/uploads/adapters/" + newFileName);
        } else if (pipeAdapter.getId() != null) {
            PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
            pipeAdapter.setImagePath(existing.getImagePath());
        }

        // ─── Чертёж ───
        if (drawingBase64 != null && !drawingBase64.isEmpty() && drawingBase64.startsWith("data:")) {
            String base64Data = drawingBase64.substring(drawingBase64.indexOf(",") + 1);
            byte[] drawingBytes = Base64.getDecoder().decode(base64Data);

            String contentType = drawingBase64.substring(5, drawingBase64.indexOf(";"));
            String extension = switch (contentType) {
                case "application/pdf" -> ".pdf";
                case "image/jpeg"      -> ".jpg";
                case "image/png"       -> ".png";
                default -> ".bin";
            };

            if (pipeAdapter.getId() != null) {
                PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
                deleteFile(existing.getDrawingPath());
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            saveFile("uploads/drawings/", newFileName, drawingBytes);
            pipeAdapter.setDrawingPath("/uploads/drawings/" + newFileName);
            pipeAdapter.setDrawingOriginalName(
                    drawingOriginalName != null && !drawingOriginalName.isBlank()
                            ? drawingOriginalName
                            : newFileName);
        } else if (pipeAdapter.getId() != null) {
            PipeAdapterEntity existing = pipeAdapterService.findById(pipeAdapter.getId());
            pipeAdapter.setDrawingPath(existing.getDrawingPath());
            pipeAdapter.setDrawingOriginalName(existing.getDrawingOriginalName());
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
        deleteFile(existing.getImagePath());
        deleteFile(existing.getDrawingPath());
        pipeAdapterService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Переводник удалён");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/pipe-adapters";
    }


    // ===== Удаление чертежа =====
    @PostMapping("/{id}/drawing/delete")
    public String deleteDrawing(@PathVariable Long id, RedirectAttributes ra) {
        PipeAdapterEntity adapter = pipeAdapterService.findById(id);
        deleteFile(adapter.getDrawingPath());
        adapter.setDrawingPath(null);
        adapter.setDrawingOriginalName(null);
        pipeAdapterService.save(adapter);

        ra.addFlashAttribute("message", "Чертёж удалён");
        ra.addFlashAttribute("messageType", "danger");
        return "redirect:/pipe-adapters/" + id;
    }

    // ===== Скачивание чертежа =====
    @GetMapping("/{id}/drawing/download")
    public ResponseEntity<Resource> downloadDrawing(@PathVariable Long id) throws IOException {
        PipeAdapterEntity adapter = pipeAdapterService.findById(id);
        String path = adapter.getDrawingPath();
        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(System.getProperty("user.dir") + path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        String originalName = adapter.getDrawingOriginalName() != null
                ? adapter.getDrawingOriginalName()
                : file.getName();

        String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentType;
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".pdf"))      contentType = "application/pdf";
        else if (lower.endsWith(".png")) contentType = "image/png";
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
        else contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    // ===== Просмотр =====
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PipeAdapterEntity pipeAdapter = pipeAdapterService.findById(id);
        model.addAttribute("pipeAdapter", pipeAdapter);
        model.addAttribute("contentTemplate", FRAGMENT_DETAIL);
        return "welcomeToHome/layout";
    }

    private void saveFile(String relativeDir, String fileName, byte[] bytes) throws IOException {
        String uploadDir = System.getProperty("user.dir") + "/" + relativeDir;
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File destFile = new File(uploadDir + fileName);
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(bytes);
        }
    }

    private void deleteFile(String path) {
        if (path != null && path.startsWith("/uploads/")) {
            try {
                File file = new File(System.getProperty("user.dir") + path);
                if (file.exists()) file.delete();
            } catch (Exception e) {
                log.warn("Не удалось удалить файл: {}", path, e);
            }
        }
    }
}