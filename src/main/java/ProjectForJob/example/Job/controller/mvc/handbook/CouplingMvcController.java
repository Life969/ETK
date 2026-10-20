package ProjectForJob.example.Job.MVCcontrollers.Handbook;

import ProjectForJob.example.Job.entityJob.Handbook.CouplingEntity;
import ProjectForJob.example.Job.services.Handbook.CouplingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    private static final String FRAGMENT_LIST   = "fragments/couplings/list :: content";
    private static final String FRAGMENT_FORM   = "fragments/couplings/form :: content";
    private static final String FRAGMENT_DETAIL = "fragments/couplings/detail :: content";

    @GetMapping
    public String listAllGrouped(Model model) {
        log.info("GET /couplings - группировка по типам");
        List<CouplingEntity> allCouplings = couplingService.findAll();

        Map<String, List<CouplingEntity>> groupedByType = allCouplings.stream()
                .collect(Collectors.groupingBy(
                        CouplingEntity::getType,
                        TreeMap::new,
                        Collectors.toList()
                ));

        groupedByType.forEach((type, list) ->
                list.sort(Comparator.comparingInt(c -> Integer.parseInt(c.getConditionalDiameter())))
        );

        model.addAttribute("groupedCouplings", groupedByType);
        model.addAttribute("totalCouplings", allCouplings.size());   // ← ДОБАВЬ
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return "welcomeToHome/layout";
    }

    @GetMapping("/{id}")
    public String showCoupling(@PathVariable Long id, Model model) {
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        model.addAttribute("contentTemplate", FRAGMENT_DETAIL);
        return "welcomeToHome/layout";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("coupling", new CouplingEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("coupling") CouplingEntity coupling,
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

            if (coupling.getId() != null) {
                CouplingEntity existing = couplingService.findById(coupling.getId());
                deleteFile(existing.getImagePath());
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            saveFile("uploads/couplings/", newFileName, imageBytes);
            coupling.setImagePath("/uploads/couplings/" + newFileName);
        } else if (coupling.getId() != null) {
            CouplingEntity existing = couplingService.findById(coupling.getId());
            coupling.setImagePath(existing.getImagePath());
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

            if (coupling.getId() != null) {
                CouplingEntity existing = couplingService.findById(coupling.getId());
                deleteFile(existing.getDrawingPath());
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            saveFile("uploads/drawings/", newFileName, drawingBytes);
            coupling.setDrawingPath("/uploads/drawings/" + newFileName);
            coupling.setDrawingOriginalName(
                    drawingOriginalName != null && !drawingOriginalName.isBlank()
                            ? drawingOriginalName
                            : newFileName);
        } else if (coupling.getId() != null) {
            CouplingEntity existing = couplingService.findById(coupling.getId());
            coupling.setDrawingPath(existing.getDrawingPath());
            coupling.setDrawingOriginalName(existing.getDrawingOriginalName());
        }

        couplingService.save(coupling);
        redirectAttributes.addFlashAttribute("message", "Муфта успешно сохранена");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/couplings";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CouplingEntity existing = couplingService.findById(id);
        deleteFile(existing.getImagePath());
        deleteFile(existing.getDrawingPath());
        couplingService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Муфта удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/couplings";
    }

    // ============================================================
    // Удаление чертежа
    // ============================================================
    @PostMapping("/{id}/drawing/delete")
    public String deleteDrawing(@PathVariable Long id, RedirectAttributes ra) {
        CouplingEntity coupling = couplingService.findById(id);
        deleteFile(coupling.getDrawingPath());
        coupling.setDrawingPath(null);
        coupling.setDrawingOriginalName(null);
        couplingService.save(coupling);

        ra.addFlashAttribute("message", "Чертёж удалён");
        ra.addFlashAttribute("messageType", "danger");
        return "redirect:/couplings/" + id;
    }

    // ============================================================
    // Скачивание чертежа
    // ============================================================
    @GetMapping("/{id}/drawing/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDrawing(@PathVariable Long id) throws IOException {
        CouplingEntity coupling = couplingService.findById(id);
        String path = coupling.getDrawingPath();
        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        java.io.File file = new java.io.File(System.getProperty("user.dir") + path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(file);

        String originalName = coupling.getDrawingOriginalName() != null
                ? coupling.getDrawingOriginalName()
                : file.getName();

        String encoded = java.net.URLEncoder.encode(originalName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentType;
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".pdf"))      contentType = "application/pdf";
        else if (lower.endsWith(".png")) contentType = "image/png";
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
        else contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .body(resource);
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