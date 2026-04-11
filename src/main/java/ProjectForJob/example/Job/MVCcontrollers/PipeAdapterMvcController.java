package ProjectForJob.example.Job.MVCcontrollers;

import ProjectForJob.example.Job.entityJob.PipeAdapterEntity;
import ProjectForJob.example.Job.services.PipeAdapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pipe-adapters")
@RequiredArgsConstructor
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
    public String save(@ModelAttribute("pipeAdapter") PipeAdapterEntity pipeAdapter,
                       RedirectAttributes redirectAttributes) {
        pipeAdapterService.save(pipeAdapter);
        redirectAttributes.addAttribute("message", "Переводник успешно сохранён");
        redirectAttributes.addAttribute("messageType", "success");
        return "redirect:/pipe-adapters";
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