package ProjectForJob.example.Job.controller.mvc.handbook;

import ProjectForJob.example.Job.domain.entity.handbook.MachineEntity;
import ProjectForJob.example.Job.service.handbook.MachinesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/machines")
@RequiredArgsConstructor
@Slf4j
public class MachinesMvcController {

    private final MachinesService machinesService;

    private static final String FRAGMENT_LIST = "fragments/machines/list :: content";
    private static final String FRAGMENT_FORM = "fragments/machines/form :: content";

    @GetMapping
    public String listAllMachines(Model model) {
        log.info("GET /machines");
        List<MachineEntity> machines = machinesService.findAll();
        model.addAttribute("machines", machines);
        model.addAttribute("contentTemplate", FRAGMENT_LIST);
        return "welcomeToHome/layout";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("machine", new MachineEntity());
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/save")
    public String saveMachine(@Valid @ModelAttribute("machine") MachineEntity machine,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("contentTemplate", FRAGMENT_FORM);
            return "welcomeToHome/layout";
        }
        machinesService.save(machine);
        redirectAttributes.addFlashAttribute("message", "Станок успешно сохранён");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/machines";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        MachineEntity machine = machinesService.findById(id);
        model.addAttribute("machine", machine);
        model.addAttribute("contentTemplate", FRAGMENT_FORM);
        return "welcomeToHome/layout";
    }

    @PostMapping("/delete/{id}")
    public String deleteMachine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        machinesService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Станок удалён");
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/machines";
    }
}