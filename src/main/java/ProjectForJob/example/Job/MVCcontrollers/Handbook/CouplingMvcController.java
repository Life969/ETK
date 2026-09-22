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
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };

            if (coupling.getId() != null) {
                CouplingEntity existing = couplingService.findById(coupling.getId());
                deleteImageFile(existing.getImagePath());
            }

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
        CouplingEntity coupling = couplingService.findById(id);
        model.addAttribute("coupling", coupling);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        couplingService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Муфта удалена");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/couplings";
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